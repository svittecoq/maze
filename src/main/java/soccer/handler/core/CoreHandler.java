package soccer.handler.core;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import soccer.Setup;
import soccer.base.Api;
import soccer.base.RestOutput;
import soccer.base.Result;
import soccer.handler.session.SessionHandler;
import soccer.handler.team.TeamHandler;
import soccer.handler.user.UserHandler;
import soccer.http.HttpService;
import soccer.http.servlet.DashboardServlet;
import soccer.http.servlet.LoginServlet;
import soccer.model.Market;
import soccer.model.Player;
import soccer.model.PlayerId;
import soccer.model.Team;
import soccer.model.TeamId;
import soccer.model.User;
import soccer.model.UserToken;
import soccer.model.outcome.CreateTeamOutcome;
import soccer.model.outcome.TransferPlayerOutcome;
import soccer.model.outcome.UpdatePlayerOutcome;
import soccer.model.outcome.UpdateTeamOutcome;
import soccer.rest.RestService;
import soccer.store.StoreService;
import soccer.store.player.PlayerRecord;
import soccer.store.team.TeamRecord;
import soccer.store.user.UserRecord;

public class CoreHandler {

    private final ConcurrentHashMap<String, UserHandler>       _userHandlerMap;
    private final ConcurrentHashMap<UserToken, SessionHandler> _sessionHandlerMap;
    private final StoreService                                 _storeService;
    private final RestService                                  _restService;
    private final HttpService                                  _httpService;

    private CoreHandler(URI databaseURI, Optional<String> webPathOptional, Optional<Integer> webPortOptional) {

        _userHandlerMap = new ConcurrentHashMap<String, UserHandler>();
        _sessionHandlerMap = new ConcurrentHashMap<UserToken, SessionHandler>();
        _storeService = new StoreService(databaseURI);
        _restService = new RestService(this);
        _httpService = new HttpService(webPathOptional,
                                       webPortOptional,
                                       restService(),
                                       List.of(new LoginServlet(this), new DashboardServlet(this)),
                                       Setup.WEB_PATH);
    }

    private ConcurrentHashMap<String, UserHandler> userHandlerMap() {

        return _userHandlerMap;
    }

    private ConcurrentHashMap<UserToken, SessionHandler> sessionHandlerMap() {

        return _sessionHandlerMap;
    }

    private StoreService storeService() {

        return _storeService;
    }

    public RestService restService() {

        return _restService;
    }

    private HttpService httpService() {

        return _httpService;
    }

    private RestOutput<UserHandler> findUserHandler(UserToken userToken) {

        SessionHandler sessionHandler;

        if (Api.isNull(userToken)) {
            return RestOutput.badRequest();
        }

        sessionHandler = sessionHandlerMap().get(userToken);
        if (sessionHandler == null) {
            Api.error("This session does not exist anymore. FORBIDDEN", userToken, this);
            return RestOutput.forbidden();
        }

        if (sessionHandler.hasTimedOut()) {
            Api.error("This session has timed out. FORBIDDEN", userToken, sessionHandler, this);
            return RestOutput.forbidden();
        }

        // Refresh the session
        sessionHandler.refresh();

        return RestOutput.ok(sessionHandler.userHandler());
    }

    private void monitorSessionHandlers() {

        // Remove any session which has timed out
        sessionHandlerMap().values().removeIf(SessionHandler::hasTimedOut);

        // Reschedule the monitoring later on
        CompletableFuture.runAsync(this::monitorSessionHandlers,
                                   CompletableFuture.delayedExecutor(Setup.SESSION_TIME_OUT.toMillis(),
                                                                     TimeUnit.MILLISECONDS));
    }

    private RestOutput<UserHandler> addUser(User user, Boolean addToStore) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;
        RestOutput<Result> resultOutput;

        if (Api.isNull(user, user.getUsername(), user.getPassword(), addToStore)) {
            return RestOutput.badRequest();
        }

        userHandlerOutput = UserHandler.with(user);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("UserHandler to addUser is NOT OK", userHandlerOutput, user, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        if (userHandlerMap().putIfAbsent(userHandler.userId(), userHandler) != null) {
            Api.error("User to addUser already exists. BAD REQUEST", user, this);
            return RestOutput.badRequest();
        }

        if (addToStore) {
            // Store this new user in the store
            resultOutput = userHandler.persist(storeService());
            if (RestOutput.isNOK(resultOutput)) {
                Api.error("persist to addUser is NOT OK", resultOutput, userHandler, user, this);
                return RestOutput.of(resultOutput);
            }
        }

        return RestOutput.ok(userHandler);
    }

    public RestOutput<UserToken> signUpUser(User user) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;
        UserToken userToken;
        RestOutput<SessionHandler> sessionHandlerOutput;
        SessionHandler sessionHandler;
        RestOutput<Team> teamOutput;
        Team team;
        RestOutput<CreateTeamOutcome> createTeamOutcomeOutput;
        CreateTeamOutcome createTeamOutcome;

        if (Api.isNull(user, user.getUsername(), user.getPassword())) {
            return RestOutput.badRequest();
        }

        userHandlerOutput = addUser(user, Boolean.TRUE);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("addUser to signUpUser is NOT OK", userHandlerOutput, user, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        // Generate unique User Token
        userToken = new UserToken(UUID.randomUUID().toString());

        sessionHandlerOutput = SessionHandler.with(userToken, userHandler);
        if (RestOutput.isNOK(sessionHandlerOutput)) {
            Api.error("SessionHandler to signUpUser is NOT OK",
                      sessionHandlerOutput,
                      userToken,
                      userHandler,
                      user,
                      this);
            return RestOutput.of(sessionHandlerOutput);
        }
        sessionHandler = sessionHandlerOutput.output();

        if (sessionHandlerMap().putIfAbsent(userToken, sessionHandler) != null) {
            Api.error("SessionHandler to signUpUser is a duplicate. INTERNAL FAILURE",
                      userToken,
                      userHandler,
                      user,
                      this);
            return RestOutput.internalFailure();
        }

        // Generate a random team
        teamOutput = TeamHandler.random();
        if (RestOutput.isNOK(teamOutput)) {
            Api.error("Random Team to signUpUser is NOT OK", teamOutput, userHandler, user, this);
            return RestOutput.of(teamOutput);
        }
        team = teamOutput.output();

        // Add this team to the new user
        createTeamOutcomeOutput = createTeam(userHandler, team, Boolean.TRUE);
        if (RestOutput.isNOK(createTeamOutcomeOutput)) {
            Api.error("createTeam to signUpUser failed", createTeamOutcomeOutput, team, userHandler, this);
            return RestOutput.of(createTeamOutcomeOutput);
        }
        createTeamOutcome = createTeamOutcomeOutput.output();

        // The team should be created without error
        if (createTeamOutcome.getError() != null) {
            Api.error("createTream to signUpUser failed. INTERNAL FAILURE", createTeamOutcome, team, userHandler, this);
            return RestOutput.internalFailure();
        }

        return RestOutput.ok(sessionHandler.userToken());
    }

    public RestOutput<UserToken> loginUser(User user) {

        UserHandler userHandler;
        UserToken userToken;
        RestOutput<SessionHandler> sessionHandlerOutput;
        SessionHandler sessionHandler;

        if (Api.isNull(user, user.getUsername(), user.getPassword())) {
            return RestOutput.badRequest();
        }

        userHandler = userHandlerMap().get(user.getUsername());
        if (userHandler == null) {
            Api.error("User to loginUser does not exist. FORBIDDEN", user, this);
            return RestOutput.forbidden();
        }

        // Make sure the password is right
        if (userHandler.matches(user.getPassword()) == false) {
            Api.error("User to loginUser does not match password. FORBIDDEN", user, this);
            return RestOutput.forbidden();
        }

        // Generate unique User Token
        userToken = new UserToken(UUID.randomUUID().toString());

        sessionHandlerOutput = SessionHandler.with(userToken, userHandler);
        if (RestOutput.isNOK(sessionHandlerOutput)) {
            Api.error("SessionHandler to loginUser is NOT OK",
                      sessionHandlerOutput,
                      userToken,
                      userHandler,
                      user,
                      this);
            return RestOutput.of(sessionHandlerOutput);
        }
        sessionHandler = sessionHandlerOutput.output();

        if (sessionHandlerMap().putIfAbsent(userToken, sessionHandler) != null) {
            Api.error("SessionHandler to loginUser is a duplicate. INTERNAL FAILURE",
                      userToken,
                      userHandler,
                      user,
                      this);
            return RestOutput.internalFailure();
        }

        return RestOutput.ok(sessionHandler.userToken());
    }

    public RestOutput<Result> validateUserToken(UserToken userToken) {

        RestOutput<UserHandler> userHandlerOutput;

        if (Api.isNull(userToken)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the UserToken
        userHandlerOutput = findUserHandler(userToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("findUserHandler to validateUserToken is NOT OK", userHandlerOutput, userToken, this);
            return RestOutput.of(userHandlerOutput);
        }

        return RestOutput.OK;
    }

    public RestOutput<CreateTeamOutcome> createTeam(UserHandler userHandler, Team team, Boolean addToStore) {

        AtomicReference<String> errorReference;
        String error;
        RestOutput<TeamHandler> teamHandlerOutput;
        TeamHandler teamHandler;
        RestOutput<Result> resultOutput;

        if (Api.isNull(userHandler, team, addToStore)) {
            return RestOutput.ok(new CreateTeamOutcome(null, "Attributes to create the team are missing."));
        }

        errorReference = new AtomicReference<String>(null);

        // Associate a team to this user
        teamHandlerOutput = userHandler.createTeam(team, errorReference);
        if (RestOutput.isBadRequest(teamHandlerOutput)) {
            error = errorReference.get();
            if (error == null) {
                Api.error("createTeam failed without error. INTERNAL FAILURE",
                          teamHandlerOutput,
                          userHandler,
                          team,
                          this);
                return RestOutput.internalFailure();
            }
            return RestOutput.ok(new CreateTeamOutcome(team.getTeamId(), error));
        }
        if (RestOutput.isNOK(teamHandlerOutput)) {
            Api.error("createTeam is NOT OK", teamHandlerOutput, userHandler, team, errorReference, this);
            return RestOutput.of(teamHandlerOutput);
        }
        teamHandler = teamHandlerOutput.output();

        if (addToStore) {
            // Persist this new team and its players in the store
            resultOutput = teamHandler.persist(Boolean.TRUE, userHandler.userId(), storeService());
            if (RestOutput.isNOK(resultOutput)) {
                Api.error("persist to createTeam is NOT OK", resultOutput, teamHandler, userHandler, this);
                return RestOutput.of(resultOutput);
            }
        }

        // Return a successful team creation
        return RestOutput.ok(new CreateTeamOutcome(teamHandler.teamId()));
    }

    public RestOutput<Team> retrieveTeam(UserToken userToken, Optional<TeamId> teamIdOptional) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;

        if (Api.isNull(userToken, teamIdOptional)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the UserToken
        userHandlerOutput = findUserHandler(userToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("UserHandler to retrieveTeam is NOT OK", userHandlerOutput, userToken, teamIdOptional, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        return userHandler.retrieveTeam(teamIdOptional);
    }

    public RestOutput<UpdateTeamOutcome> updateTeam(UserToken userToken, TeamId teamId, Team team) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;

        if (Api.isNull(userToken, teamId, team)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the UserToken
        userHandlerOutput = findUserHandler(userToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("UserHandler to updateTeam is NOT OK", userHandlerOutput, userToken, team, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        // Check the teamId to verify the appropriate resource
        if (Objects.equals(teamId, team.getTeamId()) == false) {
            Api.error("Mismatch for teamId to updateTeam. BAD REQUEST", userToken, teamId, team, this);
            return RestOutput.badRequest();
        }

        // Update this team
        return userHandler.updateTeam(team.getTeamId(), team.getTeamName(), team.getTeamCountry(), storeService());
    }

    public RestOutput<UpdatePlayerOutcome> updatePlayer(UserToken userToken, PlayerId playerId, Player player) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;

        if (Api.isNull(userToken, playerId, player)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the UserToken
        userHandlerOutput = findUserHandler(userToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("UserHandler to updatePlayer is NOT OK", userHandlerOutput, userToken, player, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        // Check the playerId to verify the appropriate resource
        if (Objects.equals(playerId, player.getPlayerId()) == false) {
            Api.error("Mismatch for playerId to updatePlayer. BAD REQUEST", userToken, playerId, player, this);
            return RestOutput.badRequest();
        }

        // Update this player
        return userHandler.updatePlayer(player.getPlayerId(),
                                        player.getPlayerFirstName(),
                                        player.getPlayerLastName(),
                                        player.getPlayerCountry(),
                                        player.getPlayerTransferValue(),
                                        storeService());
    }

    public RestOutput<Market> retrieveMarket(UserToken userToken) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;
        Team[] teamArray;
        Market market;

        if (Api.isNull(userToken)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the UserToken
        userHandlerOutput = findUserHandler(userToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("UserHandler to retrieveMarket is NOT OK", userHandlerOutput, userToken, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        // Iterate over the teams of all users excluding the active user
        // To allow for concurrent transfers and to not lock all teams for this snapshot,
        // we accept the very remote possibility for listing a player already transfered
        // But the transfer function itself fully protects this race condition

        teamArray = userHandlerMap().values()
                                    .stream()
                                    .filter(uh -> uh != userHandler)
                                    .map(UserHandler::retrieveMarket)
                                    .map(RestOutput::stream)
                                    .filter(Objects::nonNull)
                                    .flatMap(Optional::stream)
                                    .toArray(Team[]::new);

        market = new Market(teamArray);

        return RestOutput.ok(market);
    }

    public RestOutput<TransferPlayerOutcome> transferPlayer(UserToken userToken, Player player) {

        TeamId teamId;
        PlayerId playerId;
        RestOutput<UserHandler> userHandlerOutput;
        UserHandler fromUserHandler;
        UserHandler toUserHandler;
        TeamHandler fromTeamHandler;
        TeamHandler toTeamHandler;

        if (Api.isNull(userToken, player, player.getTeamId(), player.getPlayerId())) {
            return RestOutput.badRequest();
        }

        teamId = player.getTeamId();
        playerId = player.getPlayerId();

        // Find the UserHandler from the UserToken
        userHandlerOutput = findUserHandler(userToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("UserHandler to transferPlayer is NOT OK", userHandlerOutput, userToken, player, this);
            return RestOutput.of(userHandlerOutput);
        }
        toUserHandler = userHandlerOutput.output();

        toTeamHandler = toUserHandler.accessTeamHandler();
        if (toTeamHandler == null) {
            Api.error("Team for this user is not defined", userToken, toUserHandler, teamId, playerId, this);
            return RestOutput.ok(new TransferPlayerOutcome(teamId, playerId, "Team for this user is not defined"));
        }

        // TODO Linear lookup OK as long as total number of teams remain small
        fromUserHandler = userHandlerMap().values().stream().filter(uh -> uh.hasTeam(teamId)).findAny().orElse(null);

        if (fromUserHandler == null) {
            Api.error("User of team to transfer from is not defined", userToken, toUserHandler, teamId, playerId, this);
            return RestOutput.ok(new TransferPlayerOutcome(teamId,
                                                           playerId,
                                                           "User of team to transfer from is not defined"));
        }

        fromTeamHandler = fromUserHandler.accessTeamHandler();
        if (fromTeamHandler == null) {
            Api.error("Team to transfer from is not defined", userToken, fromUserHandler, teamId, playerId, this);
            return RestOutput.ok(new TransferPlayerOutcome(teamId, playerId, "Team to transfer from is not defined"));
        }

        // Transfer this player from the old team to the team of this user
        return toTeamHandler.transferPlayer(fromUserHandler.userId(),
                                            fromTeamHandler,
                                            toUserHandler.userId(),
                                            playerId,
                                            storeService());
    }

    public RestOutput<Result> run() {

        RestOutput<Result> resultOutput;
        RestOutput<UserHandler> addUserOutput;
        RestOutput<List<UserRecord>> userRecordListOutput;
        List<UserRecord> userRecordList;
        User user;
        UserHandler userHandler;
        RestOutput<List<TeamRecord>> teamRecordListOutput;
        List<TeamRecord> teamRecordList;
        RestOutput<List<PlayerRecord>> playerRecordListOutput;
        List<PlayerRecord> playerRecordList;
        List<Player> playerList;
        Map<TeamId, List<Player>> playerListMap;
        Team team;
        RestOutput<CreateTeamOutcome> createTeamOutcomeOutput;
        CreateTeamOutcome createTeamOutcome;

        // Start the StoreService
        resultOutput = storeService().start();
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("Start StoreService is NOT OK", resultOutput, this);
            return RestOutput.of(resultOutput);
        }

        // Load all users
        userRecordListOutput = storeService().loadUserRecords();
        if (RestOutput.isNOK(userRecordListOutput)) {
            Api.error("loadUserRecords is NOT OK", userRecordListOutput, this);
            return RestOutput.of(userRecordListOutput);
        }
        userRecordList = userRecordListOutput.output();

        for (UserRecord userRecord : userRecordList) {

            user = new User(userRecord.userId(), userRecord.userPassword());

            // Add each user into the map
            addUserOutput = addUser(user, Boolean.FALSE);
            if (RestOutput.isNOK(addUserOutput)) {
                Api.error("addUser to run failed. User skipped", addUserOutput, user, userRecord, this);
            }
        }

        // Load all teams
        teamRecordListOutput = storeService().loadTeamRecords();
        if (RestOutput.isNOK(teamRecordListOutput)) {
            Api.error("loadTeamRecords is NOT OK", teamRecordListOutput, this);
            return RestOutput.of(teamRecordListOutput);
        }
        teamRecordList = teamRecordListOutput.output();

        // Load all players
        playerRecordListOutput = storeService().loadPlayerRecords();
        if (RestOutput.isNOK(playerRecordListOutput)) {
            Api.error("loadPlayerRecords is NOT OK", playerRecordListOutput, this);
            return RestOutput.of(playerRecordListOutput);
        }
        playerRecordList = playerRecordListOutput.output();

        // Group all players into their relevant teams
        playerListMap = playerRecordList.stream()
                                        .map(pr -> new Player(pr.playerId(),
                                                              pr.playerType(),
                                                              pr.firstName(),
                                                              pr.lastName(),
                                                              pr.country(),
                                                              pr.age(),
                                                              pr.assetValue(),
                                                              pr.transferValue(),
                                                              pr.teamId()))
                                        .collect(Collectors.groupingBy(Player::getTeamId));

        for (TeamRecord teamRecord : teamRecordList) {

            userHandler = userHandlerMap().get(teamRecord.userId());
            if (userHandler == null) {
                Api.error("User does not exist to restore Team. Team skippped", teamRecord, this);
                continue;
            }

            // Remove this list of players
            playerList = playerListMap.remove(teamRecord.teamId());
            if (playerList == null) {
                Api.error("no list of players to run. Team skipped", teamRecord, userHandler, this);
                continue;
            }

            // Create the full team for this user
            team = new Team(teamRecord.teamId(),
                            teamRecord.name(),
                            teamRecord.country(),
                            teamRecord.balance(),
                            playerList.toArray(Player[]::new));

            // Add each team for its respective user
            createTeamOutcomeOutput = createTeam(userHandler, team, Boolean.FALSE);
            if (RestOutput.isNOK(createTeamOutcomeOutput)) {
                Api.error("createTeam to run failed. Team skipped",
                          createTeamOutcomeOutput,
                          team,
                          teamRecord,
                          userHandler,
                          this);
                continue;
            }
            createTeamOutcome = createTeamOutcomeOutput.output();

            // The team should be recreated without error
            if (createTeamOutcome.getError() != null) {
                Api.error("createTream to run failed. Team skipped",
                          createTeamOutcome,
                          team,
                          teamRecord,
                          userHandler,
                          this);
                continue;
            }
        }

        // Check there is no orphan player without a team
        if (playerListMap.isEmpty() == false) {
            Api.error("There are orphan players. INTERNAL FAILURE", playerListMap.size(), this);
            return RestOutput.internalFailure();
        }

        // Check each users has its team
        if (userHandlerMap().values().stream().filter(UserHandler::hasNoTeam).findAny().isPresent()) {
            Api.error("User without a team. INTERNAL FAILURE", this);
            return RestOutput.internalFailure();
        }

        // Start the HttpService
        resultOutput = httpService().start();
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("Open HttpService is NOT OK", resultOutput, this);
            return RestOutput.of(resultOutput);
        }

        // Monitor periodically the sessionHandlers
        monitorSessionHandlers();

        return RestOutput.OK;
    }

    public RestOutput<Result> terminate() {

        RestOutput<Result> resultOutput;

        // Stop the StoreService and drop the database
        resultOutput = storeService().stop(true);
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("Stop StoreService is NOT OK", resultOutput, this);
            return RestOutput.of(resultOutput);
        }

        // Stop the HttpService
        resultOutput = httpService().stop();
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("Stop HttpService is NOT OK", resultOutput, this);
            return RestOutput.of(resultOutput);
        }

        return RestOutput.OK;
    }

    @Override
    public String toString() {
        return "CoreHandler [_userHandlerMap=" + _userHandlerMap + ", _sessionHandlerMap=" + _sessionHandlerMap + "]";
    }

    public static RestOutput<CoreHandler> with(URI databaseURI,
                                               Optional<String> webPathOptional,
                                               Optional<Integer> webPortOptional) {

        CoreHandler coreHandler;

        if (Api.isNull(databaseURI, webPathOptional, webPortOptional)) {
            return RestOutput.badRequest();
        }

        coreHandler = new CoreHandler(databaseURI, webPathOptional, webPortOptional);

        return RestOutput.ok(coreHandler);
    }
}

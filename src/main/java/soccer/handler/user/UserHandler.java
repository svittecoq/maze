package soccer.handler.user;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import soccer.Setup;
import soccer.base.Api;
import soccer.base.RestOutput;
import soccer.base.Result;
import soccer.handler.team.TeamHandler;
import soccer.model.PlayerId;
import soccer.model.Team;
import soccer.model.TeamId;
import soccer.model.User;
import soccer.model.outcome.UpdatePlayerOutcome;
import soccer.model.outcome.UpdateTeamOutcome;
import soccer.store.StoreService;
import soccer.store.user.UserRecord;

public class UserHandler {

    private final String                       _userId;
    private final String                       _userPassword;

    private final AtomicReference<TeamHandler> _teamHandlerReference;

    private UserHandler(String userId, String userPassword) {

        _userId = userId;
        _userPassword = userPassword;

        _teamHandlerReference = new AtomicReference<TeamHandler>(null);
    }

    public String userId() {

        return _userId;
    }

    private String userPassword() {

        return _userPassword;
    }

    public boolean matches(String userPassword) {

        return Objects.equals(userPassword(), userPassword);
    }

    public RestOutput<Result> persist(StoreService storeService) {

        UserRecord userRecord;

        if (Api.isNull(storeService)) {
            return RestOutput.badRequest();
        }

        // Build the UserRecord
        userRecord = new UserRecord(userId(), userPassword());

        // Store or update the UserRecord
        return storeService.storeUserRecord(userRecord);
    }

    private AtomicReference<TeamHandler> teamHandlerReference() {

        return _teamHandlerReference;
    }

    public RestOutput<Result> assignTeamHandler(TeamHandler teamHandler) {

        if (Api.isNull(teamHandler)) {
            return RestOutput.badRequest();
        }

        if (teamHandlerReference().compareAndSet(null, teamHandler) == false) {
            Api.error("TeamHandler already assigned. INTERNAL FAILURE", teamHandler, this);
            return RestOutput.internalFailure();
        }

        return RestOutput.OK;
    }

    public TeamHandler accessTeamHandler() {

        return teamHandlerReference().get();
    }

    public boolean hasNoTeam() {

        return (accessTeamHandler() == null);
    }

    public boolean hasTeam(TeamId teamId) {

        TeamHandler teamHandler;

        teamHandler = accessTeamHandler();
        if (teamHandler == null) {
            return false;
        }

        return Objects.equals(teamId, teamHandler.teamId());
    }

    public RestOutput<Team> retrieveTeam(Optional<TeamId> teamIdOptional) {

        return accessTeamHandler().retrieveTeam(teamIdOptional);
    }

    public RestOutput<TeamHandler> createTeam(Team team, AtomicReference<String> errorReference) {

        RestOutput<TeamHandler> teamHandlerOutput;
        TeamHandler teamHandler;
        RestOutput<Result> resultOutput;

        if (Api.isNull(team,
                       team.getTeamId(),
                       team.getTeamName(),
                       team.getTeamCountry(),
                       team.getTeamBalance(),
                       team.getPlayerArray())) {
            errorReference.set("Invalid attributes to create team");
            return RestOutput.badRequest();
        }

        // Generate a new team for this new user
        teamHandlerOutput = TeamHandler.with(team);
        if (RestOutput.isNOK(teamHandlerOutput)) {
            Api.error("TeamHandler is NOT OK", teamHandlerOutput, team, this);
            return RestOutput.of(teamHandlerOutput);
        }
        teamHandler = teamHandlerOutput.output();

        // Assign this team to this user
        resultOutput = assignTeamHandler(teamHandler);
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("assignTeamHandler is NOT OK", resultOutput, teamHandler, team, this);
            return RestOutput.of(resultOutput);
        }

        return RestOutput.ok(teamHandler);
    }

    public RestOutput<UpdateTeamOutcome> updateTeam(TeamId teamId,
                                                    String name,
                                                    String country,
                                                    StoreService storeService) {

        return accessTeamHandler().updateTeam(teamId, name, country, userId(), storeService);
    }

    public RestOutput<UpdatePlayerOutcome> updatePlayer(PlayerId playerId,
                                                        String firstName,
                                                        String lastName,
                                                        String country,
                                                        Long transferValue,
                                                        StoreService storeService) {

        return accessTeamHandler().updatePlayer(playerId, firstName, lastName, country, transferValue, storeService);
    }

    public RestOutput<Optional<Team>> retrieveMarket() {

        // Return a team of players available for transfer

        return accessTeamHandler().retrieveMarket();
    }

    @Override
    public String toString() {
        return "UserHandler [_userId=" + _userId
               + ", _userPassword="
               + _userPassword
               + ", _teamHandlerReference="
               + _teamHandlerReference
               + "]";
    }

    public static RestOutput<UserHandler> with(User user) {

        String userName;
        String userPassword;
        UserHandler userHandler;

        if (Api.isNull(user, user.getUsername(), user.getPassword())) {
            return RestOutput.badRequest();
        }

        try {

            // Check the validity of the password
            userName = user.getUsername().trim();
            if (userName.isEmpty()) {
                Api.error("UserName is empty. BAD REQUEST", user);
                return RestOutput.badRequest();
            }
            if (Setup.USER_NAME_PATTERN.matcher(userName).find() == false) {
                Api.error("UserName is not valid. BAD REQUEST", user);
                return RestOutput.badRequest();
            }

            // Check the validity of the password
            userPassword = user.getPassword().trim();
            if (userPassword.isEmpty()) {
                Api.error("UserPassword is empty. BAD REQUEST", user);
                return RestOutput.badRequest();
            }

            if (Setup.USER_PASSWORD_PATTERN.matcher(userPassword).find() == false) {
                Api.error("UserPassword is not strong enough. BAD REQUEST",
                          "At least 1 lower case alphabetical character",
                          "At least 1 upper case",
                          "At least 1 numeric",
                          "At least 1 special character",
                          "At least 8 characters",
                          user);
                return RestOutput.badRequest();
            }

            userHandler = new UserHandler(userName, userPassword);

            return RestOutput.ok(userHandler);

        } catch (Throwable t) {
            Api.error(t, "User failed. BAD REQUEST", user);
            return RestOutput.badRequest();
        }
    }
}

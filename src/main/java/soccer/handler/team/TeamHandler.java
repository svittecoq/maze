package soccer.handler.team;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Stream;

import soccer.Setup;
import soccer.base.Api;
import soccer.base.RestOutput;
import soccer.base.Result;
import soccer.handler.player.PlayerHandler;
import soccer.model.Player;
import soccer.model.PlayerId;
import soccer.model.PlayerType;
import soccer.model.Team;
import soccer.model.TeamId;
import soccer.model.outcome.TransferPlayerOutcome;
import soccer.model.outcome.UpdatePlayerOutcome;
import soccer.model.outcome.UpdateTeamOutcome;
import soccer.store.StoreService;
import soccer.store.team.TeamRecord;

public class TeamHandler implements Comparable<TeamHandler> {

    private final TeamId                           _teamId;

    private String                                 _name;
    private String                                 _country;
    private Long                                   _balance;

    private final HashMap<PlayerId, PlayerHandler> _playerHandlerMap;
    private final ReentrantLock                    _lock;

    private final static AtomicInteger             IndexGenerator = new AtomicInteger(0);

    private TeamHandler(TeamId teamId, String name, String country, Long balance) {

        _teamId = teamId;

        _name = name;
        _country = country;
        _balance = balance;

        _playerHandlerMap = new HashMap<PlayerId, PlayerHandler>();

        // All players within a team are managed within this lock
        _lock = new ReentrantLock();
    }

    private void lock() {

        _lock.lock();
    }

    private void unlock() {

        _lock.unlock();
    }

    public TeamId teamId() {

        return _teamId;
    }

    private String getName() {
        return _name;
    }

    private void setName(String name) {
        _name = name;
    }

    private String getCountry() {
        return _country;
    }

    private void setCountry(String country) {
        _country = country;
    }

    private Long getBalance() {
        return _balance;
    }

    private void incrementBalance(Long value) {

        _balance += value;
    }

    private void decrementBalance(Long value) {

        _balance -= value;
    }

    private HashMap<PlayerId, PlayerHandler> playerHandlerMap() {

        return _playerHandlerMap;
    }

    private RestOutput<Result> linkPlayerUnderLock(PlayerHandler playerHandler) {

        if (Api.isNull(playerHandler)) {
            return RestOutput.badRequest();
        }

        // Link this player into this team
        if (playerHandlerMap().putIfAbsent(playerHandler.playerId(), playerHandler) != null) {
            Api.error("Player already present in team to link. INTERNAL FAILURE", playerHandler, this);
            return RestOutput.internalFailure();
        }

        Api.info("Player " + playerHandler + " linked", this);
        return RestOutput.OK;
    }

    private RestOutput<Result> unlinkPlayerUnderLock(PlayerHandler playerHandler) {

        if (Api.isNull(playerHandler)) {
            return RestOutput.badRequest();
        }

        // Unlink this player from this team
        if (playerHandlerMap().remove(playerHandler.playerId(), playerHandler) == false) {
            Api.error("Player not present in team to unlink. INTERNAL FAILURE", playerHandler, this);
            return RestOutput.internalFailure();
        }

        Api.info("Player " + playerHandler + " unlinked", this);

        return RestOutput.OK;
    }

    public RestOutput<Result> persist(Boolean persistPlayers, String userId, StoreService storeService) {

        TeamRecord teamRecord;
        RestOutput<Result> resultOutput;

        if (Api.isNull(persistPlayers, userId, storeService)) {
            return RestOutput.badRequest();
        }

        try {
            lock();

            // Build the TeamRecord
            teamRecord = new TeamRecord(userId, teamId(), getName(), getCountry(), getBalance());

            // Store or update the TeamRecord
            resultOutput = storeService.storeTeamRecord(teamRecord);
            if (RestOutput.isNOK(resultOutput)) {
                Api.error("storeTeamRecord is NOT OK", resultOutput, teamRecord, userId, this);
                return RestOutput.of(resultOutput);
            }

            if (persistPlayers) {

                for (PlayerHandler playerHandler : playerHandlerMap().values()) {

                    // Persist each player
                    resultOutput = playerHandler.persist(teamId(), storeService);
                    if (RestOutput.isNOK(resultOutput)) {
                        Api.error("persist of player is NOT OK", resultOutput, playerHandler, teamRecord, userId, this);
                        return RestOutput.of(resultOutput);
                    }
                }
            }

            return RestOutput.OK;

        } finally {
            unlock();
        }
    }

    public RestOutput<Team> retrieveTeam(Optional<TeamId> teamIdOptional) {

        Player[] playerArray;
        Team team;

        if (Api.isNull(teamIdOptional)) {
            return RestOutput.badRequest();
        }

        if (teamIdOptional.isPresent()) {
            if (teamIdOptional.get().equals(teamId()) == false) {
                Api.error("TeamId does not match to retrieveTeam. BAD REQUEST", teamIdOptional, this);
                return RestOutput.badRequest();
            }
        }

        try {
            lock();

            // Sorted all players
            playerArray = playerHandlerMap().values()
                                            .stream()
                                            .sorted()
                                            .map(ph -> ph.retrievePlayer(teamId()))
                                            .map(RestOutput::stream)
                                            .filter(Objects::nonNull)
                                            .toArray(Player[]::new);

            team = new Team(teamId(), getName(), getCountry(), getBalance(), playerArray);

            return RestOutput.ok(team);

        } finally {
            unlock();
        }
    }

    public RestOutput<UpdateTeamOutcome> updateTeam(TeamId teamId,
                                                    String name,
                                                    String country,
                                                    String userId,
                                                    StoreService storeService) {
        RestOutput<Result> resultOutput;

        if (Api.isNull(teamId, name, country, userId, storeService)) {
            return RestOutput.badRequest();
        }

        // Make sure the user updates its own team
        if (Objects.equals(teamId, teamId()) == false) {
            Api.error("TeamId does not match to updateTeam", teamId, name, country, userId, this);
            return RestOutput.ok(new UpdateTeamOutcome(teamId, "TeamId does not match"));
        }

        try {
            lock();

            setName(name);
            setCountry(country);

            // Persist this updated team in the store
            resultOutput = persist(Boolean.FALSE, userId, storeService);
            if (RestOutput.isNOK(resultOutput)) {
                Api.error("persist to updateTeam is NOT OK", resultOutput, teamId, name, country, userId, this);
                return RestOutput.ok(new UpdateTeamOutcome(teamId, "Persist of team in store failed"));
            }

            // Successful update of team
            return RestOutput.ok(new UpdateTeamOutcome(teamId()));

        } finally {
            unlock();
        }
    }

    public RestOutput<UpdatePlayerOutcome> updatePlayer(PlayerId playerId,
                                                        String firstName,
                                                        String lastName,
                                                        String country,
                                                        Long transferValue,
                                                        StoreService storeService) {

        PlayerHandler playerHandler;

        if (Api.isNull(playerId, firstName, lastName, country, transferValue, storeService)) {
            return RestOutput.badRequest();
        }

        try {
            lock();

            playerHandler = playerHandlerMap().get(playerId);
            if (playerHandler == null) {
                Api.error("Player does not exist in this team.", playerId, firstName, lastName, country, this);
                return RestOutput.ok(new UpdatePlayerOutcome(playerId, "Player does not exist"));
            }

            return playerHandler.update(firstName, lastName, country, transferValue, teamId(), storeService);

        } finally {
            unlock();
        }
    }

    public RestOutput<Optional<Team>> retrieveMarket() {

        Player[] playerArray;
        Team team;

        try {
            lock();

            // Return a team of players available for transfer
            // (With a strictly positive Transfer Value)
            playerArray = playerHandlerMap().values()
                                            .stream()
                                            .filter(PlayerHandler::isInMarket)
                                            .map(ph -> ph.retrievePlayer(teamId()))
                                            .map(RestOutput::stream)
                                            .filter(Objects::nonNull)
                                            .toArray(Player[]::new);

            if (playerArray.length == 0) {
                // No Player in the market available for transfer
                return RestOutput.ok(Optional.empty());
            }

            team = new Team(teamId(), getName(), getCountry(), getBalance(), playerArray);

            return RestOutput.ok(Optional.of(team));

        } finally {
            unlock();
        }
    }

    public RestOutput<TransferPlayerOutcome> transferPlayer(String fromUserId,
                                                            TeamHandler fromTeamHandler,
                                                            String toUserId,
                                                            PlayerId playerId,
                                                            StoreService storeService) {

        PlayerHandler playerHandler;
        Long marketPrice;
        RestOutput<Result> resultOutput;

        if (Api.isNull(fromUserId, fromTeamHandler, toUserId, playerId, storeService)) {
            return RestOutput.badRequest();
        }

        try {
            lock();

            try {
                fromTeamHandler.lock();

                // Lock both teams to avoid double spend
                // There is no roll-back mechanism defined without more context

                // 1. Locate the player to transfer
                playerHandler = fromTeamHandler.playerHandlerMap().get(playerId);
                if (playerHandler == null) {
                    Api.error("Player to transfer does not exist in this team", fromTeamHandler, playerId, this);
                    return RestOutput.ok(new TransferPlayerOutcome(fromTeamHandler.teamId(),
                                                                   playerId,
                                                                   "Player to transfer does not exist in this team"));
                }

                // 2. Check player is in market
                if (playerHandler.isInMarket() == false) {
                    Api.error("Player is not in the market for transfer", fromTeamHandler, playerId, this);
                    return RestOutput.ok(new TransferPlayerOutcome(fromTeamHandler.teamId(),
                                                                   playerId,
                                                                   "Player is not in the market for transfer"));

                }

                // 3. Check balance of buying team
                marketPrice = playerHandler.marketPrice();
                if (getBalance() < marketPrice) {
                    Api.error("Balance is not enough to transfer this player", fromTeamHandler, playerId, this);
                    return RestOutput.ok(new TransferPlayerOutcome(fromTeamHandler.teamId(),
                                                                   playerId,
                                                                   "Balance is not enough to transfer this player"));
                }

                // 4. Update the balances and update the player
                fromTeamHandler.incrementBalance(marketPrice);
                decrementBalance(marketPrice);

                resultOutput = playerHandler.transfer();
                if (RestOutput.isNOK(resultOutput)) {
                    Api.error("Failure to transfer the player", fromTeamHandler, playerId, playerHandler, this);
                    return RestOutput.ok(new TransferPlayerOutcome(fromTeamHandler.teamId(),
                                                                   playerId,
                                                                   "Failure to transfer the player"));
                }

                // 5. Remove player from old team
                resultOutput = fromTeamHandler.unlinkPlayerUnderLock(playerHandler);
                if (RestOutput.isNOK(resultOutput)) {
                    Api.error("Failure to unlink the player from its old team",
                              fromTeamHandler,
                              playerId,
                              playerHandler,
                              this);
                    return RestOutput.ok(new TransferPlayerOutcome(fromTeamHandler.teamId(),
                                                                   playerId,
                                                                   "Failure to remove the player from its old team"));
                }

                // 6. Add player to new team
                resultOutput = linkPlayerUnderLock(playerHandler);
                if (RestOutput.isNOK(resultOutput)) {
                    Api.error("Failure to link the player to its new team",
                              fromTeamHandler,
                              playerId,
                              playerHandler,
                              this);
                    return RestOutput.ok(new TransferPlayerOutcome(fromTeamHandler.teamId(),
                                                                   playerId,
                                                                   "Failure to remove the player from its old team"));
                }

                // 7. Persist the from team
                resultOutput = fromTeamHandler.persist(Boolean.FALSE, fromUserId, storeService);
                if (RestOutput.isNOK(resultOutput)) {
                    Api.error("persist of fromTeam to transferPlayer is NOT OK",
                              resultOutput,
                              fromUserId,
                              fromTeamHandler,
                              playerId,
                              this);
                    return RestOutput.ok(new TransferPlayerOutcome(fromTeamHandler.teamId(),
                                                                   playerId,
                                                                   "Persist of from team in store failed"));
                }

                // 8. Persist the to team
                resultOutput = persist(Boolean.FALSE, toUserId, storeService);
                if (RestOutput.isNOK(resultOutput)) {
                    Api.error("persist of toTeam to transferPlayer is NOT OK", resultOutput, toUserId, playerId, this);
                    return RestOutput.ok(new TransferPlayerOutcome(fromTeamHandler.teamId(),
                                                                   playerId,
                                                                   "Persist of to team in store failed"));
                }

                // 9. Persist the updated player
                resultOutput = playerHandler.persist(teamId(), storeService);
                if (RestOutput.isNOK(resultOutput)) {
                    Api.error("persist of player to transferPlayer is NOT OK", resultOutput, playerId, this);
                    return RestOutput.ok(new TransferPlayerOutcome(fromTeamHandler.teamId(),
                                                                   playerId,
                                                                   "Persist of player in store failed"));
                }

                Api.info("Player transfered from team " + fromTeamHandler.teamId() + " to team " + teamId());

                // Transfer successful
                return RestOutput.ok(new TransferPlayerOutcome(teamId(), playerId));

            } finally {
                fromTeamHandler.unlock();
            }

        } finally {
            unlock();
        }
    }

    @Override
    public int compareTo(TeamHandler teamHandler) {

        return Objects.compare(teamId(), teamHandler.teamId(), TeamId::compareTo);
    }

    @Override
    public String toString() {
        return "TeamHandler [_teamId=" + _teamId
               + ", _name="
               + _name
               + ", _country="
               + _country
               + ", _balance="
               + _balance
               + "]";
    }

    public static RestOutput<TeamHandler> with(Team team) {

        TeamHandler teamHandler;
        Optional<RestOutput<Result>> resultOutputOptional;

        if (Api.isNull(team.getTeamId(),
                       team.getTeamName(),
                       team.getTeamCountry(),
                       team.getTeamBalance(),
                       team.getPlayerArray())) {
            return RestOutput.badRequest();
        }

        try {

            teamHandler = new TeamHandler(team.getTeamId(),
                                          team.getTeamName(),
                                          team.getTeamCountry(),
                                          team.getTeamBalance());

            // Link all players in the teamHandler
            resultOutputOptional = Arrays.stream(team.getPlayerArray())
                                         .map(PlayerHandler::with)
                                         .map(RestOutput::stream)
                                         .map(teamHandler::linkPlayerUnderLock)
                                         .filter(ro -> ro != RestOutput.OK)
                                         .findAny();

            if (resultOutputOptional.isPresent()) {
                Api.error("Link Players failed. BAD REQUEST", team);
                return RestOutput.badRequest();
            }

            return RestOutput.ok(teamHandler);

        } catch (Throwable t) {
            Api.error(t, "Team failed. INTERNAL FAILURE", team);
            return RestOutput.internalFailure();
        }
    }

    private static Stream<Player> randomPlayers(PlayerType playerType, TeamId teamId, int count) {

        return Stream.generate(() -> PlayerHandler.random(playerType, teamId, IndexGenerator.incrementAndGet()))
                     .limit(count)
                     .map(RestOutput::stream)
                     .filter(Objects::nonNull);
    }

    public static RestOutput<Team> random() {

        Integer index;
        TeamId teamId;
        String name;
        String country;
        Long balance;
        Team team;

        index = IndexGenerator.incrementAndGet();

        teamId = TeamId.random();

        name = "name_" + index;
        country = "country_" + index;

        balance = Setup.DEFAULT_TEAM_BALANCE;

        // Generate a team with all relevant player types
        team = new Team(teamId,
                        name,
                        country,
                        balance,
                        Setup.DEFAULT_TEAM_SIZE.entrySet()
                                               .stream()
                                               .map(entry -> randomPlayers(entry.getKey(), teamId, entry.getValue()))
                                               .flatMap(Function.identity())
                                               .toArray(Player[]::new));

        return RestOutput.ok(team);
    }
}

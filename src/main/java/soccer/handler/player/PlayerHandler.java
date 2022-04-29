package soccer.handler.player;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import soccer.Setup;
import soccer.base.Api;
import soccer.base.RestOutput;
import soccer.base.Result;
import soccer.model.Player;
import soccer.model.PlayerId;
import soccer.model.PlayerType;
import soccer.model.TeamId;
import soccer.model.outcome.UpdatePlayerOutcome;
import soccer.store.StoreService;
import soccer.store.player.PlayerRecord;

public class PlayerHandler implements Comparable<PlayerHandler> {

    private final PlayerId   _playerId;

    private final PlayerType _type;

    private String           _firstName;
    private String           _lastName;
    private String           _country;
    private Integer          _age;

    private Long             _assetValue;
    private Long             _transferValue;

    private PlayerHandler(PlayerId playerId,
                          PlayerType type,
                          String firstName,
                          String lastName,
                          String country,
                          Integer age,
                          Long assetValue,
                          Long transferValue) {

        _playerId = playerId;

        _type = type;
        _firstName = firstName;
        _lastName = lastName;
        _country = country;
        _age = age;

        _assetValue = assetValue;
        _transferValue = transferValue;

    }

    public PlayerId playerId() {

        return _playerId;
    }

    private PlayerType type() {
        return _type;
    }

    private String getFirstName() {
        return _firstName;
    }

    private void setFirstName(String firstName) {
        _firstName = firstName;
    }

    private String getLastName() {
        return _lastName;
    }

    private void setLastName(String lastName) {
        _lastName = lastName;
    }

    private String getCountry() {
        return _country;
    }

    private void setCountry(String country) {
        _country = country;
    }

    private Integer getAge() {
        return _age;
    }

    private void setAge(Integer age) {
        _age = age;
    }

    private Long getAssetValue() {
        return _assetValue;
    }

    private void setAssetValue(Long assetValue) {
        _assetValue = assetValue;
    }

    private Long getTransferValue() {
        return _transferValue;
    }

    private void setTransferValue(Long transferValue) {
        _transferValue = transferValue;
    }

    public RestOutput<Result> persist(TeamId teamId, StoreService storeService) {

        PlayerRecord playerRecord;

        if (Api.isNull(teamId, storeService)) {
            return RestOutput.badRequest();
        }

        // Build the PlayerRecord
        playerRecord = new PlayerRecord(playerId(),
                                        type(),
                                        getFirstName(),
                                        getLastName(),
                                        getCountry(),
                                        getAge(),
                                        getAssetValue(),
                                        getTransferValue(),
                                        teamId);

        // Store or update the TeamRecord
        return storeService.storePlayerRecord(playerRecord);
    }

    public Long marketPrice() {

        return (getTransferValue());
    }

    public boolean isInMarket() {

        return (getTransferValue() > Setup.PLAYER_NO_TRANSFER_VALUE);
    }

    public RestOutput<Player> retrievePlayer(TeamId teamId) {

        if (Api.isNull(teamId)) {
            return RestOutput.badRequest();
        }

        return RestOutput.ok(new Player(playerId(),
                                        type(),
                                        getFirstName(),
                                        getLastName(),
                                        getCountry(),
                                        getAge(),
                                        getAssetValue(),
                                        getTransferValue(),
                                        teamId));
    }

    public RestOutput<UpdatePlayerOutcome> update(String firstName,
                                                  String lastName,
                                                  String country,
                                                  Long transferValue,
                                                  TeamId teamId,
                                                  StoreService storeService) {
        RestOutput<Result> resultOutput;

        if (Api.isNull(firstName, lastName, country, transferValue, teamId, storeService)) {
            return RestOutput.badRequest();
        }

        // Validate the transfer value
        if (transferValue < Setup.PLAYER_NO_TRANSFER_VALUE) {
            Api.error("TransferValue is not valid to updatePlayer",
                      firstName,
                      lastName,
                      country,
                      transferValue,
                      teamId,
                      this);
            return RestOutput.ok(new UpdatePlayerOutcome(playerId(),
                                                         "TransferValue " + transferValue + " is not valid"));
        }

        setFirstName(firstName);
        setLastName(lastName);
        setCountry(country);
        setTransferValue(transferValue);

        // No one is eternal
        setAge(getAge());

        // Persist this updated team in the store
        resultOutput = persist(teamId, storeService);
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("persist to updatePlayer is NOT OK",
                      resultOutput,
                      firstName,
                      lastName,
                      country,
                      transferValue,
                      teamId,
                      this);
            return RestOutput.ok(new UpdatePlayerOutcome(playerId(), "Persist of player in store failed"));
        }

        // Successful update of player
        return RestOutput.ok(new UpdatePlayerOutcome(playerId()));
    }

    public RestOutput<Result> transfer() {

        Long valueIncrease;
        Long assetValue;
        int percentageIncrease;

        assetValue = getTransferValue();

        percentageIncrease = 10 + ThreadLocalRandom.current().nextInt(91);
        valueIncrease = assetValue * percentageIncrease / 100;

        assetValue += valueIncrease;

        // Set the new asset value for this player
        setAssetValue(assetValue);

        // Player is not on the market anymore
        setTransferValue(Setup.PLAYER_NO_TRANSFER_VALUE);

        return RestOutput.OK;
    }

    @Override
    public int compareTo(PlayerHandler playerHandler) {

        return Objects.compare(playerId(), playerHandler.playerId(), PlayerId::compareTo);
    }

    @Override
    public String toString() {
        return "PlayerHandler [_playerId=" + _playerId
               + ", _type="
               + _type
               + ", _firstName="
               + _firstName
               + ", _lastName="
               + _lastName
               + ", _country="
               + _country
               + ", _age="
               + _age
               + ", _assetValue="
               + _assetValue
               + ", _transferValue="
               + _transferValue
               + "]";
    }

    public static RestOutput<PlayerHandler> with(Player player) {

        PlayerHandler playerHandler;

        if (Api.isNull(player,
                       player.getPlayerId(),
                       player.getPlayerType(),
                       player.getPlayerFirstName(),
                       player.getPlayerLastName(),
                       player.getPlayerCountry(),
                       player.getPlayerAge(),
                       player.getPlayerAssetValue(),
                       player.getPlayerTransferValue())) {
            return RestOutput.badRequest();
        }

        try {

            playerHandler = new PlayerHandler(player.getPlayerId(),
                                              player.getPlayerType(),
                                              player.getPlayerFirstName(),
                                              player.getPlayerLastName(),
                                              player.getPlayerCountry(),
                                              player.getPlayerAge(),
                                              player.getPlayerAssetValue(),
                                              player.getPlayerTransferValue());

            return RestOutput.ok(playerHandler);

        } catch (Throwable t) {
            Api.error(t, "Player failed. INTERNAL FAILURE", player);
            return RestOutput.internalFailure();
        }
    }

    public static RestOutput<Player> random(PlayerType playerType, TeamId teamId, Integer index) {

        PlayerId playerId;
        String firstName;
        String lastName;
        String country;
        Integer age;
        Long assetValue;
        Long transferValue;
        Player player;

        if (Api.isNull(playerType, teamId, index)) {
            return RestOutput.badRequest();
        }

        playerId = PlayerId.random();

        firstName = "firstName_" + index;
        lastName = "lastName_" + index;
        country = "country_" + index;

        age = Setup.DEFAULT_PLAYER_AGE_SUPPLIER.get();

        assetValue = Setup.DEFAULT_PLAYER_ASSET_VALUE;
        transferValue = Setup.PLAYER_NO_TRANSFER_VALUE;

        player = new Player(playerId, playerType, firstName, lastName, country, age, assetValue, transferValue, teamId);

        return RestOutput.ok(player);
    }
}

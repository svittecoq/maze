package soccer.store.player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import soccer.base.Api;
import soccer.base.RestOutput;
import soccer.base.Result;
import soccer.model.PlayerId;
import soccer.model.PlayerType;
import soccer.model.TeamId;
import soccer.store.Collection;
import soccer.store.StoreService;

public class PlayerCollection extends Collection<PlayerRecord> {

    private static final String   COLLECTION            = "player_collection";

    private static final String   PLAYER_ID             = "player_id";
    private static final String   PLAYER_TYPE           = "player_type";
    private static final String   PLAYER_FIRST_NAME     = "player_first_name";
    private static final String   PLAYER_LAST_NAME      = "player_last_name";
    private static final String   PLAYER_COUNTRY        = "player_country";
    private static final String   PLAYER_AGE            = "player_age";

    private static final String   PLAYER_ASSET_VALUE    = "player_asset_value";
    private static final String   PLAYER_TRANSFER_VALUE = "player_transfer_value";

    private static final String   TEAM_ID               = "team_id";

    private static final String[] FieldArray            = new String[] { PLAYER_ID,
                                                                         PLAYER_TYPE,
                                                                         PLAYER_FIRST_NAME,
                                                                         PLAYER_LAST_NAME,
                                                                         PLAYER_COUNTRY,
                                                                         PLAYER_AGE,
                                                                         PLAYER_ASSET_VALUE,
                                                                         PLAYER_TRANSFER_VALUE,
                                                                         TEAM_ID };

    public PlayerCollection(StoreService storeService) {
        super(COLLECTION, FieldArray, storeService);
    }

    @Override
    protected String[] primaryFields() {

        return new String[] { PLAYER_ID };
    }

    @Override
    protected Map<String, String> updateFields(PlayerRecord record) {

        return Map.of(PLAYER_FIRST_NAME,
                      record.firstName(),
                      PLAYER_LAST_NAME,
                      record.lastName(),
                      PLAYER_COUNTRY,
                      record.country(),
                      TEAM_ID,
                      record.teamId().toText());
    }

    @Override
    protected PlayerRecord to(ResultSet resultSet) throws SQLException {

        PlayerId playerId;
        PlayerType type;
        String firstName;
        String lastName;
        String country;
        Integer age;
        Long assetValue;
        Long transferValue;
        TeamId teamId;

        if (Api.isNull(resultSet)) {
            return null;
        }

        playerId = new PlayerId(UUID.fromString(resultSet.getString(1)));

        type = PlayerType.valueOf(resultSet.getString(2));
        firstName = resultSet.getString(3);
        lastName = resultSet.getString(4);
        country = resultSet.getString(5);
        age = resultSet.getInt(6);
        assetValue = resultSet.getLong(7);
        transferValue = resultSet.getLong(8);

        teamId = new TeamId(UUID.fromString(resultSet.getString(9)));

        return new PlayerRecord(playerId, type, firstName, lastName, country, age, assetValue, transferValue, teamId);
    }

    @Override
    protected String[] from(PlayerRecord playerRecord) {

        if (Api.isNull(playerRecord)) {
            return null;
        }

        return new String[] { playerRecord.playerId().toText(),
                              playerRecord.playerType().toString(),
                              playerRecord.firstName(),
                              playerRecord.lastName(),
                              playerRecord.country(),
                              playerRecord.age().toString(),
                              playerRecord.assetValue().toString(),
                              playerRecord.transferValue().toString(),
                              playerRecord.teamId().toText() };
    }

    public RestOutput<Result> initCollection() {

        return init(textEntry(PLAYER_ID),
                    textEntry(PLAYER_TYPE),
                    textEntry(PLAYER_FIRST_NAME),
                    textEntry(PLAYER_LAST_NAME),
                    textEntry(PLAYER_COUNTRY),
                    textEntry(PLAYER_AGE),
                    textEntry(PLAYER_ASSET_VALUE),
                    textEntry(PLAYER_TRANSFER_VALUE),
                    textEntry(TEAM_ID),
                    primaryKeyEntry(PLAYER_ID));
    }

    public RestOutput<Result> storePlayerRecord(PlayerRecord playerRecord) {

        return storeRecord(playerRecord);
    }

    public RestOutput<List<PlayerRecord>> loadPlayerRecords() {

        return loadRecords();
    }
}

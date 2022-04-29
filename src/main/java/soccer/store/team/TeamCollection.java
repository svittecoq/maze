package soccer.store.team;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import soccer.base.Api;
import soccer.base.RestOutput;
import soccer.base.Result;
import soccer.model.TeamId;
import soccer.store.Collection;
import soccer.store.StoreService;

public class TeamCollection extends Collection<TeamRecord> {

    private static final String   COLLECTION   = "team_collection";

    private static final String   USER_ID      = "user_id";
    private static final String   TEAM_ID      = "team_id";
    private static final String   TEAM_NAME    = "team_name";
    private static final String   TEAM_COUNTRY = "team_country";
    private static final String   TEAM_BALANCE = "team_balance";

    private static final String[] FieldArray   = new String[] { USER_ID,
                                                                TEAM_ID,
                                                                TEAM_NAME,
                                                                TEAM_COUNTRY,
                                                                TEAM_BALANCE };

    public TeamCollection(StoreService storeService) {
        super(COLLECTION, FieldArray, storeService);
    }

    @Override
    protected String[] primaryFields() {

        return new String[] { USER_ID, TEAM_ID };
    }

    @Override
    protected Map<String, String> updateFields(TeamRecord record) {

        return Map.of(TEAM_NAME, record.name(), TEAM_COUNTRY, record.country());
    }

    @Override
    protected TeamRecord to(ResultSet resultSet) throws SQLException {

        String userId;
        TeamId teamId;
        String name;
        String country;
        Long balance;

        if (Api.isNull(resultSet)) {
            return null;
        }

        userId = resultSet.getString(1);
        teamId = new TeamId(UUID.fromString(resultSet.getString(2)));
        name = resultSet.getString(3);
        country = resultSet.getString(4);
        balance = resultSet.getLong(5);

        return new TeamRecord(userId, teamId, name, country, balance);
    }

    @Override
    protected String[] from(TeamRecord teamRecord) {

        if (Api.isNull(teamRecord)) {
            return null;
        }

        return new String[] { teamRecord.userId(),
                              teamRecord.teamId().toText(),
                              teamRecord.name(),
                              teamRecord.country(),
                              teamRecord.balance().toString() };
    }

    public RestOutput<Result> initCollection() {

        return init(textEntry(USER_ID),
                    textEntry(TEAM_ID),
                    textEntry(TEAM_NAME),
                    textEntry(TEAM_COUNTRY),
                    textEntry(TEAM_BALANCE),
                    primaryKeyEntry(USER_ID, TEAM_ID));
    }

    public RestOutput<Result> storeTeamRecord(TeamRecord teamRecord) {

        return storeRecord(teamRecord);
    }

    public RestOutput<List<TeamRecord>> loadTeamRecords() {

        return loadRecords();
    }
}

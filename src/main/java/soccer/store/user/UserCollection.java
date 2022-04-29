package soccer.store.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import soccer.base.Api;
import soccer.base.RestOutput;
import soccer.base.Result;
import soccer.store.Collection;
import soccer.store.StoreService;

public class UserCollection extends Collection<UserRecord> {

    private static final String   COLLECTION    = "user_collection";

    private static final String   USER_ID       = "user_id";
    private static final String   USER_PASSWORD = "user_password";

    private static final String[] FieldArray    = new String[] { USER_ID, USER_PASSWORD };

    public UserCollection(StoreService storeService) {
        super(COLLECTION, FieldArray, storeService);

        // TODO UserCollection stores UserPassword in clear text.
    }

    @Override
    protected String[] primaryFields() {

        return new String[] { USER_ID };
    }

    @Override
    protected Map<String, String> updateFields(UserRecord record) {

        return Map.of();
    }

    @Override
    protected UserRecord to(ResultSet resultSet) throws SQLException {

        if (Api.isNull(resultSet)) {
            return null;
        }

        return new UserRecord(resultSet.getString(1), resultSet.getString(2));
    }

    @Override
    protected String[] from(UserRecord userRecord) {

        if (Api.isNull(userRecord)) {
            return null;
        }

        return new String[] { userRecord.userId(), userRecord.userPassword() };
    }

    public RestOutput<Result> initCollection() {

        return init(textEntry(USER_ID), textEntry(USER_PASSWORD), primaryKeyEntry(USER_ID));
    }

    public RestOutput<Result> storeUserRecord(UserRecord userRecord) {

        return storeRecord(userRecord);
    }

    public RestOutput<List<UserRecord>> loadUserRecords() {

        return loadRecords();
    }
}

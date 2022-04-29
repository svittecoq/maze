package maze.store.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import maze.base.Api;
import maze.base.RestOutput;
import maze.base.Result;
import maze.store.Collection;
import maze.store.StoreService;

public class UserCollection extends Collection<UserRecord> {

    private static final String   COLLECTION   = "user_collection";

    private static final String   UserId       = "user_id";
    private static final String   UserPassword = "user_password";

    private static final String[] FieldArray   = new String[] { UserId, UserPassword };

    public UserCollection(StoreService storeService) {
        super(COLLECTION, FieldArray, storeService);

        // UserCollection stores UserPassword in clear text.
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

        return init(textEntry(UserId), textEntry(UserPassword), primaryKeyEntry(UserId));
    }

    public RestOutput<Result> storeUserRecord(UserRecord userRecord) {

        return storeRecord(userRecord);
    }

    public RestOutput<List<UserRecord>> loadUserRecords() {

        return loadRecords();
    }
}

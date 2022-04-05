package maze.store;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import maze.base.Api;
import maze.base.RestOutput;
import maze.base.Result;
import maze.store.maze.MazeCollection;
import maze.store.maze.MazeRecord;
import maze.store.user.UserCollection;
import maze.store.user.UserRecord;

public class StoreService {

    private final String         _storeUrl;
    private final String         _storeUser;
    private final String         _storePassword;
    private final String         _databaseId;
    private final String         _databaseUrl;

    private final UserCollection _userCollection;
    private final MazeCollection _mazeCollection;

    public StoreService(String storeUrl, String storeUser, String storePassword, String databaseId) {

        _storeUrl = storeUrl;
        _storeUser = storeUser;
        _storePassword = storePassword;

        _databaseId = databaseId;
        _databaseUrl = storeUrl() + "/" + databaseId;

        _userCollection = new UserCollection(this);
        _mazeCollection = new MazeCollection(this);
    }

    public String storeUrl() {
        return _storeUrl;
    }

    public String storeUser() {
        return _storeUser;
    }

    public String storePassword() {
        return _storePassword;
    }

    public String databaseId() {
        return _databaseId;
    }

    public String databaseUrl() {
        return _databaseUrl;
    }

    private UserCollection userCollection() {
        return _userCollection;
    }

    private MazeCollection mazeCollection() {
        return _mazeCollection;
    }

    public RestOutput<Result> start() {

        RestOutput<Result> resultOutput;
        String storeUrl;

        storeUrl = storeUrl() + "/postgres";

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException t) {
            Api.error("Failure to load the postgresql Driver. INTERNAL FAILURE");
            return RestOutput.internalFailure();
        }

        try (Connection connection = DriverManager.getConnection(storeUrl, storeUser(), storePassword());
                Statement statement = connection.createStatement()) {

            // Check if the database exists already or not
            try (ResultSet selectResultSet = statement.executeQuery("SELECT 1 FROM pg_database WHERE datname ='"
                                                                    + databaseId()
                                                                    + "';")) {

                if (selectResultSet.next()) {
                    Api.info("Database " + _databaseId + " exists already", this);
                } else {
                    // Create the database
                    statement.executeUpdate("CREATE DATABASE \"" + _databaseId + "\";");
                    Api.info("Database " + _databaseId + " created", this);
                }
            }
        } catch (Throwable t) {
            Api.error(t, "Failure to open database connection. INTERNAL FAILURE", this);
            return RestOutput.internalFailure();
        }

        // Initialize the User Collection
        resultOutput = userCollection().initCollection();
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("Init UserCollection is NOT OK", resultOutput, this);
            return RestOutput.of(resultOutput);
        }

        // Initialize the Maze Collection
        resultOutput = mazeCollection().initCollection();
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("Init MazeCollection is NOT OK", resultOutput, this);
            return RestOutput.of(resultOutput);
        }

        return RestOutput.OK;
    }

    public RestOutput<Result> stop() {

        return RestOutput.OK;
    }

    public RestOutput<Result> storeUserRecord(UserRecord userRecord) {

        return userCollection().storeUserRecord(userRecord);
    }

    public RestOutput<List<UserRecord>> loadUserRecords() {

        return userCollection().loadUserRecords();
    }

    public RestOutput<Result> storeMazeRecord(MazeRecord mazeRecord) {

        return mazeCollection().storeMazeRecord(mazeRecord);
    }

    public RestOutput<List<MazeRecord>> loadMazeRecords() {

        return mazeCollection().loadMazeRecords();
    }

    @Override
    public String toString() {
        return "StoreService [_databaseId=" + _databaseId
               + ", _storeUrl="
               + _storeUrl
               + ", _storeUser="
               + _storeUser
               + ", _storePassword="
               + _storePassword
               + "]";
    }
}

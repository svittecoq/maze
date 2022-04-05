package maze.store;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import maze.Setup;
import maze.base.Api;
import maze.base.RestOutput;
import maze.base.Result;
import maze.store.maze.MazeCollection;
import maze.store.maze.MazeRecord;
import maze.store.user.UserCollection;
import maze.store.user.UserRecord;

public class StoreService {

    private final URI            _databaseURI;
    private final String         _storeUser;
    private final String         _storePassword;
    private final String         _databaseUrl;
    private final String         _databaseId;

    private final UserCollection _userCollection;
    private final MazeCollection _mazeCollection;

    public StoreService(URI databaseURI) {

        _databaseURI = databaseURI;

        _storeUser = databaseURI().getUserInfo().split(":")[0];
        _storePassword = databaseURI().getUserInfo().split(":")[1];

        _databaseUrl = "jdbc:postgresql://" + databaseURI().getHost()
                       + ':'
                       + databaseURI().getPort()
                       + databaseURI().getPath()
                       + "?sslmode=require";

        String path = databaseURI().getPath();
        _databaseId = path.substring(path.lastIndexOf("/") + 1);

        _userCollection = new UserCollection(this);
        _mazeCollection = new MazeCollection(this);
    }

    public URI databaseURI() {
        return _databaseURI;
    }

    public String storeUser() {
        return _storeUser;
    }

    public String storePassword() {
        return _storePassword;
    }

    public String databaseUrl() {
        return _databaseUrl;
    }

    public String databaseId() {
        return _databaseId;
    }

    private UserCollection userCollection() {
        return _userCollection;
    }

    private MazeCollection mazeCollection() {
        return _mazeCollection;
    }

    private RestOutput<Result> createDatabase() {

        String storeUrl;

        if (databaseId().startsWith(Setup.DATABASE_PREFIX) == false) {
            // Database already created
            return RestOutput.OK;
        }

        storeUrl = databaseUrl().replace(databaseId(), "postgres");

        try (Connection connection = DriverManager.getConnection(storeUrl, storeUser(), storePassword());
                Statement statement = connection.createStatement()) {

            // Check if the database exists already or not
            try (ResultSet selectResultSet = statement.executeQuery("SELECT 1 FROM pg_database WHERE datname ='"
                                                                    + databaseId()
                                                                    + "';")) {

                if (selectResultSet.next()) {
                    Api.info("Database " + databaseId() + " exists already", this);
                } else {
                    // Create the database
                    statement.executeUpdate("CREATE DATABASE \"" + databaseId() + "\";");
                    Api.info("Database " + databaseId() + " created", this);
                }
            }
        } catch (Throwable t) {
            Api.error(t, "Failure to create database. INTERNAL FAILURE", this);
            return RestOutput.internalFailure();
        }

        return RestOutput.OK;
    }

    private RestOutput<Result> dropDatabase() {

        String storeUrl;
        String updateSQL;

        if (databaseId().startsWith(Setup.DATABASE_PREFIX) == false) {
            // Database can not be dropped
            return RestOutput.OK;
        }

        storeUrl = databaseUrl().replace(databaseId(), "postgres");

        try (Connection connection = DriverManager.getConnection(storeUrl, storeUser(), storePassword());
                Statement statement = connection.createStatement()) {

            updateSQL = "DROP DATABASE IF EXISTS \"" + databaseId() + "\"";

            Api.info("Droping Database : " + databaseId());

            statement.executeUpdate(updateSQL);

            Api.info("Database " + databaseId() + " DROPPED");

        } catch (Throwable t) {
            Api.error(t, "Failure to drop database. INTERNAL FAILURE", this);
            return RestOutput.internalFailure();
        }

        return RestOutput.OK;
    }

    public RestOutput<Result> start() {

        RestOutput<Result> resultOutput;

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException t) {
            Api.error("Failure to load the postgresql Driver. INTERNAL FAILURE");
            return RestOutput.internalFailure();
        }

        // Create the database if needed
        resultOutput = createDatabase();
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("createDatabase is NOT OK", resultOutput, this);
            return RestOutput.of(resultOutput);
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

    public RestOutput<Result> stop(boolean dropDatabase) {

        if (dropDatabase) {
            return dropDatabase();
        }
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
        return "StoreService [_databaseURI=" + _databaseURI
               + ", _storeUser="
               + _storeUser
               + ", _storePassword="
               + _storePassword
               + ", _databaseUrl="
               + _databaseUrl
               + ", _databaseId="
               + _databaseId
               + ", _userCollection="
               + _userCollection
               + ", _mazeCollection="
               + _mazeCollection
               + "]";
    }
}

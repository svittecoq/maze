package maze.handler.core;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import maze.Setup;
import maze.base.Api;
import maze.base.RestOutput;
import maze.base.Result;
import maze.handler.maze.MazeHandler;
import maze.handler.session.SessionHandler;
import maze.handler.user.UserHandler;
import maze.http.HttpService;
import maze.http.servlet.DashboardServlet;
import maze.http.servlet.LoginServlet;
import maze.model.Maze;
import maze.model.MazeCreation;
import maze.model.MazeSolution;
import maze.model.SessionToken;
import maze.model.User;
import maze.rest.RestService;
import maze.store.StoreService;
import maze.store.maze.MazeRecord;
import maze.store.user.UserRecord;

public class CoreHandler {

    private final ConcurrentHashMap<String, UserHandler>          _userHandlerMap;
    private final ConcurrentHashMap<SessionToken, SessionHandler> _sessionHandlerMap;
    private final StoreService                                    _storeService;
    private final RestService                                     _restService;
    private final HttpService                                     _httpService;

    private CoreHandler(String storeUrl,
                        String storeUser,
                        String storePassword,
                        String databaseId,
                        Optional<String> webUrlOptional) {

        _userHandlerMap = new ConcurrentHashMap<String, UserHandler>();
        _sessionHandlerMap = new ConcurrentHashMap<SessionToken, SessionHandler>();
        _storeService = new StoreService(storeUrl, storeUser, storePassword, databaseId);
        _restService = new RestService(this);
        _httpService = new HttpService(webUrlOptional,
                                       restService(),
                                       List.of(new LoginServlet(this), new DashboardServlet(this)),
                                       Setup.WEB_PATH);
    }

    private ConcurrentHashMap<String, UserHandler> userHandlerMap() {

        return _userHandlerMap;
    }

    private ConcurrentHashMap<SessionToken, SessionHandler> sessionHandlerMap() {

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

    private RestOutput<Result> storeUserRecord(UserRecord userRecord) {

        return storeService().storeUserRecord(userRecord);
    }

    private RestOutput<List<UserRecord>> loadUserRecords() {

        return storeService().loadUserRecords();
    }

    private RestOutput<Result> storeMazeRecord(MazeRecord mazeRecord) {

        return storeService().storeMazeRecord(mazeRecord);
    }

    private RestOutput<List<MazeRecord>> loadMazeRecords() {

        return storeService().loadMazeRecords();
    }

    private RestOutput<UserHandler> findUserHandler(SessionToken sessionToken) {

        SessionHandler sessionHandler;

        if (Api.isNull(sessionToken)) {
            return RestOutput.badRequest();
        }

        sessionHandler = sessionHandlerMap().get(sessionToken);
        if (sessionHandler == null) {
            Api.error("This session does not exist anymore. FORBIDDEN", sessionToken, this);
            return RestOutput.forbidden();
        }

        if (sessionHandler.hasTimedOut()) {
            Api.error("This session has timed out. FORBIDDEN", sessionToken, sessionHandler, this);
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
        RestOutput<UserRecord> userRecordOutput;
        UserRecord userRecord;
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

            // Build the UserRecord to store
            userRecordOutput = userHandler.buildUserRecord();
            if (RestOutput.isNOK(userRecordOutput)) {
                Api.error("storeUserRecord to SignUpUser is NOT OK", userRecordOutput, userHandler, user, this);
                return RestOutput.of(userRecordOutput);
            }
            userRecord = userRecordOutput.output();

            // Store this new user in the store
            resultOutput = storeUserRecord(userRecord);
            if (RestOutput.isNOK(resultOutput)) {
                Api.error("storeUserRecord to SignUpUser is NOT OK", resultOutput, userRecord, userHandler, user, this);
                return RestOutput.of(resultOutput);
            }
        }
        return RestOutput.ok(userHandler);
    }

    public RestOutput<SessionToken> signUpUser(User user) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;
        SessionToken sessionToken;
        RestOutput<SessionHandler> sessionHandlerOutput;
        SessionHandler sessionHandler;

        if (Api.isNull(user, user.getUsername(), user.getPassword())) {
            return RestOutput.badRequest();
        }

        userHandlerOutput = addUser(user, Boolean.TRUE);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("addUser to signUpUser is NOT OK", userHandlerOutput, user, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        // Generate a new SessionToken for this User signing up
        sessionToken = SessionToken.random();

        sessionHandlerOutput = SessionHandler.with(sessionToken, userHandler);
        if (RestOutput.isNOK(sessionHandlerOutput)) {
            Api.error("SessionHandler to signUpUser is NOT OK",
                      sessionHandlerOutput,
                      sessionToken,
                      userHandler,
                      user,
                      this);
            return RestOutput.of(sessionHandlerOutput);
        }
        sessionHandler = sessionHandlerOutput.output();

        if (sessionHandlerMap().putIfAbsent(sessionToken, sessionHandler) != null) {
            Api.error("SessionHandler to signUpUser is a duplicate. INTERNAL FAILURE",
                      sessionToken,
                      userHandler,
                      user,
                      this);
            return RestOutput.internalFailure();
        }

        return RestOutput.ok(sessionHandler.sessionToken());
    }

    public RestOutput<SessionToken> loginUser(User user) {

        UserHandler userHandler;
        SessionToken sessionToken;
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

        // Generate a new SessionToken for this User signing up
        sessionToken = SessionToken.random();

        sessionHandlerOutput = SessionHandler.with(sessionToken, userHandler);
        if (RestOutput.isNOK(sessionHandlerOutput)) {
            Api.error("SessionHandler to loginUser is NOT OK",
                      sessionHandlerOutput,
                      sessionToken,
                      userHandler,
                      user,
                      this);
            return RestOutput.of(sessionHandlerOutput);
        }
        sessionHandler = sessionHandlerOutput.output();

        if (sessionHandlerMap().putIfAbsent(sessionToken, sessionHandler) != null) {
            Api.error("SessionHandler to loginUser is a duplicate. INTERNAL FAILURE",
                      sessionToken,
                      userHandler,
                      user,
                      this);
            return RestOutput.internalFailure();
        }

        return RestOutput.ok(sessionHandler.sessionToken());
    }

    public RestOutput<Result> validateSessionToken(SessionToken sessionToken) {

        RestOutput<UserHandler> userHandlerOutput;

        if (Api.isNull(sessionToken)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the SessionToken
        userHandlerOutput = findUserHandler(sessionToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("findUserHandler to validateSessionToken is NOT OK", userHandlerOutput, sessionToken, this);
            return RestOutput.of(userHandlerOutput);
        }

        return RestOutput.OK;
    }

    public RestOutput<MazeCreation> addMaze(UserHandler userHandler, Maze maze, Boolean addToStore) {

        AtomicReference<String> errorReference;
        String error;
        RestOutput<MazeHandler> mazeHandlerOutput;
        MazeHandler mazeHandler;
        RestOutput<Result> resultOutput;
        RestOutput<MazeRecord> mazeRecordOutput;
        MazeRecord mazeRecord;

        if (Api.isNull(userHandler, maze.getEntrance(), maze.getGridSize(), maze.getWalls(), addToStore)) {
            return RestOutput.ok(new MazeCreation("Attributes to create the maze are missing."));
        }

        errorReference = new AtomicReference<String>(null);

        mazeHandlerOutput = userHandler.addMaze(maze, errorReference);
        if (RestOutput.isBadRequest(mazeHandlerOutput)) {
            error = errorReference.get();
            if (error == null) {
                Api.error("addMaze failed without error. INTERNAL FAILURE", mazeHandlerOutput, userHandler, maze, this);
                return RestOutput.internalFailure();
            }
            return RestOutput.ok(new MazeCreation(error));
        }
        if (RestOutput.isNOK(mazeHandlerOutput)) {
            Api.error("addMaze is NOT OK", mazeHandlerOutput, userHandler, maze, errorReference, this);
            return RestOutput.of(mazeHandlerOutput);
        }
        mazeHandler = mazeHandlerOutput.output();

        if (addToStore) {

            // Build the MazeRecord
            mazeRecordOutput = mazeHandler.buildMazeRecord(userHandler.userId());
            if (RestOutput.isNOK(mazeRecordOutput)) {
                Api.error("buildMazeRecord to addMaze is NOT OK", mazeRecordOutput, userHandler, maze, this);
                return RestOutput.ok(new MazeCreation("Created maze could not be converted into a record."));
            }
            mazeRecord = mazeRecordOutput.output();

            // Store the Maze
            resultOutput = storeMazeRecord(mazeRecord);
            if (RestOutput.isNOK(resultOutput)) {
                Api.error("storeMazeRecord to addMaze is NOT OK", resultOutput, mazeRecord, userHandler, maze, this);
                return RestOutput.ok(new MazeCreation("Created maze could not be stored."));
            }
        }

        // Return a successful maze creation
        return RestOutput.ok(new MazeCreation(mazeHandler.mazeId()));
    }

    public RestOutput<MazeCreation> addMaze(SessionToken sessionToken, Maze maze) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;

        if (Api.isNull(sessionToken, maze.getEntrance(), maze.getGridSize(), maze.getWalls())) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the SessionToken
        userHandlerOutput = findUserHandler(sessionToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("SessionHandler to addMaze is NOT OK", userHandlerOutput, sessionToken, maze, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        return addMaze(userHandler, maze, Boolean.TRUE);
    }

    public RestOutput<Maze> retrieveMaze(SessionToken sessionToken, Integer mazeId) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;

        if (Api.isNull(sessionToken, mazeId)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the SessionToken
        userHandlerOutput = findUserHandler(sessionToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("SessionHandler to retrieveMaze is NOT OK", userHandlerOutput, sessionToken, mazeId, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        return userHandler.retrieveMaze(mazeId);
    }

    public RestOutput<Maze[]> retrieveMazes(SessionToken sessionToken) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;

        if (Api.isNull(sessionToken)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the SessionToken
        userHandlerOutput = findUserHandler(sessionToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("SessionHandler to retrieveMazes is NOT OK", userHandlerOutput, sessionToken, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        return userHandler.retrieveMazes();
    }

    public RestOutput<MazeSolution> solveMinPath(SessionToken sessionToken, Integer mazeId) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;

        if (Api.isNull(sessionToken, mazeId)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the SessionToken
        userHandlerOutput = findUserHandler(sessionToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("SessionHandler to solveMinPath is NOT OK", userHandlerOutput, sessionToken, mazeId, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        return userHandler.solveMinPath(mazeId);
    }

    public RestOutput<MazeSolution> solveMaxPath(SessionToken sessionToken, Integer mazeId) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;

        if (Api.isNull(sessionToken, mazeId)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the SessionToken
        userHandlerOutput = findUserHandler(sessionToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("SessionHandler to solveMaxPath is NOT OK", userHandlerOutput, sessionToken, mazeId, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        return userHandler.solveMaxPath(mazeId);
    }

    public RestOutput<Result> run() {

        RestOutput<Result> resultOutput;
        RestOutput<UserHandler> addUserOutput;
        RestOutput<List<UserRecord>> userRecordListOutput;
        List<UserRecord> userRecordList;
        User user;
        UserHandler userHandler;
        RestOutput<List<MazeRecord>> mazeRecordListOutput;
        List<MazeRecord> mazeRecordList;
        Maze maze;
        RestOutput<MazeCreation> mazeCreationOutput;
        MazeCreation mazeCreation;

        // Start the StoreService
        resultOutput = storeService().start();
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("Start StoreService is NOT OK", resultOutput, this);
            return RestOutput.of(resultOutput);
        }

        // Load all users
        userRecordListOutput = loadUserRecords();
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

        // Load all mazes
        mazeRecordListOutput = loadMazeRecords();
        if (RestOutput.isNOK(mazeRecordListOutput)) {
            Api.error("loadMazeRecords is NOT OK", mazeRecordListOutput, this);
            return RestOutput.of(mazeRecordListOutput);
        }
        mazeRecordList = mazeRecordListOutput.output();

        for (MazeRecord mazeRecord : mazeRecordList) {

            userHandler = userHandlerMap().get(mazeRecord.userId());
            if (userHandler == null) {
                Api.error("User does not exist to restore Maze. Maze skippped", mazeRecord, this);
                continue;
            }

            maze = new Maze(mazeRecord.mazeId(), mazeRecord.entrance(), mazeRecord.gridSize(), mazeRecord.walls());

            // Add each maze for its respective user
            mazeCreationOutput = addMaze(userHandler, maze, Boolean.FALSE);
            if (RestOutput.isNOK(mazeCreationOutput)) {
                Api.error("addMaze to run failed. Maze skipped",
                          mazeCreationOutput,
                          maze,
                          mazeRecord,
                          userHandler,
                          this);
                continue;
            }
            mazeCreation = mazeCreationOutput.output();
            // The maze should be re created without error
            if (mazeCreation.getError() != null) {
                Api.error("addMaze to run failed. Maze skipped", mazeCreation, maze, mazeRecord, userHandler, this);
                continue;
            }
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

    @Override
    public String toString() {
        return "CoreHandler [_userHandlerMap=" + _userHandlerMap + ", _sessionHandlerMap=" + _sessionHandlerMap + "]";
    }

    public static RestOutput<CoreHandler> with(Optional<String> storeUrlOptional,
                                               Optional<String> storeUserOptional,
                                               Optional<String> storePasswordOptional,
                                               Optional<String> databaseIdOptional,
                                               Optional<String> webUrlOptional) {

        CoreHandler coreHandler;
        String storeUrl;
        String storeUser;
        String storePassword;
        String databaseId;

        if (Api.isNull(storeUrlOptional,
                       storeUserOptional,
                       storePasswordOptional,
                       databaseIdOptional,
                       webUrlOptional)) {
            return RestOutput.badRequest();
        }

        storeUrl = storeUrlOptional.orElse(Setup.STORE_URL);
        storeUser = storeUserOptional.orElse(Setup.STORE_USER);
        storePassword = storePasswordOptional.orElse(Setup.STORE_PASSWORD);
        databaseId = databaseIdOptional.orElse(Setup.DATABASE_ID);

        coreHandler = new CoreHandler(storeUrl, storeUser, storePassword, databaseId, webUrlOptional);

        return RestOutput.ok(coreHandler);
    }
}

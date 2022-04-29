package maze.handler.core;

import java.net.URI;
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
import maze.model.User;
import maze.model.UserToken;
import maze.rest.RestService;
import maze.store.StoreService;
import maze.store.maze.MazeRecord;
import maze.store.user.UserRecord;

public class CoreHandler {

    private final ConcurrentHashMap<String, UserHandler>       _userHandlerMap;
    private final ConcurrentHashMap<UserToken, SessionHandler> _sessionHandlerMap;
    private final StoreService                                 _storeService;
    private final RestService                                  _restService;
    private final HttpService                                  _httpService;

    private CoreHandler(URI databaseURI, Optional<String> webPathOptional, Optional<Integer> webPortOptional) {

        _userHandlerMap = new ConcurrentHashMap<String, UserHandler>();
        _sessionHandlerMap = new ConcurrentHashMap<UserToken, SessionHandler>();
        _storeService = new StoreService(databaseURI);
        _restService = new RestService(this);
        _httpService = new HttpService(webPathOptional,
                                       webPortOptional,
                                       restService(),
                                       List.of(new LoginServlet(this), new DashboardServlet(this)),
                                       Setup.WEB_PATH);
    }

    private ConcurrentHashMap<String, UserHandler> userHandlerMap() {

        return _userHandlerMap;
    }

    private ConcurrentHashMap<UserToken, SessionHandler> sessionHandlerMap() {

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

    private RestOutput<UserHandler> findUserHandler(UserToken userToken) {

        SessionHandler sessionHandler;

        if (Api.isNull(userToken)) {
            return RestOutput.badRequest();
        }

        sessionHandler = sessionHandlerMap().get(userToken);
        if (sessionHandler == null) {
            Api.error("This session does not exist anymore. FORBIDDEN", userToken, this);
            return RestOutput.forbidden();
        }

        if (sessionHandler.hasTimedOut()) {
            Api.error("This session has timed out. FORBIDDEN", userToken, sessionHandler, this);
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

    public RestOutput<UserToken> signUpUser(User user) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;
        UserToken userToken;
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

        // Generate a new UserToken for this User signing up
        userToken = UserToken.random();

        sessionHandlerOutput = SessionHandler.with(userToken, userHandler);
        if (RestOutput.isNOK(sessionHandlerOutput)) {
            Api.error("SessionHandler to signUpUser is NOT OK",
                      sessionHandlerOutput,
                      userToken,
                      userHandler,
                      user,
                      this);
            return RestOutput.of(sessionHandlerOutput);
        }
        sessionHandler = sessionHandlerOutput.output();

        if (sessionHandlerMap().putIfAbsent(userToken, sessionHandler) != null) {
            Api.error("SessionHandler to signUpUser is a duplicate. INTERNAL FAILURE",
                      userToken,
                      userHandler,
                      user,
                      this);
            return RestOutput.internalFailure();
        }

        return RestOutput.ok(sessionHandler.userToken());
    }

    public RestOutput<UserToken> loginUser(User user) {

        UserHandler userHandler;
        UserToken userToken;
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

        // Generate a new UserToken for this User signing up
        userToken = UserToken.random();

        sessionHandlerOutput = SessionHandler.with(userToken, userHandler);
        if (RestOutput.isNOK(sessionHandlerOutput)) {
            Api.error("SessionHandler to loginUser is NOT OK",
                      sessionHandlerOutput,
                      userToken,
                      userHandler,
                      user,
                      this);
            return RestOutput.of(sessionHandlerOutput);
        }
        sessionHandler = sessionHandlerOutput.output();

        if (sessionHandlerMap().putIfAbsent(userToken, sessionHandler) != null) {
            Api.error("SessionHandler to loginUser is a duplicate. INTERNAL FAILURE",
                      userToken,
                      userHandler,
                      user,
                      this);
            return RestOutput.internalFailure();
        }

        return RestOutput.ok(sessionHandler.userToken());
    }

    public RestOutput<Result> validateUserToken(UserToken userToken) {

        RestOutput<UserHandler> userHandlerOutput;

        if (Api.isNull(userToken)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the UserToken
        userHandlerOutput = findUserHandler(userToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("findUserHandler to validateUserToken is NOT OK", userHandlerOutput, userToken, this);
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

    public RestOutput<MazeCreation> addMaze(UserToken userToken, Maze maze) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;

        if (Api.isNull(userToken, maze.getEntrance(), maze.getGridSize(), maze.getWalls())) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the UserToken
        userHandlerOutput = findUserHandler(userToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("SessionHandler to addMaze is NOT OK", userHandlerOutput, userToken, maze, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        return addMaze(userHandler, maze, Boolean.TRUE);
    }

    public RestOutput<Maze> retrieveMaze(UserToken userToken, Integer mazeId) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;

        if (Api.isNull(userToken, mazeId)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the UserToken
        userHandlerOutput = findUserHandler(userToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("SessionHandler to retrieveMaze is NOT OK", userHandlerOutput, userToken, mazeId, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        return userHandler.retrieveMaze(mazeId);
    }

    public RestOutput<Maze[]> retrieveMazes(UserToken userToken) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;

        if (Api.isNull(userToken)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the UserToken
        userHandlerOutput = findUserHandler(userToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("SessionHandler to retrieveMazes is NOT OK", userHandlerOutput, userToken, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        return userHandler.retrieveMazes();
    }

    public RestOutput<MazeSolution> solveMinPath(UserToken userToken, Integer mazeId) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;

        if (Api.isNull(userToken, mazeId)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the UserToken
        userHandlerOutput = findUserHandler(userToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("SessionHandler to solveMinPath is NOT OK", userHandlerOutput, userToken, mazeId, this);
            return RestOutput.of(userHandlerOutput);
        }
        userHandler = userHandlerOutput.output();

        return userHandler.solveMinPath(mazeId);
    }

    public RestOutput<MazeSolution> solveMaxPath(UserToken userToken, Integer mazeId) {

        RestOutput<UserHandler> userHandlerOutput;
        UserHandler userHandler;

        if (Api.isNull(userToken, mazeId)) {
            return RestOutput.badRequest();
        }

        // Find the UserHandler from the UserToken
        userHandlerOutput = findUserHandler(userToken);
        if (RestOutput.isNOK(userHandlerOutput)) {
            Api.error("SessionHandler to solveMaxPath is NOT OK", userHandlerOutput, userToken, mazeId, this);
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

    public RestOutput<Result> terminate() {

        RestOutput<Result> resultOutput;

        // Stop the StoreService and drop the database
        resultOutput = storeService().stop(true);
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("Stop StoreService is NOT OK", resultOutput, this);
            return RestOutput.of(resultOutput);
        }

        // Stop the HttpService
        resultOutput = httpService().stop();
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("Stop HttpService is NOT OK", resultOutput, this);
            return RestOutput.of(resultOutput);
        }

        return RestOutput.OK;
    }

    @Override
    public String toString() {
        return "CoreHandler [_userHandlerMap=" + _userHandlerMap + ", _sessionHandlerMap=" + _sessionHandlerMap + "]";
    }

    public static RestOutput<CoreHandler> with(URI databaseURI,
                                               Optional<String> webPathOptional,
                                               Optional<Integer> webPortOptional) {

        CoreHandler coreHandler;

        if (Api.isNull(databaseURI, webPathOptional, webPortOptional)) {
            return RestOutput.badRequest();
        }

        coreHandler = new CoreHandler(databaseURI, webPathOptional, webPortOptional);

        return RestOutput.ok(coreHandler);
    }
}

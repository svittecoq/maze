package maze.handler.user;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import maze.Setup;
import maze.base.Api;
import maze.base.RestOutput;
import maze.handler.maze.MazeHandler;
import maze.model.Maze;
import maze.model.MazeSolution;
import maze.model.User;
import maze.store.user.UserRecord;

public class UserHandler {

    private final String                                  _userId;
    private final String                                  _userPassword;
    private final ConcurrentHashMap<Integer, MazeHandler> _mazeHandlerMap;
    private final AtomicInteger                           _mazeIdGenerator;

    private UserHandler(String userId, String userPassword) {

        _userId = userId;
        _userPassword = userPassword;
        _mazeHandlerMap = new ConcurrentHashMap<Integer, MazeHandler>();
        _mazeIdGenerator = new AtomicInteger(0);
    }

    public String userId() {

        return _userId;
    }

    private String userPassword() {

        return _userPassword;
    }

    private ConcurrentHashMap<Integer, MazeHandler> mazeHandlerMap() {

        return _mazeHandlerMap;
    }

    private AtomicInteger mazeIdGenerator() {

        return _mazeIdGenerator;
    }

    private void updateMazeIdGenerator(int mazeId) {

        mazeIdGenerator().updateAndGet(value -> value < mazeId ? mazeId : value);
    }

    private Integer supplyMazeId() {

        return Integer.valueOf(mazeIdGenerator().incrementAndGet());
    }

    public boolean matches(String userPassword) {

        return Objects.equals(userPassword(), userPassword);
    }

    public RestOutput<MazeHandler> addMaze(Maze maze, AtomicReference<String> errorReference) {

        Integer mazeId;
        Supplier<Integer> mazeIdSupplier;
        RestOutput<MazeHandler> mazeHandlerOutput;
        MazeHandler mazeHandler;

        if (Api.isNull(maze, errorReference)) {
            return RestOutput.badRequest();
        }

        if (maze.getMazeId() != null) {

            mazeId = maze.getMazeId();
            if (mazeId < 1) {
                Api.error("MazeId to addMaze is not valid. BAD REQUEST", maze, this);
                return RestOutput.badRequest();
            }
            updateMazeIdGenerator(mazeId.intValue());

            // We will supply exactly this MazeId
            mazeIdSupplier = () -> mazeId;
        } else {

            // We will supply a new MazeId if the maze is valid
            mazeIdSupplier = this::supplyMazeId;
        }

        // Create a MazeHandler
        mazeHandlerOutput = MazeHandler.with(maze, mazeIdSupplier, errorReference);
        if (RestOutput.isNOK(mazeHandlerOutput)) {
            Api.error("Maze to add is NOT OK", mazeHandlerOutput, maze, errorReference, this);
            return RestOutput.of(mazeHandlerOutput);
        }
        mazeHandler = mazeHandlerOutput.output();

        // Insert this new Maze in the Map
        if (mazeHandlerMap().putIfAbsent(mazeHandler.mazeId(), mazeHandler) != null) {
            Api.error("Maze to add is a duplicate. INTERNAL FAILURE", mazeHandler, maze, this);
            return RestOutput.internalFailure();
        }

        Api.info("New Maze added for " + userId() + " " + maze.toString(), this);

        return RestOutput.ok(mazeHandler);
    }

    public RestOutput<Maze> retrieveMaze(Integer mazeId) {

        MazeHandler mazeHandler;

        if (Api.isNull(mazeId)) {
            return RestOutput.badRequest();
        }

        mazeHandler = mazeHandlerMap().get(mazeId);
        if (mazeHandler == null) {
            Api.error("Maze does not exist for user. NOT FOUND", mazeId, this);
            return RestOutput.notFound();
        }

        return mazeHandler.retrieveMaze();
    }

    public RestOutput<Maze[]> retrieveMazes() {

        Maze[] mazeArray;

        // Sorted set of all mazes for this user from the last to first
        mazeArray = mazeHandlerMap().values()
                                    .stream()
                                    .sorted(Comparator.reverseOrder())
                                    .map(MazeHandler::retrieveMaze)
                                    .map(RestOutput::stream)
                                    .filter(Objects::nonNull)
                                    .toArray(Maze[]::new);

        return RestOutput.ok(mazeArray);
    }

    public RestOutput<MazeSolution> solveMinPath(Integer mazeId) {

        MazeHandler mazeHandler;

        if (Api.isNull(mazeId)) {
            return RestOutput.badRequest();
        }

        mazeHandler = mazeHandlerMap().get(mazeId);
        if (mazeHandler == null) {
            Api.error("Maze to solveMinPath does not exist for user.", mazeId, this);
            return RestOutput.ok(new MazeSolution("Maze " + mazeId + " does not exit for this user."));
        }

        return mazeHandler.solveMinPath();
    }

    public RestOutput<MazeSolution> solveMaxPath(Integer mazeId) {

        MazeHandler mazeHandler;

        if (Api.isNull(mazeId)) {
            return RestOutput.badRequest();
        }

        mazeHandler = mazeHandlerMap().get(mazeId);
        if (mazeHandler == null) {
            Api.error("Maze to solveMaxPath does not exist for user. NOT FOUND", mazeId, this);
            return RestOutput.notFound();
        }

        return mazeHandler.solveMaxPath();
    }

    public RestOutput<UserRecord> buildUserRecord() {

        UserRecord userRecord;

        userRecord = new UserRecord(userId(), userPassword());

        return RestOutput.ok(userRecord);
    }

    @Override
    public String toString() {
        return "UserHandler [_userId=" + _userId
               + ", _userPassword="
               + _userPassword
               + ", _mazeHandlerMap="
               + _mazeHandlerMap
               + ", _mazeIdGenerator="
               + _mazeIdGenerator
               + "]";
    }

    public static RestOutput<UserHandler> with(User user) {

        String userName;
        String userPassword;
        UserHandler userHandler;

        if (Api.isNull(user, user.getUsername(), user.getPassword())) {
            return RestOutput.badRequest();
        }

        try {

            // Check the validity of the password
            userName = user.getUsername().trim();
            if (userName.isEmpty()) {
                Api.error("UserName is empty. BAD REQUEST", user);
                return RestOutput.badRequest();
            }
            if (Setup.USER_NAME_PATTERN.matcher(userName).find() == false) {
                Api.error("UserName is not valid. BAD REQUEST", user);
                return RestOutput.badRequest();
            }

            // Check the validity of the password
            userPassword = user.getPassword().trim();
            if (userPassword.isEmpty()) {
                Api.error("UserPassword is empty. BAD REQUEST", user);
                return RestOutput.badRequest();
            }

            if (Setup.USER_PASSWORD_PATTERN.matcher(userPassword).find() == false) {
                Api.error("UserPassword is not strong enough. BAD REQUEST",
                          "At least 1 lower case alphabetical character",
                          "At least 1 upper case",
                          "At least 1 numeric",
                          "At least 1 special character",
                          "At least 8 characters",
                          user);
                return RestOutput.badRequest();
            }

            userHandler = new UserHandler(userName, userPassword);

            return RestOutput.ok(userHandler);

        } catch (Throwable t) {
            Api.error(t, "User failed. BAD REQUEST", user);
            return RestOutput.badRequest();
        }
    }
}

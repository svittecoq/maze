package maze;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;

import maze.base.Api;
import maze.base.RestOutput;
import maze.base.Result;
import maze.handler.core.CoreHandler;
import maze.http.HttpCode;
import maze.model.Maze;
import maze.model.MazeCreation;
import maze.model.MazeSolution;
import maze.model.SessionToken;
import maze.model.User;

public class RestServiceTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {

        RestOutput<Result> resultOutput;
        RestOutput<CoreHandler> coreHandlerOutput;
        CoreHandler coreHandler;

        String databaseId = UUID.randomUUID().toString();

        coreHandlerOutput = CoreHandler.with(Optional.empty(),
                                             Optional.empty(),
                                             Optional.empty(),
                                             Optional.of(databaseId),
                                             Optional.empty());
        if (RestOutput.isNOK(coreHandlerOutput)) {
            Api.error("CoreHandler creation is NOT OK", coreHandlerOutput);
            throw new RuntimeException("CoreHandler creation for RestServiceTest failed");
        }
        coreHandler = coreHandlerOutput.output();

        // Run the CoreHandler
        resultOutput = coreHandler.run();
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("CoreHandler run for RestServiceTest is NOT OK", resultOutput);
            throw new RuntimeException("CoreHandler run for RestServiceTest failed");
        }

        return coreHandler.restService();
    }

    private String generateValidUsername() {

        Random Random = new Random();

        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'

        return Random.ints(leftLimit, rightLimit + 1)
                     .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                     .limit(10)
                     .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                     .toString();
    }

    @Test
    public void createUserWithValidUserNameAndPassword() {

        User user;
        SessionToken sessionToken;

        user = new User(generateValidUsername(), "a2TT&d3mn");

        // Post the new user
        Response response = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                           .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response.getStatus());
        sessionToken = response.readEntity(SessionToken.class);
        assertTrue("Session Token should be returned", (sessionToken.getToken() != null));
    }

    @Test
    public void createUserWithInvalidPassword() {

        User user;

        // Password without special character
        user = new User(generateValidUsername(), "a2TTd3mn");

        // Post the new User
        Response response = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                           .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be BAD REQUEST", HttpCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void createValidUser_thenRecreateUserWithSameUserId() {

        String userId;
        User user;
        SessionToken sessionToken;

        userId = generateValidUsername();
        user = new User(userId, "aQQQ2TT&d3mn");

        // Post the new User
        Response response1 = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                            .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response1.getStatus());

        sessionToken = response1.readEntity(SessionToken.class);
        assertTrue("Session Token should be returned", (sessionToken.getToken() != null));

        // Re-create another User with same userId
        user = new User(userId, "aXQ233&TTd3mn");

        // Post again this new User
        Response response2 = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                            .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be BAD REQUEST", HttpCode.BAD_REQUEST_400, response2.getStatus());
    }

    @Test
    public void createValidUser_thenCreateValidMaze() {

        String userId;
        User user;
        SessionToken sessionToken;
        Maze maze;
        MazeCreation mazeCreation;

        userId = generateValidUsername();
        user = new User(userId, "aQQQ2TT&d3mn");

        // Post this User
        Response response1 = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                            .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response1.getStatus());

        sessionToken = response1.readEntity(SessionToken.class);
        assertTrue("Session Token should be returned", (sessionToken.getToken() != null));

        maze = new Maze(null, "A2", "6x4", new String[] { "A1", "E1", "C2", "C3", "F3", "A4", "B4", "C4", "D4", "F4" });

        // Post this Maze
        Response response2 = target("/maze").request(MediaType.APPLICATION_JSON_TYPE)
                                            .cookie(Setup.SESSION_TOKEN, sessionToken.toText())
                                            .post(Entity.entity(maze, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response2.getStatus());

        mazeCreation = response2.readEntity(MazeCreation.class);
        assertTrue("MazeCreation should include a maze Id", (mazeCreation.getMazeId() >= 1));
        assertNull("MazeCreation should not include any error", mazeCreation.getError());
    }

    @Test
    public void createValidUser_thenCreateValidMaze_thenSolveMin() {

        String userId;
        User user;
        SessionToken sessionToken;
        Maze maze;
        MazeCreation mazeCreation;
        MazeSolution mazeSolution;

        userId = generateValidUsername();
        user = new User(userId, "aQTT$d3mn");

        // Post this User
        Response response1 = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                            .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response1.getStatus());

        sessionToken = response1.readEntity(SessionToken.class);
        assertTrue("Session Token should be returned", (sessionToken.getToken() != null));

        maze = new Maze(null, "E1", "5x5", new String[] { "B2", "D2", "E2", "B3", "E4", "A5", "B5", "D5", "E5" });

        // Post this Maze
        Response response2 = target("/maze").request(MediaType.APPLICATION_JSON_TYPE)
                                            .cookie(Setup.SESSION_TOKEN, sessionToken.toText())
                                            .post(Entity.entity(maze, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response2.getStatus());

        mazeCreation = response2.readEntity(MazeCreation.class);
        assertTrue("MazeCreation should include a maze Id", (mazeCreation.getMazeId() >= 1));
        assertNull("MazeCreation should not include any error", mazeCreation.getError());

        // Get the Min Path
        Response response3 = target("/maze/" + mazeCreation.getMazeId()
                                    + "/solution").queryParam("steps", "min")
                                                  .request(MediaType.APPLICATION_JSON_TYPE)
                                                  .cookie(Setup.SESSION_TOKEN, sessionToken.toText())
                                                  .get();

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response3.getStatus());

        mazeSolution = response3.readEntity(MazeSolution.class);
        assertArrayEquals("MazeSolution should include this solution path",
                          mazeSolution.getPath(),
                          new String[] { "E1", "D1", "C1", "C2", "C3", "C4", "C5" });
        assertNull("MazeSolution should not include any error", mazeSolution.getError());
    }

    @Test
    public void createValidUser_thenCreateValidMaze_thenFailSolveMax() {

        String userId;
        User user;
        SessionToken sessionToken;
        Maze maze;
        MazeCreation mazeCreation;
        MazeSolution mazeSolution;

        userId = generateValidUsername();
        user = new User(userId, "aQTT$d3mn");

        // Post this User
        Response response1 = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                            .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response1.getStatus());

        sessionToken = response1.readEntity(SessionToken.class);
        assertTrue("Session Token should be returned", (sessionToken.getToken() != null));

        maze = new Maze(null,
                        "A2",
                        "8x8",
                        new String[] { "B2",
                                       "D2",
                                       "F2",
                                       "A3",
                                       "B3",
                                       "D3",
                                       "G3",
                                       "A4",
                                       "D4",
                                       "F4",
                                       "G4",
                                       "B5",
                                       "D5",
                                       "G5",
                                       "A6",
                                       "D6",
                                       "F6",
                                       "A8",
                                       "B8",
                                       "C8",
                                       "E8",
                                       "F8",
                                       "G8",
                                       "H8" });

        // Post this Maze
        Response response2 = target("/maze").request(MediaType.APPLICATION_JSON_TYPE)
                                            .cookie(Setup.SESSION_TOKEN, sessionToken.toText())
                                            .post(Entity.entity(maze, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response2.getStatus());

        mazeCreation = response2.readEntity(MazeCreation.class);
        assertTrue("MazeCreation should include a maze Id", (mazeCreation.getMazeId() >= 1));
        assertNull("MazeCreation should not include any error", mazeCreation.getError());

        // Get the Max Path
        Response response3 = target("/maze/" + mazeCreation.getMazeId()
                                    + "/solution").queryParam("steps", "max")
                                                  .request(MediaType.APPLICATION_JSON_TYPE)
                                                  .cookie(Setup.SESSION_TOKEN, sessionToken.toText())
                                                  .get();

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response3.getStatus());

        mazeSolution = response3.readEntity(MazeSolution.class);
        assertNull("MazeSolution should not include a solution path", mazeSolution.getPath());
        assertEquals("MazeSolution should contain an error on multiple paths ",
                     "Maze has multiple paths to exit for Max Path.",
                     mazeSolution.getError());
    }
}

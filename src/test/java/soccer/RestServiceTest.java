package soccer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import soccer.base.Api;
import soccer.base.RestOutput;
import soccer.base.Result;
import soccer.handler.core.CoreHandler;
import soccer.http.HttpCode;
import soccer.model.Player;
import soccer.model.Team;
import soccer.model.User;
import soccer.model.UserToken;
import soccer.model.outcome.TransferPlayerOutcome;
import soccer.model.outcome.UpdatePlayerOutcome;
import soccer.model.outcome.UpdateTeamOutcome;
import soccer.rest.RestService;

public class RestServiceTest extends JerseyTest {

    static RestService restService = null;

    @Override
    protected ResourceConfig configure() {

        URI databaseURI;
        RestOutput<Result> resultOutput;
        RestOutput<CoreHandler> coreHandlerOutput;
        CoreHandler coreHandler;

        // Use a test database with the defined prefix
        databaseURI = Api.URI(Setup.DEFAULT_STORE_URI + Setup.DATABASE_PREFIX + UUID.randomUUID().toString());

        coreHandlerOutput = CoreHandler.with(databaseURI, Optional.empty(), Optional.empty());
        if (RestOutput.isNOK(coreHandlerOutput)) {
            Api.error("CoreHandler creation is NOT OK", coreHandlerOutput, databaseURI);
            throw new RuntimeException("CoreHandler creation for RestServiceTest failed");
        }
        coreHandler = coreHandlerOutput.output();

        // Run the CoreHandler
        resultOutput = coreHandler.run();
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("CoreHandler run for RestServiceTest is NOT OK", resultOutput, databaseURI);
            throw new RuntimeException("CoreHandler run for RestServiceTest failed");
        }

        restService = coreHandler.restService();

        return restService;
    }

    @AfterEach
    private void terminate() {

        if (restService != null) {
            restService.terminate();
        }
    }

    private String generateValidUsername() {

        Random Random = new Random();

        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'

        return Random.ints(leftLimit, rightLimit + 1)
                     .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                     .limit(10)
                     .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                     .toString()
               + "@soccer.com";
    }

    @Test
    public void createUserWithValidUserNameAndPassword() {

        User user;
        UserToken userToken;

        user = new User(generateValidUsername(), "a2TT&d3mn");

        // Post the new user
        Response response = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                           .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response.getStatus());
        userToken = response.readEntity(UserToken.class);
        assertTrue("User Token should be returned", (userToken.getToken() != null));
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
        UserToken userToken;

        userId = generateValidUsername();
        user = new User(userId, "aQQQ2TT&d3mn");

        // Post the new User
        Response response1 = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                            .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response1.getStatus());

        userToken = response1.readEntity(UserToken.class);
        assertTrue("User Token should be returned", (userToken.getToken() != null));

        // Re-create another User with same userId
        user = new User(userId, "aXQ233&TTd3mn");

        // Post again this new User
        Response response2 = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                            .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be BAD REQUEST", HttpCode.BAD_REQUEST_400, response2.getStatus());
    }

    @Test
    public void createValidUser_thenUpdateTeam() {

        String userId;
        User user;
        UserToken userToken;
        Team team;
        Team updateTeam;
        UpdateTeamOutcome updateTeamOutcome;

        userId = generateValidUsername();
        user = new User(userId, "aQQQ2TT&d3mn");

        // Post this User
        Response response1 = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                            .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response1.getStatus());

        userToken = response1.readEntity(UserToken.class);
        assertTrue("User Token should be returned", (userToken.getToken() != null));

        // Get this Team
        Response response2 = target("/team").request(MediaType.APPLICATION_JSON_TYPE)
                                            .header(Setup.USER_TOKEN, userToken.toText())
                                            .get();

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response2.getStatus());

        team = response2.readEntity(Team.class);
        assertEquals("Team should have 20 players", team.getPlayerArray().length, 20);
        assertEquals("Team balance should be 5 million", team.getTeamBalance(), Long.valueOf(5000000));

        // Update the team
        updateTeam = new Team(team.getTeamId(), "newTeamName", "newCountry", null, null);
        Response response3 = target("/team/" + updateTeam.getTeamId()
                                                         .toText()).request(MediaType.APPLICATION_JSON_TYPE)
                                                                   .header(Setup.USER_TOKEN, userToken.toText())
                                                                   .put(Entity.entity(team,
                                                                                      MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response3.getStatus());

        updateTeamOutcome = response3.readEntity(UpdateTeamOutcome.class);

        assertNull("UpdateTeamOutcome should not include any error", updateTeamOutcome.getError());
    }

    @Test
    public void createValidUser_thenUpdatePlayer() {

        String userId;
        User user;
        UserToken userToken;
        Team team;
        Player player;
        Player updatePlayer;
        UpdatePlayerOutcome updatePlayerOutcome;

        userId = generateValidUsername();
        user = new User(userId, "aWWWQ2TT&d3mn");

        // Post this User
        Response response1 = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                            .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response1.getStatus());

        userToken = response1.readEntity(UserToken.class);
        assertTrue("User Token should be returned", (userToken.getToken() != null));

        // Get this Team
        Response response2 = target("/team").request(MediaType.APPLICATION_JSON_TYPE)
                                            .header(Setup.USER_TOKEN, userToken.toText())
                                            .get();

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response2.getStatus());

        team = response2.readEntity(Team.class);
        assertEquals("Team should have 20 players", team.getPlayerArray().length, 20);
        assertEquals("Team balance should be 5 million", team.getTeamBalance(), Long.valueOf(5000000));

        // Select a player
        int playerIndex = ThreadLocalRandom.current().nextInt(21);
        player = team.getPlayerArray()[playerIndex];

        // Update the player
        updatePlayer = new Player(player.getPlayerId(),
                                  player.getPlayerType(),
                                  "newPlayerFirstName",
                                  "newPlayerLastName",
                                  "newCountry",
                                  null,
                                  null,
                                  0L,
                                  player.getTeamId());
        Response response3 = target("/player/" + updatePlayer.getPlayerId()
                                                             .toText()).request(MediaType.APPLICATION_JSON_TYPE)
                                                                       .header(Setup.USER_TOKEN, userToken.toText())
                                                                       .put(Entity.entity(player,
                                                                                          MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response3.getStatus());

        updatePlayerOutcome = response3.readEntity(UpdatePlayerOutcome.class);

        assertNull("UpdatePlayerOutcome should not include any error", updatePlayerOutcome.getError());
    }

    @Test
    public void createTwoValidUsers_thenTransferOnePlayer() {

        String userId1;
        String userId2;
        User user1;
        User user2;
        UserToken userToken1;
        UserToken userToken2;
        Team team;
        Player player;
        Player updatePlayer;
        Player transferPlayer;
        UpdatePlayerOutcome updatePlayerOutcome;
        TransferPlayerOutcome transferPlayerOutcome;

        // Create user 1
        userId1 = generateValidUsername();
        user1 = new User(userId1, "aWWWQ2TT&d3mn");

        // Post this User
        Response response1 = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                            .post(Entity.entity(user1, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response1.getStatus());

        userToken1 = response1.readEntity(UserToken.class);
        assertTrue("User Token should be returned", (userToken1.getToken() != null));

        // Create user 2
        userId2 = generateValidUsername();
        user2 = new User(userId2, "aWWWQ2TT&d3mn");

        // Post this User
        Response response2 = target("/user").request(MediaType.APPLICATION_JSON_TYPE)
                                            .post(Entity.entity(user2, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response2.getStatus());

        userToken2 = response2.readEntity(UserToken.class);
        assertTrue("User Token should be returned", (userToken2.getToken() != null));

        // Get this Team
        Response response3 = target("/team").request(MediaType.APPLICATION_JSON_TYPE)
                                            .header(Setup.USER_TOKEN, userToken1.toText())
                                            .get();

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response3.getStatus());

        team = response3.readEntity(Team.class);

        // Select a player
        int playerIndex = ThreadLocalRandom.current().nextInt(21);
        player = team.getPlayerArray()[playerIndex];

        // Update the player to add it to the market
        updatePlayer = new Player(player.getPlayerId(),
                                  player.getPlayerType(),
                                  "newPlayerFirstName",
                                  "newPlayerLastName",
                                  "newCountry",
                                  null,
                                  null,
                                  2000000L,
                                  player.getTeamId());
        Response response4 = target("/player/" + updatePlayer.getPlayerId()
                                                             .toText()).request(MediaType.APPLICATION_JSON_TYPE)
                                                                       .header(Setup.USER_TOKEN, userToken1.toText())
                                                                       .put(Entity.entity(updatePlayer,
                                                                                          MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response4.getStatus());

        updatePlayerOutcome = response4.readEntity(UpdatePlayerOutcome.class);
        assertNull("UpdatePlayerOutcome should not include any error", updatePlayerOutcome.getError());

        transferPlayer = new Player(player.getPlayerId(), player.getTeamId());

        // Transfer this player to the other team
        Response response5 = target("/transfer").request(MediaType.APPLICATION_JSON_TYPE)
                                                .header(Setup.USER_TOKEN, userToken1.toText())
                                                .post(Entity.entity(transferPlayer, MediaType.APPLICATION_JSON_TYPE));

        assertEquals("Http Response should be 200-OK", HttpCode.OK_200, response5.getStatus());

        transferPlayerOutcome = response5.readEntity(TransferPlayerOutcome.class);
        assertNull("TransferPlayerOutcome should not include any error", transferPlayerOutcome.getError());
    }

}

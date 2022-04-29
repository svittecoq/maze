package soccer.rest;

import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.ResourceConfig;

import soccer.Setup;
import soccer.base.Api;
import soccer.base.RestOutput;
import soccer.handler.core.CoreHandler;
import soccer.http.HttpService;
import soccer.model.Player;
import soccer.model.PlayerId;
import soccer.model.Team;
import soccer.model.TeamId;
import soccer.model.User;
import soccer.model.UserToken;

@Path("/")
public class RestService extends ResourceConfig {

    private final CoreHandler _coreHandler;

    public RestService(CoreHandler coreHandler) {
        super();

        _coreHandler = coreHandler;

        packages(RestService.class.getPackageName());
    }

    private CoreHandler coreHandler() {

        return _coreHandler;
    }

    public String pathSpecification() {

        return "/*";
    }

    public void terminate() {

        coreHandler().terminate();
    }

    @POST
    @Path("/login")
    @Consumes(Setup.JSON_MEDIA_TYPE)
    @Produces(Setup.JSON_MEDIA_TYPE)
    public void signIn(@Context HttpServletRequest httpRequest,
                       User user,
                       @Suspended final AsyncResponse asyncResponse) {

        RestCall.run(asyncResponse, (cookieReference) -> {

            RestOutput<UserToken> userTokenOutput;
            UserToken userToken;

            // Login the User with its username and password
            userTokenOutput = coreHandler().loginUser(user);
            if (RestOutput.isNOK(userTokenOutput)) {
                Api.error("loginUser is NOT OK", userTokenOutput, user, this);
                return RestOutput.of(userTokenOutput);
            }
            userToken = userTokenOutput.output();

            return RestOutput.ok(userToken);
        });
    }

    @POST
    @Path("/user")
    @Consumes(Setup.JSON_MEDIA_TYPE)
    @Produces(Setup.JSON_MEDIA_TYPE)
    public void signUpUser(@Context HttpServletRequest httpRequest,
                           User user,
                           @Suspended final AsyncResponse asyncResponse) {

        RestCall.run(asyncResponse, (cookieReference) -> {

            RestOutput<UserToken> userTokenOutput;
            UserToken userToken;

            // Sign up the new user with its username and password
            userTokenOutput = coreHandler().signUpUser(user);
            if (RestOutput.isNOK(userTokenOutput)) {
                Api.error("signUpUser is NOT OK", userTokenOutput, user, this);
                return RestOutput.of(userTokenOutput);
            }
            userToken = userTokenOutput.output();

            return RestOutput.ok(userToken);
        });
    }

    @GET
    @Path("/team")
    @Produces(Setup.JSON_MEDIA_TYPE)
    public void getTeam(@Context HttpServletRequest httpRequest, @Suspended final AsyncResponse asyncResponse) {

        RestCall.run(asyncResponse, (cookieReference) -> {

            Optional<UserToken> userTokenOptional;
            UserToken userToken;

            // Search the UserToken from the Request
            userTokenOptional = HttpService.searchUserToken(httpRequest);
            if (userTokenOptional.isEmpty()) {
                Api.error("UserToken is not defined to getTeam. FORBIDDEN");
                return RestOutput.forbidden();
            }
            userToken = userTokenOptional.get();

            // Get the team for this user
            return coreHandler().retrieveTeam(userToken, Optional.empty());
        });
    }

    @GET
    @Path("/team/{teamId}")
    @Produces(Setup.JSON_MEDIA_TYPE)
    public void getTeam(@Context HttpServletRequest httpRequest,
                        @PathParam("teamId") String teamId,
                        @Suspended final AsyncResponse asyncResponse) {

        RestCall.run(asyncResponse, (cookieReference) -> {

            Optional<UserToken> userTokenOptional;
            UserToken userToken;

            // Search the UserToken from the Request
            userTokenOptional = HttpService.searchUserToken(httpRequest);
            if (userTokenOptional.isEmpty()) {
                Api.error("UserToken is not defined to getTeam. FORBIDDEN", teamId);
                return RestOutput.forbidden();
            }
            userToken = userTokenOptional.get();

            // Get the team for this user
            return coreHandler().retrieveTeam(userToken, Optional.of(new TeamId(UUID.fromString(teamId))));
        });
    }

    @PUT
    @Path("/team/{teamId}")
    @Consumes(Setup.JSON_MEDIA_TYPE)
    @Produces(Setup.JSON_MEDIA_TYPE)
    public void updateTeam(@Context HttpServletRequest httpRequest,
                           @PathParam("teamId") String teamId,
                           Team team,
                           @Suspended final AsyncResponse asyncResponse) {

        RestCall.run(asyncResponse, (cookieReference) -> {

            Optional<UserToken> userTokenOptional;
            UserToken userToken;

            // Search the UserToken from the Request
            userTokenOptional = HttpService.searchUserToken(httpRequest);
            if (userTokenOptional.isEmpty()) {
                Api.error("UserToken is not defined to updateTeam. FORBIDDEN", teamId, team);
                return RestOutput.forbidden();
            }
            userToken = userTokenOptional.get();

            // Update the team for this user
            return coreHandler().updateTeam(userToken, new TeamId(UUID.fromString(teamId)), team);
        });
    }

    @PUT
    @Path("/player/{playerId}")
    @Consumes(Setup.JSON_MEDIA_TYPE)
    @Produces(Setup.JSON_MEDIA_TYPE)
    public void updatePlayer(@Context HttpServletRequest httpRequest,
                             @PathParam("playerId") String playerId,
                             Player player,
                             @Suspended final AsyncResponse asyncResponse) {

        RestCall.run(asyncResponse, (cookieReference) -> {

            Optional<UserToken> userTokenOptional;
            UserToken userToken;

            // Search the UserToken from the Request
            userTokenOptional = HttpService.searchUserToken(httpRequest);
            if (userTokenOptional.isEmpty()) {
                Api.error("UserToken is not defined to updatePlayer. FORBIDDEN", playerId, player);
                return RestOutput.forbidden();
            }
            userToken = userTokenOptional.get();

            // Update the player for this user
            return coreHandler().updatePlayer(userToken, new PlayerId(UUID.fromString(playerId)), player);
        });
    }

    @GET
    @Path("/market")
    @Produces(Setup.JSON_MEDIA_TYPE)
    public void getMarket(@Context HttpServletRequest httpRequest, @Suspended final AsyncResponse asyncResponse) {

        RestCall.run(asyncResponse, (cookieReference) -> {

            Optional<UserToken> userTokenOptional;
            UserToken userToken;

            // Search the UserToken from the Request
            userTokenOptional = HttpService.searchUserToken(httpRequest);
            if (userTokenOptional.isEmpty()) {
                Api.error("UserToken is not defined to getMarket. FORBIDDEN");
                return RestOutput.forbidden();
            }
            userToken = userTokenOptional.get();

            // Get the market for this user
            return coreHandler().retrieveMarket(userToken);
        });
    }

    @POST
    @Path("/transfer")
    @Consumes(Setup.JSON_MEDIA_TYPE)
    @Produces(Setup.JSON_MEDIA_TYPE)
    public void transferPlayer(@Context HttpServletRequest httpRequest,
                               Player player,
                               @Suspended final AsyncResponse asyncResponse) {

        RestCall.run(asyncResponse, (cookieReference) -> {

            Optional<UserToken> userTokenOptional;
            UserToken userToken;

            // Search the UserToken from the Request
            userTokenOptional = HttpService.searchUserToken(httpRequest);
            if (userTokenOptional.isEmpty()) {
                Api.error("UserToken is not defined to transferPlayer. FORBIDDEN", player);
                return RestOutput.forbidden();
            }
            userToken = userTokenOptional.get();

            // Transfer the player into the team of this user
            return coreHandler().transferPlayer(userToken, player);
        });
    }

    @Override
    public String toString() {
        return "RestService [_coreHandler=" + _coreHandler + "]";
    }

}
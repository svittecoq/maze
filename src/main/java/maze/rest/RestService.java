package maze.rest;

import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;

import org.glassfish.jersey.server.ResourceConfig;

import maze.Setup;
import maze.base.Api;
import maze.base.RestOutput;
import maze.handler.core.CoreHandler;
import maze.http.HttpService;
import maze.model.Maze;
import maze.model.User;
import maze.model.UserToken;

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
    public void postUserLogin(@Context HttpServletRequest httpRequest,
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

            HttpService.assignUserToken(httpRequest, userToken);
            cookieReference.set(new Cookie(Setup.USER_TOKEN, userToken.toText()));

            return RestOutput.ok(userToken);
        });
    }

    @POST
    @Path("/user")
    @Consumes(Setup.JSON_MEDIA_TYPE)
    @Produces(Setup.JSON_MEDIA_TYPE)
    public void postUser(@Context HttpServletRequest httpRequest,
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

            HttpService.assignUserToken(httpRequest, userToken);
            cookieReference.set(new Cookie(Setup.USER_TOKEN, userToken.toText()));

            return RestOutput.ok(userToken);
        });
    }

    @POST
    @Path("/maze")
    @Consumes(Setup.JSON_MEDIA_TYPE)
    @Produces(Setup.JSON_MEDIA_TYPE)
    public void postMaze(@Context HttpServletRequest httpRequest,
                         Maze maze,
                         @Suspended final AsyncResponse asyncResponse) {

        RestCall.run(asyncResponse, (cookieReference) -> {

            Optional<UserToken> userTokenOptional;
            UserToken userToken;

            // Search the UserToken from the Request
            userTokenOptional = HttpService.searchUserToken(httpRequest);
            if (userTokenOptional.isEmpty()) {
                Api.error("UserToken is not defined to postMaze. FORBIDDEN", maze);
                return RestOutput.forbidden();
            }
            userToken = userTokenOptional.get();

            // Add the new maze for this user
            return coreHandler().addMaze(userToken, maze);
        });
    }

    @GET
    @Path("/maze/{mazeId}")
    @Produces(Setup.JSON_MEDIA_TYPE)
    public void getMaze(@Context HttpServletRequest httpRequest,
                        @PathParam("mazeId") Integer mazeId,
                        @Suspended final AsyncResponse asyncResponse) {

        RestCall.run(asyncResponse, (cookieReference) -> {

            Optional<UserToken> userTokenOptional;
            UserToken userToken;

            // Search the UserToken from the Request
            userTokenOptional = HttpService.searchUserToken(httpRequest);
            if (userTokenOptional.isEmpty()) {
                Api.error("UserToken is not defined to getMaze. FORBIDDEN", mazeId);
                return RestOutput.forbidden();
            }
            userToken = userTokenOptional.get();

            return coreHandler().retrieveMaze(userToken, mazeId);
        });
    }

    @GET
    @Path("/maze")
    @Produces(Setup.JSON_MEDIA_TYPE)
    public void getMazes(@Context HttpServletRequest httpRequest, @Suspended final AsyncResponse asyncResponse) {

        RestCall.run(asyncResponse, (cookieReference) -> {

            Optional<UserToken> userTokenOptional;
            UserToken userToken;

            // Search the UserToken from the Request
            userTokenOptional = HttpService.searchUserToken(httpRequest);
            if (userTokenOptional.isEmpty()) {
                Api.error("UserToken is not defined to getMazes. FORBIDDEN");
                return RestOutput.forbidden();
            }
            userToken = userTokenOptional.get();

            return coreHandler().retrieveMazes(userToken);
        });
    }

    @GET
    @Path("/maze/{mazeId}/solution")
    @Produces(Setup.JSON_MEDIA_TYPE)
    public void getMazeSolution(@Context HttpServletRequest httpRequest,
                                @PathParam("mazeId") Integer mazeId,
                                @QueryParam("steps") String steps,
                                @Suspended final AsyncResponse asyncResponse) {

        RestCall.run(asyncResponse, (cookieReference) -> {

            Optional<UserToken> userTokenOptional;
            UserToken userToken;

            // Search the UserToken from the Request
            userTokenOptional = HttpService.searchUserToken(httpRequest);
            if (userTokenOptional.isEmpty()) {
                Api.error("UserToken is not defined to getMazeSolution. FORBIDDEN");
                return RestOutput.forbidden();
            }
            userToken = userTokenOptional.get();

            if (Objects.equals("min", steps)) {
                // Return the Min Path for this Maze
                return coreHandler().solveMinPath(userToken, mazeId);
            }
            if (Objects.equals("max", steps)) {
                // Return the Max Path for this Maze
                return coreHandler().solveMaxPath(userToken, mazeId);
            }
            Api.error("Get Maze Solution requires steps parameter to be min or max. BAD REQUEST");
            return RestOutput.badRequest();
        });
    }

    @Override
    public String toString() {
        return "RestService [_coreHandler=" + _coreHandler + "]";
    }

}
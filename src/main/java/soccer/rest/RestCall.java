package soccer.rest;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.EofException;
import org.glassfish.jersey.server.internal.process.MappableException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import soccer.Setup;
import soccer.base.Api;
import soccer.base.RestCode;
import soccer.base.RestOutput;
import soccer.base.Result;

public class RestCall<T> implements ConnectionCallback, TimeoutHandler, CompletionCallback {

    private static final ExecutorService                           Executor     = Api.executorService("rest");

    private static final ObjectMapper                              JsonMapper   = objectMapper();
    private static final CacheControl                              CacheControl = buildNoCache();

    private final AsyncResponse                                    _asyncResponse;
    private final Function<AtomicReference<Cookie>, RestOutput<T>> _method;

    private RestCall(AsyncResponse asyncResponse, Function<AtomicReference<Cookie>, RestOutput<T>> method) {

        _asyncResponse = asyncResponse;

        _method = method;

        _asyncResponse.setTimeout(Setup.REST_CALL_TIME_OUT.toMillis(), TimeUnit.MILLISECONDS);
        _asyncResponse.setTimeoutHandler(this);
        _asyncResponse.register(this);
    }

    private AsyncResponse asyncResponse() {

        return _asyncResponse;
    }

    private Function<AtomicReference<Cookie>, RestOutput<T>> method() {

        return _method;
    }

    private RestOutput<Result> resume(RestCode restCode, Response response) {

        if (Api.isNull(restCode, response)) {
            return RestOutput.internalFailure();
        }

        try {
            // Resume the ASync Response now than we are done processing the request
            if (asyncResponse().resume(response) == false) {
                restCode = RestCode.INTERNAL_FAILURE;
                Api.error("AsyncResponse is not in the proper state. INTERNAL FAILURE", this);
            }

        } catch (MappableException t) {
            if ((t.getCause() != null) && (EofException.class.isInstance(t.getCause()))) {
                restCode = RestCode.NOT_AVAILABLE;
                Api.error("Resume of rest API could not complete with EOF detected. Maybe a restart of the HTTP server. NOT AVAILABLE",
                          this);
            } else if ((t.getCause() != null) && (TimeoutException.class.isInstance(t.getCause()))) {
                restCode = RestCode.TIMEOUT;
                Api.error("Resume of rest API could not complete with Timeout detected. Maybe a restart of the HTTP server. TIMEOUT",
                          this);
            } else {
                restCode = RestCode.INTERNAL_FAILURE;
                Api.error(t, "resume of rest API failed with MappableException. INTERNAL FAILURE", this);
            }
        } catch (Throwable t) {
            restCode = RestCode.INTERNAL_FAILURE;
            Api.error(t, "resume of rest API failed. INTERNAL FAILURE", this);
        }

        switch (restCode) {
        case OK:
            return RestOutput.OK;
        case BAD_REQUEST:
            return RestOutput.badRequest();
        case FORBIDDEN:
            return RestOutput.forbidden();
        case NOT_FOUND:
            return RestOutput.notFound();
        case TIMEOUT:
            return RestOutput.timeout();
        case INTERNAL_FAILURE:
            return RestOutput.internalFailure();
        case NOT_AVAILABLE:
            return RestOutput.notAvailable();
        default:
            Api.error("Invalid Rest Code. INTERNAL FAILURE", restCode, this);
            return RestOutput.internalFailure();
        }
    }

    @Override
    public void onDisconnect(AsyncResponse disconnected) {

        // Force the Rest Code as connection get lost while sending the response
        Api.error("Disconnection detected. NOT AVAILABLE", this);
        resume(RestCode.NOT_AVAILABLE, notAvailableResponse());
    }

    @Override
    public void handleTimeout(AsyncResponse asyncResponse) {

        // Force the Rest Code as timeout occurred while preparing the response
        Api.error("Timeout detected. TIMEOUT", this);
        resume(RestCode.TIMEOUT, timeoutResponse());
    }

    @Override
    public void onComplete(Throwable throwable) {

        // Will be called upon completion

        if (throwable != null) {
            // Force the Rest Code as Internal Error
            Api.error(throwable, "RestCall onComplete failed", this);
        }
    }

    protected void execute() {

        AtomicReference<Cookie> cookieReference;
        RestOutput<T> restOutput;
        T output;

        try {

            cookieReference = new AtomicReference<Cookie>(null);

            // Process the rest method
            restOutput = method().apply(cookieReference);

            if (RestOutput.isNOK(restOutput)) {
                Api.error("RestCall is NOT OK", restOutput, this);
                switch (restOutput.restCode()) {
                case BAD_REQUEST:
                    resume(restOutput.restCode(), badRequestResponse());
                    break;
                case FORBIDDEN:
                    resume(restOutput.restCode(), forbiddenResponse());
                    break;
                case NOT_FOUND:
                    resume(restOutput.restCode(), notFoundResponse());
                    break;
                case TIMEOUT:
                    resume(restOutput.restCode(), timeoutResponse());
                    break;
                case INTERNAL_FAILURE:
                    resume(restOutput.restCode(), internalFailureResponse());
                    break;
                case NOT_AVAILABLE:
                    resume(restOutput.restCode(), notAvailableResponse());
                    break;
                default:
                    Api.error("Invalid Rest Code. INTERNAL FAILURE", restOutput, this);
                    resume(restOutput.restCode(), internalFailureResponse());
                    break;
                }
                return;
            }

            output = restOutput.output();

            if (Result.class.isInstance(output)) {
                // Resume without a JSON payload
                resume(RestCode.OK, okResponse(Optional.ofNullable(cookieReference.get())));
                return;
            }

            // Resume with a JSON payload
            resume(RestCode.OK, okResponse(Optional.ofNullable(cookieReference.get()), output));

        } catch (Throwable t) {
            Api.error(t, "RestCall execute failed. INTERNAL FAILURE", this);
            resume(RestCode.INTERNAL_FAILURE, internalFailureResponse());
        }
    }

    protected void run() {

        CompletableFuture.runAsync(this::execute, Executor);
    }

    @Override
    public String toString() {
        return "RestCall [_asyncResponse=" + _asyncResponse + ", _method=" + _method + "]";
    }

    private static CacheControl buildNoCache() {

        CacheControl cacheControl = new CacheControl();

        cacheControl.setNoCache(true);
        return cacheControl;
    }

    private static ObjectMapper objectMapper() {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getTypeFactory().clearCache();

        objectMapper.setSerializationInclusion(Include.NON_NULL);

        return objectMapper;
    }

    private static Response buildRestErrorResponse(int statusCode) {

        StringBuilder stringBuilder;

        stringBuilder = new StringBuilder();
        stringBuilder.append("Error   : ").append(HttpStatus.getMessage(statusCode)).append(System.lineSeparator());

        return Response.status(statusCode)
                       .cacheControl(CacheControl)
                       .entity(stringBuilder.toString())
                       .type(Setup.TEXT_MEDIA_TYPE)
                       .build();
    }

    private static Response okResponse(Optional<Cookie> cookieOptional) {

        Cookie cookie;
        NewCookie newCookie;

        if (cookieOptional.isEmpty()) {
            // No cookie to set in response
            return Response.status(HttpStatus.OK_200).cacheControl(CacheControl).build();
        }

        cookie = cookieOptional.get();
        newCookie = new NewCookie(cookie);

        return Response.status(HttpStatus.OK_200).cacheControl(CacheControl).cookie(newCookie).build();
    }

    private static <T> Response okResponse(Optional<Cookie> cookieOptional, T output) throws Exception {

        Cookie cookie;
        NewCookie newCookie;

        // Serialize the output of this JSON payload
        byte[] jsonPayload = JsonMapper.writeValueAsBytes(output);

        if (cookieOptional.isEmpty()) {
            // No cookie to set in response

            return Response.status(HttpStatus.OK_200)
                           .cacheControl(CacheControl)
                           .entity(jsonPayload)
                           .header(Setup.CONTENT_LENGTH_ATTRIBUTE, Integer.valueOf(jsonPayload.length).toString())
                           .build();
        }

        cookie = cookieOptional.get();
        newCookie = new NewCookie(cookie);

        return Response.status(HttpStatus.OK_200)
                       .cacheControl(CacheControl)
                       .entity(jsonPayload)
                       .header(Setup.CONTENT_LENGTH_ATTRIBUTE, Integer.valueOf(jsonPayload.length).toString())
                       .cookie(newCookie)
                       .build();
    }

    private static Response badRequestResponse() {

        return buildRestErrorResponse(HttpStatus.BAD_REQUEST_400);
    }

    private static Response forbiddenResponse() {

        return buildRestErrorResponse(HttpStatus.FORBIDDEN_403);
    }

    private static Response notFoundResponse() {

        return buildRestErrorResponse(HttpStatus.NOT_FOUND_404);
    }

    private static Response timeoutResponse() {

        return buildRestErrorResponse(HttpStatus.REQUEST_TIMEOUT_408);
    }

    private static Response internalFailureResponse() {

        return buildRestErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }

    private static Response notAvailableResponse() {

        return buildRestErrorResponse(HttpStatus.SERVICE_UNAVAILABLE_503);
    }

    public static <T> void run(AsyncResponse asyncResponse, Function<AtomicReference<Cookie>, RestOutput<T>> method) {

        RestCall<T> restCall;

        if (Api.isNull(asyncResponse, method)) {
            return;
        }

        restCall = new RestCall<T>(asyncResponse, method);

        restCall.run();

    }
}

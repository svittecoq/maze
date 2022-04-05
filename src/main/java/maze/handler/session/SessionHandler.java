package maze.handler.session;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import maze.Setup;
import maze.base.Api;
import maze.base.RestOutput;
import maze.handler.user.UserHandler;
import maze.model.SessionToken;

public class SessionHandler {

    private final SessionToken             _sessionToken;
    private final UserHandler              _userHandler;
    private final AtomicReference<Instant> _instantReference;

    private SessionHandler(SessionToken sessionToken, UserHandler userHandler) {

        _sessionToken = sessionToken;
        _userHandler = userHandler;
        _instantReference = new AtomicReference<Instant>(Instant.now());
    }

    public SessionToken sessionToken() {

        return _sessionToken;
    }

    public UserHandler userHandler() {

        return _userHandler;
    }

    public Instant instant() {

        return _instantReference.get();
    }

    public void refresh() {

        _instantReference.set(Instant.now());
    }

    public boolean hasTimedOut() {

        // Session has not been refreshed recently
        return instant().isBefore(Instant.now().minus(Setup.SESSION_TIME_OUT));
    }

    @Override
    public String toString() {
        return "SessionHandler [_sessionToken=" + _sessionToken
               + ", _userHandler="
               + _userHandler
               + ", _instantReference="
               + _instantReference
               + "]";
    }

    public static RestOutput<SessionHandler> with(SessionToken sessionToken, UserHandler userHandler) {

        SessionHandler sessionHandler;

        if (Api.isNull(sessionToken, userHandler)) {
            return RestOutput.badRequest();
        }

        sessionHandler = new SessionHandler(sessionToken, userHandler);

        return RestOutput.ok(sessionHandler);
    }
}

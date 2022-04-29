package soccer.handler.session;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import soccer.Setup;
import soccer.base.Api;
import soccer.base.RestOutput;
import soccer.handler.user.UserHandler;
import soccer.model.UserToken;

public class SessionHandler {

    private final UserToken                _userToken;
    private final UserHandler              _userHandler;
    private final AtomicReference<Instant> _instantReference;

    private SessionHandler(UserToken userToken, UserHandler userHandler) {

        _userToken = userToken;
        _userHandler = userHandler;
        _instantReference = new AtomicReference<Instant>(Instant.now());
    }

    public UserToken userToken() {

        return _userToken;
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
        return "SessionHandler [_userToken=" + _userToken
               + ", _userHandler="
               + _userHandler
               + ", _instantReference="
               + _instantReference
               + "]";
    }

    public static RestOutput<SessionHandler> with(UserToken userToken, UserHandler userHandler) {

        SessionHandler sessionHandler;

        if (Api.isNull(userToken, userHandler)) {
            return RestOutput.badRequest();
        }

        sessionHandler = new SessionHandler(userToken, userHandler);

        return RestOutput.ok(sessionHandler);
    }
}

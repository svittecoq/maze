package maze.model;

import java.util.Objects;
import java.util.UUID;

public class SessionToken {

    private UUID _token;

    public SessionToken() {
        this(null);
    }

    public SessionToken(UUID token) {

        setToken(token);
    }

    public UUID getToken() {
        return _token;
    }

    public void setToken(UUID token) {
        _token = token;
    }

    @Override
    public int hashCode() {

        return Objects.hashCode(getToken());
    }

    @Override
    public boolean equals(Object object) {

        if (this == object)
            return true;
        if (!(object instanceof SessionToken))
            return false;
        SessionToken that = (SessionToken) object;

        return Objects.equals(this.getToken(), that.getToken());
    }

    public String toText() {

        UUID token = getToken();
        if (token == null) {
            return "NO TOKEN";
        }

        return token.toString();
    }

    @Override
    public String toString() {
        return toText();
    }

    public static SessionToken with(String text) {

        return new SessionToken(UUID.fromString(text));
    }

    public static SessionToken random() {

        return new SessionToken(UUID.randomUUID());
    }
}
package maze.model;

import java.util.Objects;
import java.util.UUID;

public class UserToken {

    private UUID _token;

    public UserToken() {
        this(null);
    }

    public UserToken(UUID token) {

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
        if (!(object instanceof UserToken))
            return false;
        UserToken that = (UserToken) object;

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

    public static UserToken with(String text) {

        return new UserToken(UUID.fromString(text));
    }

    public static UserToken random() {

        return new UserToken(UUID.randomUUID());
    }
}
package soccer.model;

import java.util.Objects;

public class UserToken {

    private String _token;

    public UserToken() {
        this(null);
    }

    public UserToken(String token) {

        setToken(token);
    }

    public String getToken() {
        return _token;
    }

    public void setToken(String token) {
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

        String token = getToken();
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

        return new UserToken(text);
    }
}
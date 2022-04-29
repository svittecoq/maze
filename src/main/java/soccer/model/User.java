package soccer.model;

public class User {

    private String _username;
    private String _password;

    public User() {
        this(null, null);
    }

    public User(String username, String password) {

        setUsername(username);
        setPassword(password);
    }

    public String getUsername() {
        return _username;
    }

    public void setUsername(String username) {
        _username = username;
    }

    public String getPassword() {
        return _password;
    }

    public void setPassword(String password) {
        _password = password;
    }

    @Override
    public String toString() {
        return "User [_username=" + _username + ", _password=" + _password + "]";
    }

}

package soccer.store.user;

public class UserRecord {

    private final String _userId;
    private final String _userPassword;

    public UserRecord(String userId, String userPassword) {

        _userId = userId;
        _userPassword = userPassword;
    }

    public String userId() {
        return _userId;
    }

    public String userPassword() {
        return _userPassword;
    }

    @Override
    public String toString() {
        return "UserRecord [_userId=" + _userId + ", _userPassword=" + _userPassword + "]";
    }
}

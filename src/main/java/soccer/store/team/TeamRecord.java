package soccer.store.team;

import soccer.model.TeamId;

public class TeamRecord {

    private final String _userId;
    private final TeamId _teamId;
    private final String _name;
    private final String _country;
    private final Long   _balance;

    public TeamRecord(String userId, TeamId teamId, String name, String country, Long balance) {

        _userId = userId;
        _teamId = teamId;
        _name = name;
        _country = country;
        _balance = balance;
    }

    public String userId() {
        return _userId;
    }

    public TeamId teamId() {
        return _teamId;
    }

    public String name() {
        return _name;
    }

    public String country() {
        return _country;
    }

    public Long balance() {
        return _balance;
    }

    @Override
    public String toString() {
        return "TeamRecord [_userId=" + _userId
               + ", _teamId="
               + _teamId
               + ", _name="
               + _name
               + ", _country="
               + _country
               + ", _balance="
               + _balance
               + "]";
    }
}

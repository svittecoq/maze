package soccer.model;

public class Team {

    private TeamId   _teamId;

    private String   _teamName;
    private String   _teamCountry;
    private Long     _teamBalance;

    private Player[] _playerArray;

    public Team() {
    }

    public Team(TeamId teamId, String teamName, String teamCountry, Long teamBalance, Player[] playerArray) {

        setTeamId(teamId);
        setTeamName(teamName);
        setTeamCountry(teamCountry);
        setTeamBalance(teamBalance);

        setPlayerArray(playerArray);
    }

    public TeamId getTeamId() {
        return _teamId;
    }

    public void setTeamId(TeamId teamId) {
        _teamId = teamId;
    }

    public String getTeamName() {
        return _teamName;
    }

    public void setTeamName(String teamName) {
        _teamName = teamName;
    }

    public String getTeamCountry() {
        return _teamCountry;
    }

    public void setTeamCountry(String teamCountry) {
        _teamCountry = teamCountry;
    }

    public Long getTeamBalance() {
        return _teamBalance;
    }

    public void setTeamBalance(Long teamBalance) {
        _teamBalance = teamBalance;
    }

    public Player[] getPlayerArray() {
        return _playerArray;
    }

    public void setPlayerArray(Player[] playerArray) {
        _playerArray = playerArray;
    }

    @Override
    public String toString() {
        return "Team [_teamId=" + _teamId
               + ", _teamName="
               + _teamName
               + ", _teamCountry="
               + _teamCountry
               + ", _teamBalance="
               + _teamBalance
               + ", _playerArray="
               + _playerArray
               + "]";
    }

}

package soccer.model;

public class Market {

    private Team[] _teamArray;

    public Market() {
    }

    public Market(Team[] teamArray) {

        setTeamArray(teamArray);
    }

    public Team[] getTeamArray() {
        return _teamArray;
    }

    public void setTeamArray(Team[] teamArray) {
        _teamArray = teamArray;
    }

    @Override
    public String toString() {
        return "Market [_teamArray=" + _teamArray + "]";
    }

}

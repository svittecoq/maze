package soccer.model.outcome;

import soccer.model.TeamId;

public class CreateTeamOutcome {

    private TeamId _teamId;
    private String _error;

    public CreateTeamOutcome() {

        setTeamId(null);
        setError(null);
    }

    public CreateTeamOutcome(TeamId teamId) {

        // Team properly created
        setTeamId(teamId);
        setError(null);
    }

    public CreateTeamOutcome(TeamId teamId, String error) {

        // Team could not be properly created
        setTeamId(teamId);
        setError(error);
    }

    public TeamId getTeamId() {
        return _teamId;
    }

    public void setTeamId(TeamId teamId) {
        _teamId = teamId;
    }

    public String getError() {
        return _error;
    }

    public void setError(String error) {
        _error = error;
    }

    @Override
    public String toString() {
        return "CreateTeamOutcome [_teamId=" + _teamId + ", _error=" + _error + "]";
    }

}

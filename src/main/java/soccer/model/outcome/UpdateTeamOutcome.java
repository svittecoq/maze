package soccer.model.outcome;

import soccer.model.TeamId;

public class UpdateTeamOutcome {

    private TeamId _teamId;
    private String _error;

    public UpdateTeamOutcome() {
    }

    public UpdateTeamOutcome(TeamId teamId) {

        // Team properly updated
        setTeamId(teamId);
        setError(null);
    }

    public UpdateTeamOutcome(TeamId teamId, String error) {

        // Team not properly updated
        setTeamId(null);
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
        return "UpdateTeamOutcome [_teamId=" + _teamId + ", _error=" + _error + "]";
    }

}

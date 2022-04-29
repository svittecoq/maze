package soccer.model.outcome;

import soccer.model.PlayerId;
import soccer.model.TeamId;

public class TransferPlayerOutcome {

    private TeamId   _teamId;
    private PlayerId _playerId;
    private String   _error;

    public TransferPlayerOutcome() {
    }

    public TransferPlayerOutcome(TeamId teamId, PlayerId playerId) {

        // Transfer successful
        setTeamId(teamId);
        setPlayerId(playerId);
        setError(null);
    }

    public TransferPlayerOutcome(TeamId teamId, PlayerId playerId, String error) {

        // Transfer failed
        setTeamId(teamId);
        setPlayerId(playerId);
        setError(error);
    }

    public TeamId getTeamId() {
        return _teamId;
    }

    public void setTeamId(TeamId teamId) {
        _teamId = teamId;
    }

    public PlayerId getPlayerId() {
        return _playerId;
    }

    public void setPlayerId(PlayerId playerId) {
        _playerId = playerId;
    }

    public String getError() {
        return _error;
    }

    public void setError(String error) {
        _error = error;
    }

    @Override
    public String toString() {
        return "TransferPlayerOutcome [_teamId=" + _teamId + ", _playerId=" + _playerId + ", _error=" + _error + "]";
    }

}

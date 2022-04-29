package soccer.model.outcome;

import soccer.model.PlayerId;

public class UpdatePlayerOutcome {

    private PlayerId _playerId;
    private String   _error;

    public UpdatePlayerOutcome() {
    }

    public UpdatePlayerOutcome(PlayerId playerId) {

        // Player properly updated
        setPlayerId(playerId);
        setError(null);
    }

    public UpdatePlayerOutcome(PlayerId playerId, String error) {

        // Player not properly updated
        setPlayerId(null);
        setError(error);
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
        return "UpdatePlayerOutcome [_playerId=" + _playerId + ", _error=" + _error + "]";
    }

}

package soccer.store.player;

import soccer.model.PlayerId;
import soccer.model.PlayerType;
import soccer.model.TeamId;

public class PlayerRecord {

    private final PlayerId   _playerId;

    private final PlayerType _type;
    private final String     _firstName;
    private final String     _lastName;
    private final String     _country;
    private final Integer    _age;

    private final Long       _assetValue;
    private final Long       _transferValue;

    private final TeamId     _teamId;

    public PlayerRecord(PlayerId playerId,
                        PlayerType type,
                        String firstName,
                        String lastName,
                        String country,
                        Integer age,
                        Long assetValue,
                        Long transferValue,
                        TeamId teamId) {

        _playerId = playerId;
        _type = type;
        _firstName = firstName;
        _lastName = lastName;
        _country = country;
        _age = age;

        _assetValue = assetValue;
        _transferValue = transferValue;

        _teamId = teamId;
    }

    public PlayerId playerId() {
        return _playerId;
    }

    public PlayerType playerType() {
        return _type;
    }

    public String firstName() {
        return _firstName;
    }

    public String lastName() {
        return _lastName;
    }

    public String country() {
        return _country;
    }

    public Integer age() {
        return _age;
    }

    public Long assetValue() {
        return _assetValue;
    }

    public Long transferValue() {
        return _transferValue;
    }

    public TeamId teamId() {
        return _teamId;
    }

    @Override
    public String toString() {
        return "PlayerRecord [_playerId=" + _playerId
               + ", _type="
               + _type
               + ", _firstName="
               + _firstName
               + ", _lastName="
               + _lastName
               + ", _country="
               + _country
               + ", _age="
               + _age
               + ", _assetValue="
               + _assetValue
               + ", _transferValue="
               + _transferValue
               + ", _teamId="
               + _teamId
               + "]";
    }

}

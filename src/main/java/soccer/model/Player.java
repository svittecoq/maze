package soccer.model;

public class Player {

    private PlayerId   _playerId;

    private PlayerType _playerType;
    private String     _playerFirstName;
    private String     _playerLastName;
    private String     _playerCountry;
    private Integer    _playerAge;

    private Long       _playerAssetValue;
    private Long       _playerTransferValue;

    private TeamId     _teamId;

    public Player() {
    }

    public Player(PlayerId playerId, TeamId teamId) {
        this(playerId, null, null, null, null, null, null, null, teamId);
    }

    public Player(PlayerId playerId,
                  PlayerType playerType,
                  String playerFirstName,
                  String playerLastName,
                  String playerCountry,
                  Integer playerAge,
                  Long playerAssetValue,
                  Long playerTransferValue,
                  TeamId teamId) {

        setPlayerId(playerId);
        setPlayerType(playerType);
        setPlayerFirstName(playerFirstName);
        setPlayerLastName(playerLastName);
        setPlayerCountry(playerCountry);
        setPlayerAge(playerAge);
        setPlayerAssetValue(playerAssetValue);
        setPlayerTransferValue(playerTransferValue);
        setTeamId(teamId);
    }

    public PlayerId getPlayerId() {
        return _playerId;
    }

    public void setPlayerId(PlayerId playerId) {
        _playerId = playerId;
    }

    public PlayerType getPlayerType() {
        return _playerType;
    }

    public void setPlayerType(PlayerType playerType) {
        _playerType = playerType;
    }

    public String getPlayerFirstName() {
        return _playerFirstName;
    }

    public void setPlayerFirstName(String playerFirstName) {
        _playerFirstName = playerFirstName;
    }

    public String getPlayerLastName() {
        return _playerLastName;
    }

    public void setPlayerLastName(String playerLastName) {
        _playerLastName = playerLastName;
    }

    public String getPlayerCountry() {
        return _playerCountry;
    }

    public void setPlayerCountry(String playerCountry) {
        _playerCountry = playerCountry;
    }

    public Integer getPlayerAge() {
        return _playerAge;
    }

    public void setPlayerAge(Integer playerAge) {
        _playerAge = playerAge;
    }

    public Long getPlayerAssetValue() {
        return _playerAssetValue;
    }

    public void setPlayerAssetValue(Long playerAssetValue) {
        _playerAssetValue = playerAssetValue;
    }

    public Long getPlayerTransferValue() {
        return _playerTransferValue;
    }

    public void setPlayerTransferValue(Long playerTransferValue) {
        _playerTransferValue = playerTransferValue;
    }

    public TeamId getTeamId() {
        return _teamId;
    }

    public void setTeamId(TeamId teamId) {
        _teamId = teamId;
    }

    @Override
    public String toString() {
        return "Player [_playerId=" + _playerId
               + ", _playerType="
               + _playerType
               + ", _playerFirstName="
               + _playerFirstName
               + ", _playerLastName="
               + _playerLastName
               + ", _playerCountry="
               + _playerCountry
               + ", _playerAge="
               + _playerAge
               + ", _playerAssetValue="
               + _playerAssetValue
               + ", _playerTransferValue="
               + _playerTransferValue
               + ", _teamId="
               + _teamId
               + "]";
    }
}

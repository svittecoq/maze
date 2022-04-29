package maze.store.maze;

public class MazeRecord {

    private final String   _userId;
    private final Integer  _mazeId;
    private final String   _entrance;
    private final String   _gridSize;
    private final String[] _walls;

    public MazeRecord(String userId, Integer mazeId, String entrance, String gridSize, String[] walls) {

        _userId = userId;
        _mazeId = mazeId;
        _entrance = entrance;
        _gridSize = gridSize;
        _walls = walls;
    }

    public String userId() {
        return _userId;
    }

    public Integer mazeId() {
        return _mazeId;
    }

    public String entrance() {
        return _entrance;
    }

    public String gridSize() {
        return _gridSize;
    }

    public String[] walls() {
        return _walls;
    }

    public String wallsText() {

        return String.join(",", walls());
    }

    @Override
    public String toString() {
        return "MazeRecord [_userId=" + _userId
               + ", _mazeId="
               + _mazeId
               + ", _entrance="
               + _entrance
               + ", _gridSize="
               + _gridSize
               + ", _walls="
               + _walls
               + "]";
    }
}

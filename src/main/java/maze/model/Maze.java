package maze.model;

public class Maze {

    private Integer  _mazeId;

    private String   _entrance;
    private String   _gridSize;
    private String[] _walls;

    public Maze() {
        this(null, null, null, null);
    }

    public Maze(Integer mazeId, String entrance, String gridSize, String[] walls) {

        setMazeId(mazeId);
        setEntrance(entrance);
        setGridSize(gridSize);
        setWalls(walls);
    }

    public Integer getMazeId() {
        return _mazeId;
    }

    public void setMazeId(Integer mazeId) {
        _mazeId = mazeId;
    }

    public String getEntrance() {
        return _entrance;
    }

    public void setEntrance(String entrance) {
        _entrance = entrance;
    }

    public String getGridSize() {
        return _gridSize;
    }

    public void setGridSize(String gridSize) {
        _gridSize = gridSize;
    }

    public String[] getWalls() {
        return _walls;
    }

    public void setWalls(String[] walls) {
        _walls = walls;
    }

    @Override
    public String toString() {
        return "Maze [_mazeId=" + _mazeId
               + ", _entrance="
               + _entrance
               + ", _gridSize="
               + _gridSize
               + ", _walls="
               + _walls
               + "]";
    }
}

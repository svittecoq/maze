package maze.model;

public class MazeCreation {

    private Integer _mazeId;
    private String  _error;

    public MazeCreation() {

        setMazeId(null);
        setError(null);
    }

    public MazeCreation(Integer mazeId) {

        // Maze properly created
        setMazeId(mazeId);
        setError(null);
    }

    public MazeCreation(String error) {

        // Maze could not be properly created
        setMazeId(null);
        setError(error);
    }

    public Integer getMazeId() {
        return _mazeId;
    }

    public void setMazeId(Integer mazeId) {
        _mazeId = mazeId;
    }

    public String getError() {
        return _error;
    }

    public void setError(String error) {
        _error = error;
    }

    @Override
    public String toString() {
        return "MazeCreation [_mazeId=" + _mazeId + ", _error=" + _error + "]";
    }

}

package maze.model;

public class MazeSolution {

    private String[] _path;
    private String   _error;

    public MazeSolution() {

        setPath(null);
        setError(null);
    }

    public MazeSolution(String[] path) {

        // Maze with a proper solution
        setPath(path);
        setError(null);
    }

    public MazeSolution(String error) {

        // Maze without a proper solution
        setPath(null);
        setError(error);
    }

    public String[] getPath() {
        return _path;
    }

    public void setPath(String[] path) {
        _path = path;
    }

    public String getError() {
        return _error;
    }

    public void setError(String error) {
        _error = error;
    }

    @Override
    public String toString() {
        return "MazeSolution [_path=" + _path + ", _error=" + _error + "]";
    }

}

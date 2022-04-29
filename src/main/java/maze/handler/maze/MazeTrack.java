package maze.handler.maze;

public class MazeTrack {

    private final MazePoint _mazePoint;

    private boolean         _up;
    private boolean         _down;
    private boolean         _left;
    private boolean         _right;

    public MazeTrack(MazePoint mazePoint) {

        _mazePoint = mazePoint;

        _up = true;
        _down = true;
        _left = true;
        _right = true;
    }

    protected MazePoint mazePoint() {

        return _mazePoint;
    }

    protected boolean trackUp() {

        if (_up == false) {
            return false;
        }
        _up = false;
        return true;
    }

    protected boolean trackDown() {

        if (_down == false) {
            return false;
        }
        _down = false;
        return true;
    }

    protected boolean trackLeft() {

        if (_left == false) {
            return false;
        }
        _left = false;
        return true;
    }

    protected boolean trackRight() {

        if (_right == false) {
            return false;
        }
        _right = false;
        return true;
    }

    protected void endForward() {

        _up = false;
        _down = false;
        _left = false;
        _right = false;
    }

    @Override
    public String toString() {
        return "MazeTrack [_mazePoint=" + _mazePoint
               + ", _up="
               + _up
               + ", _down="
               + _down
               + ", _left="
               + _left
               + ", _right="
               + _right
               + "]";
    }
}

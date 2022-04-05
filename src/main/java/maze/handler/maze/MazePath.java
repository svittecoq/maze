package maze.handler.maze;

import java.util.Arrays;
import java.util.stream.Stream;

public class MazePath {

    public static final MazePath NO_PATH        = new MazePath(new MazePoint[0]);
    public static final MazePath MULTIPLE_PATHS = new MazePath(new MazePoint[0]);

    private final MazePoint[]    _mazePointArray;

    public MazePath(MazePoint[] mazePointArray) {

        _mazePointArray = mazePointArray;
    }

    private MazePoint[] mazePointArray() {

        return _mazePointArray;
    }

    protected int length() {

        return mazePointArray().length;
    }

    protected boolean isNoPath() {

        return (this == NO_PATH);
    }

    protected boolean isMultiplePaths() {

        return (this == MULTIPLE_PATHS);
    }

    @Override
    public int hashCode() {

        return Arrays.hashCode(mazePointArray());
    }

    @Override
    public boolean equals(Object object) {

        if (this == object)
            return true;
        if (!(object instanceof MazePath))
            return false;
        MazePath that = (MazePath) object;

        return (Arrays.equals(this.mazePointArray(), that.mazePointArray()));
    }

    protected String[] toTextArray() {

        return Stream.of(mazePointArray()).map(MazePoint::toText).toArray(String[]::new);
    }

    protected String toText() {

        return String.join(",", toTextArray());
    }

    @Override
    public String toString() {
        return toText();
    }
}

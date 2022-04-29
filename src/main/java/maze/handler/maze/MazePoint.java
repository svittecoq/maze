package maze.handler.maze;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import maze.Setup;
import maze.base.Api;

public class MazePoint {

    private static final Map<String, Integer> ColMap = IntStream.range(0, Setup.MAZE_COLUMNS.length)
                                                                .boxed()
                                                                .collect(Collectors.toMap(i -> Setup.MAZE_COLUMNS[i],
                                                                                          i -> i + 1));

    private final int                         _col;
    private final int                         _row;

    public MazePoint(int col, int row) {

        _col = col;
        _row = row;
    }

    protected int col() {

        return _col;
    }

    protected int row() {

        return _row;
    }

    @Override
    public int hashCode() {

        return Objects.hash(col(), row());
    }

    @Override
    public boolean equals(Object object) {

        if (this == object)
            return true;
        if (!(object instanceof MazePoint))
            return false;
        MazePoint that = (MazePoint) object;

        return ((this.col() == that.col()) && (this.row() == that.row()));
    }

    protected String toText() {

        return Setup.MAZE_COLUMNS[col() - 1] + row();
    }

    @Override
    public String toString() {
        return toText();
    }

    public static MazePoint with(String text) {

        String colText;
        String rowText;
        Integer col;
        Integer row;

        if (Api.isNull(text)) {
            return null;
        }

        text = text.trim();
        if (text.length() < 2) {
            Api.error("MazePoint is not valid", text);
            return null;
        }

        colText = text.substring(0, 1);

        col = ColMap.get(colText);
        if (col == null) {
            Api.error("MazePoint column is not valid", text);
            return null;
        }

        try {
            rowText = text.substring(1);
            row = Integer.valueOf(rowText);
            if ((row < 1) || (row > Setup.MAZE_ROW_MAX)) {
                Api.error("MazePoint row is not within range [1-" + Setup.MAZE_ROW_MAX + "]", text);
                return null;
            }
        } catch (Throwable t) {
            Api.error("MazePoint row is not valid", text);
            return null;
        }

        return new MazePoint(col.intValue(), row.intValue());
    }
}

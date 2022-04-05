package maze.handler.maze;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import maze.Setup;
import maze.base.Api;
import maze.base.RestOutput;
import maze.model.Maze;
import maze.model.MazeSolution;
import maze.store.maze.MazeRecord;

public class MazeHandler implements Comparable<MazeHandler> {

    private final int                       _mazeId;

    private final MazePoint                 _entrance;
    private final MazePoint                 _exit;
    private final boolean[][]               _grid;

    private final AtomicReference<MazePath> _minPathReference;
    private final AtomicReference<MazePath> _maxPathReference;

    private MazeHandler(int mazeId, MazePoint entrance, MazePoint exit, boolean[][] grid) {

        _mazeId = mazeId;

        _entrance = entrance;
        _exit = exit;
        _grid = grid;

        _minPathReference = new AtomicReference<MazePath>(null);
        _maxPathReference = new AtomicReference<MazePath>(null);
    }

    public int mazeId() {

        return _mazeId;
    }

    private boolean[][] grid() {

        return _grid;
    }

    private boolean[][] gridClone() {

        return Arrays.stream(grid()).map(boolean[]::clone).toArray(boolean[][]::new);
    }

    private MazePoint entrance() {

        return _entrance;
    }

    private MazePoint exit() {

        return _exit;
    }

    private MazePath computeMinPath() {

        MinMazeSolver minMazeSolver;

        // Clone the grid to be able to provide the maze while the solver is running
        minMazeSolver = new MinMazeSolver(entrance(), exit(), gridClone());

        return minMazeSolver.solve();
    }

    private MazePath computeMaxPath() {

        MaxMazeSolver maxMazeSolver;

        // Clone the grid to be able to provide the maze while the solver is running
        maxMazeSolver = new MaxMazeSolver(entrance(), exit(), gridClone());

        return maxMazeSolver.solve();
    }

    private MazePath minPath() {

        MazePath minPath;

        minPath = _minPathReference.get();
        if (minPath != null) {
            return minPath;
        }

        // Compute the Min Path for this Maze and cache the outcome
        minPath = computeMinPath();
        if (!_minPathReference.compareAndSet(null, minPath)) {
            return _minPathReference.get();
        }
        return minPath;
    }

    private MazePath maxPath() {

        MazePath maxPath;

        maxPath = _maxPathReference.get();
        if (maxPath != null) {
            return maxPath;
        }

        // Compute the Max Path for this Maze and cache the outcome
        maxPath = computeMaxPath();
        if (!_maxPathReference.compareAndSet(null, maxPath)) {
            return _maxPathReference.get();
        }
        return maxPath;
    }

    public RestOutput<Maze> retrieveMaze() {

        Maze maze;
        int colMax;
        int rowMax;
        String gridSize;
        String[] walls;

        colMax = grid().length - 2;
        rowMax = grid()[0].length - 2;

        gridSize = colMax + "x" + rowMax;

        walls = wallStream(grid()).map(MazePoint::toText).toArray(String[]::new);

        maze = new Maze(mazeId(), entrance().toText(), gridSize, walls);

        return RestOutput.ok(maze);
    }

    public RestOutput<MazeSolution> solveMinPath() {

        MazePath minPath;

        // Access or compute the Min Path for this maze
        minPath = minPath();

        if (minPath == MazePath.NO_PATH) {
            Api.error("Maze has no path to exit for Min Path", this);
            return RestOutput.ok(new MazeSolution("Maze has no path to exit for Min Path."));
        }

        if (minPath == MazePath.MULTIPLE_PATHS) {
            Api.error("Maze has multiple paths to exit for Min Path", this);
            return RestOutput.ok(new MazeSolution("Maze has multiple paths to exit for Min Path."));
        }

        return RestOutput.ok(new MazeSolution(minPath.toTextArray()));
    }

    public RestOutput<MazeSolution> solveMaxPath() {

        MazePath maxPath;

        // Access or compute the Max Path for this maze
        maxPath = maxPath();

        if (maxPath == MazePath.NO_PATH) {
            Api.error("Maze has no path to exit for Max Path", this);
            return RestOutput.ok(new MazeSolution("Maze has no path to exit for Max Path."));
        }

        if (maxPath == MazePath.MULTIPLE_PATHS) {
            Api.error("Maze has multiple paths to exit for Max Path", this);
            return RestOutput.ok(new MazeSolution("Maze has multiple paths to exit for Max Path."));
        }

        return RestOutput.ok(new MazeSolution(maxPath.toTextArray()));
    }

    public RestOutput<MazeRecord> buildMazeRecord(String userId) {

        RestOutput<Maze> mazeOutput;
        Maze maze;
        MazeRecord mazeRecord;

        if (Api.isNull(userId)) {
            return RestOutput.badRequest();
        }

        mazeOutput = retrieveMaze();
        if (RestOutput.isNOK(mazeOutput)) {
            Api.error("retrieveMaze to buildMazeRecord is NOT OK", mazeOutput, this);
            return RestOutput.of(mazeOutput);
        }
        maze = mazeOutput.output();

        mazeRecord = new MazeRecord(userId, maze.getMazeId(), maze.getEntrance(), maze.getGridSize(), maze.getWalls());

        return RestOutput.ok(mazeRecord);
    }

    @Override
    public int compareTo(MazeHandler mazeHandler) {

        return Integer.compare(mazeId(), mazeHandler.mazeId());
    }

    @Override
    public String toString() {
        return "MazeHandler [_mazeId=" + _mazeId
               + ", _entrance="
               + _entrance
               + ", _exit="
               + _exit
               + ", _grid="
               + _grid
               + ", _minPathReference="
               + _minPathReference
               + ", _maxPathReference="
               + _maxPathReference
               + "]";
    }

    private static boolean hasWallInColumn(int col, int rowFrom, int rowTo, boolean[][] grid) {

        for (int row = rowFrom; row <= rowTo; row++) {
            if (grid[col][row]) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasWallInRow(int colFrom, int colTo, int row, boolean[][] grid) {

        for (int col = colFrom; col <= colTo; col++) {
            if (grid[col][row]) {
                return true;
            }
        }
        return false;
    }

    private static int computeEmptyArea(int col, int row, boolean[][] grid) {

        int areaCol;
        int areaRow;
        boolean expandCol;
        boolean expandRow;

        // Point is a wall itself
        if (grid[col][row]) {
            return 0;
        }

        areaCol = col;
        areaRow = row;
        expandCol = true;
        expandRow = true;

        while (expandCol || expandRow) {

            if (expandCol) {
                if (hasWallInColumn(areaCol + 1, row, areaRow, grid)) {
                    expandCol = false;
                } else {
                    areaCol++;
                }
            }

            if (expandRow) {
                if (hasWallInRow(col, areaCol, areaRow + 1, grid)) {
                    expandRow = false;
                } else {
                    areaRow++;
                }
            }
        }

        if (areaCol == col) {
            // Not an area
            return 0;
        }

        if (areaRow == row) {
            // Not an area
            return 0;
        }

        return (areaCol + 1 - col) * (areaRow + 1 - row);
    }

    private static Optional<MazePoint> findEmptyArea(AtomicInteger area, boolean[][] grid) {

        MazePoint mazePoint;
        int emptyArea;
        int colMin;
        int colMax;
        int rowMin;
        int rowMax;

        colMin = 1;
        colMax = grid.length - 2;

        rowMin = 1;
        rowMax = grid[0].length - 2;

        for (int row = rowMin; row <= rowMax; row++) {
            for (int col = colMin; col <= colMax; col++) {

                // Compute the empty area for this point
                emptyArea = computeEmptyArea(col, row, grid);

                if (emptyArea > Setup.MAZE_EMPTY_AREA_MAX) {

                    mazePoint = new MazePoint(col, row);
                    Api.error("Empty Area for " + mazePoint.toText()
                              + " is "
                              + emptyArea
                              + " and above limit of "
                              + Setup.MAZE_EMPTY_AREA_MAX);
                    area.set(emptyArea);

                    // This MazePoint is part of an empty area which is too large
                    return Optional.of(mazePoint);
                }
            }
        }
        return Optional.empty();
    }

    private static Stream<MazePoint> wallStream(boolean[][] grid) {

        Stream.Builder<MazePoint> streamBuilder;
        int colMin;
        int colMax;
        int rowMin;
        int rowMax;

        streamBuilder = Stream.builder();

        colMin = 1;
        colMax = grid.length - 2;

        rowMin = 1;
        rowMax = grid[0].length - 2;

        for (int row = rowMin; row <= rowMax; row++) {
            for (int col = colMin; col <= colMax; col++) {

                if (grid[col][row]) {
                    streamBuilder.add(new MazePoint(col, row));
                }
            }
        }

        return streamBuilder.build();
    }

    private static Stream<MazePoint> bottomStream(boolean[][] grid) {

        Stream.Builder<MazePoint> streamBuilder;
        int colMin;
        int colMax;
        int rowMax;

        streamBuilder = Stream.builder();

        colMin = 1;
        colMax = grid.length - 2;
        rowMax = grid[0].length - 2;

        for (int col = colMin; col <= colMax; col++) {
            streamBuilder.add(new MazePoint(col, rowMax));
        }
        return streamBuilder.build();
    }

    private static RestOutput<Set<MazePoint>> computeExitSet(MazePoint entrance, boolean[][] grid) {

        Set<MazePoint> exitSet;

        if (Api.isNull(entrance, grid)) {
            return RestOutput.badRequest();
        }

        // Consider a possible exit as one of the bottom edge which is not a wall and not the entrance
        exitSet = bottomStream(grid).filter(mp -> !grid[mp.col()][mp.row()])
                                    .filter(mp -> !entrance.equals(mp))
                                    .collect(Collectors.toSet());

        return RestOutput.ok(exitSet);
    }

    public static RestOutput<MazeHandler> with(Maze maze,
                                               Supplier<Integer> mazeIdSupplier,
                                               AtomicReference<String> errorReference) {

        String[] gridArray;
        Integer col;
        Integer row;
        AtomicInteger area;
        Optional<MazePoint> emptyAreaOptional;
        MazePoint entrance;
        RestOutput<Set<MazePoint>> exitSetOutput;
        Set<MazePoint> exitSet;
        MazePoint exit;
        MazePoint mazePoint;
        boolean[][] grid;
        Integer mazeId;
        MazeHandler mazeHandler;

        if (Api.isNull(maze, maze.getEntrance(), maze.getGridSize(), maze.getWalls(), mazeIdSupplier, errorReference)) {
            return RestOutput.badRequest();
        }

        try {

            entrance = MazePoint.with(maze.getEntrance());
            if (entrance == null) {
                errorReference.set("Entrance is not well defined.");
                Api.error("Entrance is not well defined. BAD REQUEST", maze);
                return RestOutput.badRequest();
            }

            gridArray = maze.getGridSize().split("x");
            if ((gridArray == null) || (gridArray.length != 2)) {
                errorReference.set("Grid is not valid (Example 10x10).");
                Api.error("Grid is not well defined. BAD REQUEST", maze);
                return RestOutput.badRequest();
            }

            col = Integer.valueOf(gridArray[0]);
            row = Integer.valueOf(gridArray[1]);

            if ((col < 1) || (col > Setup.MAZE_COLUMNS.length)) {
                errorReference.set("Grid must have between 1 and " + Setup.MAZE_COLUMNS.length + " columns.");
                Api.error("Grid size is not valid. BAD REQUEST", maze);
                return RestOutput.badRequest();
            }

            if ((row < 1) || (row > Setup.MAZE_ROW_MAX)) {
                errorReference.set("Grid must have between 1 and " + Setup.MAZE_ROW_MAX + " rows.");
                Api.error("Grid size is not valid. BAD REQUEST", maze);
                return RestOutput.badRequest();
            }

            // Generate a grid with 4 extra borders to bound the solver
            grid = new boolean[col + 2][row + 2];
            for (int i = 0; i < row + 2; i++) {
                grid[0][i] = true;
                grid[col + 1][i] = true;
            }
            for (int i = 0; i < col + 2; i++) {
                grid[i][0] = true;
                grid[i][row + 1] = true;
            }

            for (String wall : maze.getWalls()) {

                mazePoint = MazePoint.with(wall);
                if (mazePoint == null) {
                    errorReference.set("A wall entry is not well formed like A1.");
                    Api.error("Walls are not valid. BAD REQUEST", wall, maze);
                    return RestOutput.badRequest();
                }

                if ((mazePoint.col() > col) || ((mazePoint.row() > row))) {
                    errorReference.set("A wall " + mazePoint.toText() + " is outside of the grid.");
                    Api.error("A wall is outside of the grid. BAD REQUEST", wall, mazePoint, maze);
                    return RestOutput.badRequest();
                }

                grid[mazePoint.col()][mazePoint.row()] = true;
            }

            // Find any MazePoint whose empty area is too large
            area = new AtomicInteger(0);
            emptyAreaOptional = findEmptyArea(area, grid);
            if (emptyAreaOptional.isPresent()) {
                errorReference.set("Empty area at " + emptyAreaOptional.get()
                                   + " is "
                                   + area.get()
                                   + " and above the limit of "
                                   + Setup.MAZE_EMPTY_AREA_MAX
                                   + ".");
                Api.error("Empty Area detected. BAD REQUEST", emptyAreaOptional, maze);
                return RestOutput.badRequest();
            }

            // Validate Entrance
            if ((entrance.col() == 1) || ((entrance.col() == col))) {
                if ((entrance.row() < 1) || ((entrance.row() > row))) {
                    errorReference.set("Entrance is not on the edge of the grid.");
                    Api.error("Entrance is not on the edge of the grid. BAD REQUEST", entrance, col, row, maze);
                    return RestOutput.badRequest();
                }
            } else if ((entrance.row() == 1) || ((entrance.row() == row))) {
                if ((entrance.col() < 1) || ((entrance.col() > col))) {
                    errorReference.set("Entrance is not on the edge of the grid.");
                    Api.error("Entrance is not on the edge of the grid. BAD REQUEST", entrance, col, row, maze);
                    return RestOutput.badRequest();
                }
            } else {
                errorReference.set("Entrance is not on the edge of the grid.");
                Api.error("Entrance is not on the edge of the grid. BAD REQUEST", entrance, col, row, maze);
                return RestOutput.badRequest();
            }

            // Make sure the entrance is not a wall
            if (grid[entrance.col()][entrance.row()]) {
                errorReference.set("Entrance is a wall.");
                Api.error("Entrance is a wall. BAD REQUEST", entrance, maze);
                return RestOutput.badRequest();
            }

            // Compute the potential set of exits
            exitSetOutput = computeExitSet(entrance, grid);
            if (RestOutput.isNOK(exitSetOutput)) {
                Api.error("computeExitSet for MazeHandler is NOT OK", exitSetOutput, entrance, maze);
                return RestOutput.of(exitSetOutput);
            }
            exitSet = exitSetOutput.output();

            // Reject the maze if there is no possible exit even before path computation
            if (exitSet.isEmpty()) {
                errorReference.set("There is no possible exit as bottom edge cells.");
                Api.error("No possible exit to this maze. BAD REQUEST", entrance, maze);
                return RestOutput.badRequest();
            }

            if (exitSet.size() > 1) {
                errorReference.set("There is multiple exits as bottom edge cells.");
                Api.error("Multiple exits to this maze. BAD REQUEST", entrance, maze);
                return RestOutput.badRequest();
            }
            exit = exitSet.stream().findFirst().get();

            mazeId = mazeIdSupplier.get();

            mazeHandler = new MazeHandler(mazeId.intValue(), entrance, exit, grid);

            return RestOutput.ok(mazeHandler);

        } catch (Throwable t) {
            Api.error(t, "Maze failed. INTERNAL FAILURE", maze);
            return RestOutput.internalFailure();
        }
    }
}

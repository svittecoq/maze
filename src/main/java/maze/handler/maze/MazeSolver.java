package maze.handler.maze;

import java.util.ArrayDeque;

import maze.base.Api;

public abstract class MazeSolver {

    private final MazePoint             _entrance;
    private final MazePoint             _exit;
    private final boolean[][]           _grid;

    private final ArrayDeque<MazeTrack> _stack;

    public MazeSolver(MazePoint entrance, MazePoint exit, boolean[][] grid) {

        _entrance = entrance;
        _exit = exit;
        _grid = grid;

        _stack = new ArrayDeque<MazeTrack>();
    }

    protected abstract MazeTrack forwardFromCurrent(MazeTrack mazeTrack);

    protected abstract SolverAction processPath(int pathLength, MazePath solverPath);

    protected abstract boolean pruneForward(int currentPathLength, MazePath solverPath);

    protected MazePoint entrance() {

        return _entrance;
    }

    protected MazePoint exit() {

        return _exit;
    }

    protected boolean[][] grid() {

        return _grid;
    }

    protected ArrayDeque<MazeTrack> stack() {

        return _stack;
    }

    protected MazeTrack push(MazePoint mazePoint) {

        MazeTrack mazeTrack;

        // Set the Grid to detect loops
        grid()[mazePoint.col()][mazePoint.row()] = true;

        mazeTrack = new MazeTrack(mazePoint);

        // Push the MazeTrack
        stack().addLast(mazeTrack);

        return mazeTrack;
    }

    private MazeTrack pop() {

        MazeTrack mazeTrack;
        MazePoint mazePoint;

        // Pop the stack
        mazeTrack = stack().removeLast();
        mazePoint = mazeTrack.mazePoint();

        // Clear the Grid
        grid()[mazePoint.col()][mazePoint.row()] = false;

        // Return the current last of the stack if not empty;
        return stack().peekLast();
    }

    private MazeTrack backtrackFromCurrent() {

        // Remove the last in the stack if not empty
        return pop();
    }

    protected MazePath solve() {

        MazeTrack currentTrack;
        MazePoint currentPoint;
        MazePath solverPath;
        int solverPathCount;
        long iteration;

        solverPath = MazePath.NO_PATH;
        solverPathCount = 0;
        currentPoint = null;
        iteration = 0L;

        // Push Entrance
        currentTrack = push(entrance());

        while (currentTrack != null) {

            iteration++;
            if (iteration % 50000 == 0) {
                if (solverPath.isNoPath()) {
                    Api.info("Iteration " + iteration + " to solve maze: No path yet", this);
                } else {
                    Api.info("Iteration " + iteration + " to solve maze: Current path : " + solverPath.toText(), this);
                }
            }

            // Find any potential way forward to keep extending the current path
            currentTrack = forwardFromCurrent(currentTrack);

            if (currentTrack != null) {

                // There is a way forward

                currentPoint = currentTrack.mazePoint();

                if (exit().equals(currentPoint)) {

                    // Select this path as a better solution or not
                    switch (processPath(stack().size(), solverPath)) {
                    case SELECT:
                        // This path is now the current solution
                        solverPath = new MazePath(stack().stream().map(MazeTrack::mazePoint).toArray(MazePoint[]::new));
                        solverPathCount = 1;
                        break;
                    case COUNT:
                        // A path of the same length has been identified
                        solverPathCount++;
                        break;
                    case SKIP:
                        // This path is of no interest
                        break;
                    }

                    // Force backtrack from exit.
                    currentTrack.endForward();
                    currentTrack = null;

                } else if (pruneForward(stack().size(), solverPath)) {

                    // Force backtrack because of pruning
                    currentTrack.endForward();
                    currentTrack = null;
                }
            }

            if (currentTrack == null) {
                // Backtrack if stack is not empty
                currentTrack = backtrackFromCurrent();
            }

        }
        if (solverPathCount > 1) {
            // Solver found multiple paths
            Api.error("Solver has identified multiple paths", this);
            return MazePath.MULTIPLE_PATHS;
        }

        return solverPath;
    }
}

package maze.handler.maze;

public class MinMazeSolver extends MazeSolver {

    public MinMazeSolver(MazePoint entrance, MazePoint exit, boolean[][] grid) {
        super(entrance, exit, grid);
    }

    @Override
    protected MazeTrack forwardFromCurrent(MazeTrack mazeTrack) {

        MazePoint mazePoint;

        mazePoint = mazeTrack.mazePoint();

        // Go forward from the current point, down first, up last.

        if (mazeTrack.trackDown()) {
            // Down Neighbor
            if (grid()[mazePoint.col()][mazePoint.row() + 1] == false) {
                return push(new MazePoint(mazePoint.col(), mazePoint.row() + 1));
            }
        }

        if (mazeTrack.trackLeft()) {
            // Left Neighbor
            if (grid()[mazePoint.col() - 1][mazePoint.row()] == false) {
                return push(new MazePoint(mazePoint.col() - 1, mazePoint.row()));
            }
        }

        if (mazeTrack.trackRight()) {
            // Right Neighbor
            if (grid()[mazePoint.col() + 1][mazePoint.row()] == false) {
                return push(new MazePoint(mazePoint.col() + 1, mazePoint.row()));
            }
        }

        if (mazeTrack.trackUp()) {
            // Up Neighbor
            if (grid()[mazePoint.col()][mazePoint.row() - 1] == false) {
                return push(new MazePoint(mazePoint.col(), mazePoint.row() - 1));
            }
        }

        // No way forward
        return null;
    }

    @Override
    protected SolverAction processPath(int pathLength, MazePath solverPath) {

        if (solverPath.isNoPath()) {
            // Solver found the first path to the exit
            return SolverAction.SELECT;
        }

        if (solverPath.length() < pathLength) {
            // Solver has already found a path strictly shorter than the current one. Skip it.
            return SolverAction.SKIP;
        }

        if (solverPath.length() > pathLength) {
            // Solver has found a path strictly greater than the current one. Select it.
            return SolverAction.SELECT;
        }

        // Increment the count of paths of this length to detect multiple paths of same length
        return SolverAction.COUNT;
    }

    @Override
    protected boolean pruneForward(int currentPathLength, MazePath solverPath) {

        // Solver has already found a path. Prune anything longer
        return ((solverPath.length() > 0) && (currentPathLength > solverPath.length()));
    }
}

package maze.store.maze;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import maze.base.Api;
import maze.base.RestOutput;
import maze.base.Result;
import maze.store.Collection;
import maze.store.StoreService;

public class MazeCollection extends Collection<MazeRecord> {

    private static final String   COLLECTION   = "maze_collection";

    private static final String   UserId       = "user_id";
    private static final String   MazeId       = "maze_id";
    private static final String   MazeEntrance = "maze_entrance";
    private static final String   MazeGridSize = "maze_grid_size";
    private static final String   MazeWalls    = "maze_walls";

    private static final String[] FieldArray   = new String[] { UserId, MazeId, MazeEntrance, MazeGridSize, MazeWalls };

    public MazeCollection(StoreService storeService) {
        super(COLLECTION, FieldArray, storeService);
    }

    @Override
    protected MazeRecord to(ResultSet resultSet) throws SQLException {

        String userId;
        int mazeId;
        String entrance;
        String gridSize;
        String wallsText;
        String[] walls;

        if (Api.isNull(resultSet)) {
            return null;
        }

        userId = resultSet.getString(1);
        mazeId = resultSet.getInt(2);
        entrance = resultSet.getString(3);
        gridSize = resultSet.getString(4);

        wallsText = resultSet.getString(5);
        if (wallsText.isBlank()) {
            walls = new String[0];
        } else {
            walls = wallsText.split(",");
        }

        return new MazeRecord(userId, mazeId, entrance, gridSize, walls);
    }

    @Override
    protected String[] from(MazeRecord mazeRecord) {

        if (Api.isNull(mazeRecord)) {
            return null;
        }

        return new String[] { mazeRecord.userId(),
                              mazeRecord.mazeId().toString(),
                              mazeRecord.entrance(),
                              mazeRecord.gridSize(),
                              mazeRecord.wallsText() };
    }

    public RestOutput<Result> initCollection() {

        return init(textEntry(UserId),
                    textEntry(MazeId),
                    textEntry(MazeEntrance),
                    textEntry(MazeGridSize),
                    textEntry(MazeWalls),
                    primaryKeyEntry(UserId, MazeId));
    }

    public RestOutput<Result> storeMazeRecord(MazeRecord mazeRecord) {

        return storeRecord(mazeRecord);
    }

    public RestOutput<List<MazeRecord>> loadMazeRecords() {

        return loadRecords();
    }
}

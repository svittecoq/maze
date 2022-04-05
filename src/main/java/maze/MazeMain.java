package maze;

import java.util.Optional;

import maze.base.Api;
import maze.base.RestOutput;
import maze.base.Result;
import maze.handler.core.CoreHandler;

public class MazeMain {

    public static void main(String[] args) {

        RestOutput<CoreHandler> coreHandlerOutput;
        CoreHandler coreHandler;
        RestOutput<Result> resultOutput;

        coreHandlerOutput = CoreHandler.with(Optional.empty(),
                                             Optional.empty(),
                                             Optional.empty(),
                                             Optional.empty(),
                                             Optional.of(Setup.WEB_URL));
        if (RestOutput.isNOK(coreHandlerOutput)) {
            Api.error("CoreHandler creation is NOT OK", coreHandlerOutput);
            System.exit(-1);
        }
        coreHandler = coreHandlerOutput.output();

        // Run the CoreHandler
        resultOutput = coreHandler.run();
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("CoreHandler run is NOT OK", resultOutput);
            System.exit(-1);
        }
    }
}

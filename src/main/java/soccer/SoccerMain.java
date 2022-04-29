package soccer;

import java.net.URI;
import java.util.Optional;

import soccer.base.Api;
import soccer.base.RestOutput;
import soccer.base.Result;
import soccer.handler.core.CoreHandler;

public class SoccerMain {

    public static void main(String[] args) {

        URI databaseURI;
        Optional<String> webPathOptional;
        Optional<Integer> webPortOptional;

        String databaseURIProperty = System.getProperty(Setup.DATABASE_URI_PROPERTY);
        String webPathProperty = System.getProperty(Setup.WEB_PATH_PROPERTY);
        String webPortProperty = System.getProperty(Setup.WEB_PORT_PROPERTY);

        // Use the provided DatabaseURI or the default one
        if (databaseURIProperty != null) {
            Api.info("Property " + Setup.DATABASE_URI_PROPERTY + " = " + databaseURIProperty);
            databaseURI = Api.URI(databaseURIProperty);
        } else {
            databaseURI = Api.URI(Setup.DEFAULT_STORE_URI + Setup.DATABASE_ID);
        }

        // Use the Web Path if provided
        if (webPathProperty != null) {
            Api.info("Property " + Setup.WEB_PATH_PROPERTY + " = " + webPathProperty);
            webPathOptional = Optional.of(webPathProperty);
        } else {
            webPathOptional = Optional.empty();
        }

        // Use the Web Port if provided
        if (webPortProperty != null) {
            Api.info("Property " + Setup.WEB_PORT_PROPERTY + " = " + webPortProperty);
            webPortOptional = Optional.of(Integer.valueOf(webPortProperty));
        } else {
            webPortOptional = Optional.empty();
        }

        RestOutput<CoreHandler> coreHandlerOutput;
        CoreHandler coreHandler;
        RestOutput<Result> resultOutput;

        coreHandlerOutput = CoreHandler.with(databaseURI, webPathOptional, webPortOptional);
        if (RestOutput.isNOK(coreHandlerOutput)) {
            Api.error("CoreHandler creation is NOT OK",
                      databaseURI,
                      webPathOptional,
                      webPortOptional,
                      coreHandlerOutput);
            System.exit(-1);
        }
        coreHandler = coreHandlerOutput.output();

        // Run the CoreHandler
        resultOutput = coreHandler.run();
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("CoreHandler run is NOT OK", resultOutput, databaseURI, webPathOptional, webPortOptional);
            System.exit(-1);
        }
    }
}

package soccer.http;

import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.rewrite.handler.RedirectRegexRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

import soccer.Setup;
import soccer.base.Api;
import soccer.base.RestOutput;
import soccer.base.Result;
import soccer.http.servlet.HtmlServlet;
import soccer.http.servlet.LoginServlet;
import soccer.model.UserToken;
import soccer.rest.RestService;

public class HttpService {

    private final Optional<String>  _webPathOptional;
    private final Optional<Integer> _webPortOptional;
    private final RestService       _restService;
    private final List<HtmlServlet> _htmlServletList;
    private final Path              _webFolderPath;

    private URL                     _url;
    private HttpServer              _httpServer;

    public HttpService(Optional<String> webPathOptional,
                       Optional<Integer> webPortOptional,
                       RestService restService,
                       List<HtmlServlet> htmlServletList,
                       Path webFolderPath) {

        _webPathOptional = webPathOptional;
        _webPortOptional = webPortOptional;
        _restService = restService;
        _htmlServletList = htmlServletList;
        _webFolderPath = webFolderPath;

        _url = null;
        _httpServer = null;
    }

    private Optional<String> webPathOptional() {

        return _webPathOptional;
    }

    private Optional<Integer> webPortOptional() {

        return _webPortOptional;
    }

    private RestService restService() {
        return _restService;
    }

    private List<HtmlServlet> htmlServletList() {
        return _htmlServletList;
    }

    private Path webFolderPath() {
        return _webFolderPath;
    }

    private void url(URL url) {

        _url = url;
    }

    public URL url() {

        return _url;
    }

    public RestOutput<Result> start() {

        try {

            // Setup Server with pool of Threads
            _httpServer = new HttpServer();

            ServerConnector connector = new ServerConnector(_httpServer);

            if (webPathOptional().isPresent()) {
                if (webPortOptional().isPresent()) {
                    URL url = new URL(webPathOptional().get() + ":" + webPortOptional().get());
                    connector.setHost(url.getHost());
                    connector.setPort(url.getPort());
                } else {
                    URL url = new URL(webPathOptional().get());
                    connector.setHost(url.getHost());
                    connector.setPort(0);
                }
            } else {
                connector.setHost(null);
                if (webPortOptional().isPresent()) {
                    connector.setPort(webPortOptional().get());
                } else {
                    connector.setPort(0);
                }
            }
            _httpServer.addConnector(connector);

            // Scheduler
            _httpServer.addBean(new ScheduledExecutorScheduler());

            // --------------------------
            // Rewrite Handler
            // --------------------------
            String welcomeURL = null;
            for (HtmlServlet htmlServlet : _htmlServletList) {

                if (LoginServlet.class.isInstance(htmlServlet)) {
                    LoginServlet loginServlet = LoginServlet.class.cast(htmlServlet);
                    welcomeURL = Setup.UI_PATH + loginServlet.url();
                    break;
                }
            }
            if (welcomeURL == null) {
                Api.error("No Login Servlet defined. INTERNAL FAILURE", this);
                return RestOutput.internalFailure();
            }

            RewriteHandler rewriteHandler = new RewriteHandler();
            rewriteHandler.setRewriteRequestURI(true);
            rewriteHandler.setRewritePathInfo(true);
            rewriteHandler.setOriginalPathAttribute("requestedPath");
            RedirectRegexRule rule = new RedirectRegexRule();
            rule.setRegex("/");
            rule.setLocation(welcomeURL);
            rewriteHandler.addRule(rule);

            // --------------------------
            // HTML Handler
            // --------------------------
            ServletContextHandler htmlContextHandler = new ServletContextHandler(_httpServer,
                                                                                 Setup.UI_PATH,
                                                                                 ServletContextHandler.SESSIONS);

            for (HtmlServlet htmlServlet : htmlServletList()) {
                ServletHolder servletHolder = new ServletHolder(htmlServlet.url(), htmlServlet);
                htmlContextHandler.addServlet(servletHolder, htmlServlet.url());
            }

            // --------------------------
            // Resource Handler for static resources
            // --------------------------
            List<String> resourceCollectionList = new ArrayList<String>();

            RestOutput<URL> webURLOutput = Api.locateResource(webFolderPath(), this.getClass().getClassLoader());
            if (RestOutput.isNOK(webURLOutput)) {
                Api.error("locateResource to start HttpManager is NOT OK", webURLOutput, this);
                return RestOutput.of(webURLOutput);
            }
            resourceCollectionList.add(webURLOutput.output().toExternalForm());
            ResourceCollection resourceCollection = new ResourceCollection(resourceCollectionList.toArray(new String[resourceCollectionList.size()]));

            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setBaseResource(resourceCollection);

            htmlContextHandler.insertHandler(resourceHandler);

            // --------------------------
            // Rest Handler
            // --------------------------
            ServletContextHandler restContextHandler = new ServletContextHandler(_httpServer,
                                                                                 "/",
                                                                                 ServletContextHandler.SESSIONS);

            // Set the Application Name
            restService().property(ServerProperties.APPLICATION_NAME, "Soccer");

            ServletContainer restServletContainer = new ServletContainer(restService());

            ServletHolder restServletHolder = new ServletHolder(restServletContainer);
            restServletHolder.setInitParameter("dirAllowed", "false");

            restContextHandler.addServlet(restServletHolder, restService().pathSpecification());

            // --------------------------
            // Default Handler
            // --------------------------
            DefaultHandler defaultHandler = new DefaultHandler();
            defaultHandler.setServeIcon(false);
            defaultHandler.setShowContexts(false);

            // Register all handlers
            HandlerList handlerList = new HandlerList();
            handlerList.addHandler(rewriteHandler);
            handlerList.addHandler(htmlContextHandler);
            handlerList.addHandler(restContextHandler);
            handlerList.addHandler(defaultHandler);

            _httpServer.setHandler(handlerList);

            // Extra options
            _httpServer.setDumpAfterStart(false);
            _httpServer.setDumpBeforeStop(false);
            _httpServer.setStopAtShutdown(true);

            // Start the server
            _httpServer.start();

            // Set the URL which may have been dynamically assigned
            Connector[] connectorArray = _httpServer.getConnectors();
            if ((connectorArray == null) || (connectorArray.length != 1)) {
                Api.error("Connectors are not set properly. INTERNAL FAILURE", this);
                return RestOutput.internalFailure();
            }
            ServerConnector serverConnector = (ServerConnector) connectorArray[0];

            String protocol = serverConnector.getDefaultConnectionFactory().getProtocol();
            String scheme = "http";
            if (protocol.startsWith("SSL-") || protocol.equals("SSL")) {
                scheme = "https";
            }

            String host = serverConnector.getHost();
            if (host == null) {
                host = InetAddress.getLocalHost().getHostAddress();
            }

            url(new URL(scheme, host, serverConnector.getLocalPort(), ""));

            while (_httpServer.isStarted() == false) {
                Api.info("Wait for HTTP Server to run. Sleep 100 ms", _httpServer, this);
                Thread.sleep(100);
            }

            Api.info("HTTP Server started: " + url(), this);

        } catch (Throwable t) {
            try {
                if (_httpServer != null) {
                    _httpServer.stop();
                }
            } catch (Exception e) {
                Api.error(e, "Unable to stop HTTP server");
            } finally {
                _httpServer = null;
            }
            Api.error(t, "Unable to start HTTP server. INTERNAL FAILURE");
            return RestOutput.internalFailure();
        }

        return RestOutput.OK;
    }

    public RestOutput<Result> stop() {

        if (_httpServer != null) {
            if (!_httpServer.isRunning()) {
                return RestOutput.OK;
            }
            try {
                while (!_httpServer.isStopped()) {
                    _httpServer.stop();
                }
            } catch (Throwable t) {
                Api.error(t, "Unable to stop the running server. INTERNAL FAILURE");
                return RestOutput.internalFailure();
            }
        }
        return RestOutput.OK;
    }

    @Override
    public String toString() {
        return "HttpService [_webPathOptional=" + _webPathOptional
               + ", _webPortOptional="
               + _webPortOptional
               + ", _restService="
               + _restService
               + ", _htmlServletList="
               + _htmlServletList
               + ", _webFolderPath="
               + _webFolderPath
               + ", _url="
               + _url
               + ", _httpServer="
               + _httpServer
               + "]";
    }

    public static Optional<UserToken> searchUserToken(HttpServletRequest httpRequest) {

        if (Api.isNull(httpRequest)) {
            return Optional.empty();
        }

        return Optional.ofNullable(httpRequest.getHeader(Setup.USER_TOKEN)).map(UserToken::new);
    }
}

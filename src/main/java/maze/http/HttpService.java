package maze.http;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.rewrite.handler.RedirectRegexRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

import maze.Setup;
import maze.base.Api;
import maze.base.RestOutput;
import maze.base.Result;
import maze.http.servlet.HtmlServlet;
import maze.http.servlet.LoginServlet;
import maze.model.SessionToken;
import maze.rest.RestService;

public class HttpService {

    private final Optional<String>  _webUrlOptional;
    private final RestService       _restService;
    private final List<HtmlServlet> _htmlServletList;
    private final Path              _webFolderPath;

    private URL                     _url;
    private HttpServer              _httpServer;

    public HttpService(Optional<String> webUrlOptional,
                       RestService restService,
                       List<HtmlServlet> htmlServletList,
                       Path webFolderPath) {

        _webUrlOptional = webUrlOptional;
        _restService = restService;
        _htmlServletList = htmlServletList;
        _webFolderPath = webFolderPath;

        _url = null;
        _httpServer = null;
    }

    private Optional<String> webUrlOptional() {

        return _webUrlOptional;
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

        KeyStore keyStore;
        KeyStore trustStore;

        try {

            // Setup Server with pool of Threads
            _httpServer = new HttpServer();

            char[] keyStorePassword = Password.deobfuscate(Setup.KEY_STORE_PASSWORD).toCharArray();
            keyStore = KeyStore.getInstance("jks");
            try (InputStream inputStream = Api.openResourceStream(Setup.KEY_STORE_PATH)) {
                keyStore.load(inputStream, keyStorePassword);
            }

            char[] trustStorePassword = Password.deobfuscate(Setup.TRUST_STORE_PASSWORD).toCharArray();
            trustStore = KeyStore.getInstance("jks");
            try (InputStream inputStream = Api.openResourceStream(Setup.TRUST_STORE_PATH)) {
                trustStore.load(inputStream, trustStorePassword);
            }

            // The HTTP configuration object.
            HttpConfiguration httpConfig = new HttpConfiguration();

            httpConfig.setSendServerVersion(false);
            httpConfig.setSendDateHeader(false);

            // Add the SecureRequestCustomizer because we are using TLS.
            httpConfig.addCustomizer(new SecureRequestCustomizer());

            // The ConnectionFactory for HTTP/1.1.
            HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

            // The ConnectionFactory for HTTP/2.
            HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpConfig);

            // The ALPN ConnectionFactory.
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
            // The default protocol to use in case there is no negotiation.
            alpn.setDefaultProtocol(http11.getProtocol());

            // Configure the SslContextFactory with the keyStore information.
            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStore(keyStore);
            sslContextFactory.setTrustStore(trustStore);
            sslContextFactory.setKeyStorePassword(new String(keyStorePassword));

            // The ConnectionFactory for TLS.
            SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

            // The ServerConnector instance.
            ServerConnector connector = new ServerConnector(_httpServer, tls, alpn, h2, http11);

            if (webUrlOptional().isPresent()) {

                URL url = new URL(webUrlOptional().get());
                connector.setHost(url.getHost());
                connector.setPort(url.getPort());
            } else {
                connector.setHost(null);
                connector.setPort(0);
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

            int corsss;
            // Add the CORS filter for HTML
            // FilterHolder htmlCorsFilter = htmlContextHandler.addFilter(CrossOriginFilter.class,
            // "/*",
            // EnumSet.of(DispatcherType.REQUEST));
            // htmlCorsFilter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
            // htmlCorsFilter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
            // htmlCorsFilter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM,
            // "GET,POST,OPTIONS,DELETE,PUT,HEAD");
            // htmlCorsFilter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "*");

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

            int cors;
            // Add the CORS filter for REST
            // FilterHolder restCorsFilter = restContextHandler.addFilter(CrossOriginFilter.class,
            // "/*",
            // EnumSet.of(DispatcherType.REQUEST));
            // restCorsFilter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
            // restCorsFilter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
            // restCorsFilter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM,
            // "GET,POST,OPTIONS,DELETE,PUT,HEAD");
            // restCorsFilter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "*");

            // Set the Application Name
            restService().property(ServerProperties.APPLICATION_NAME, "Maze");

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

            url(new URL(HttpScheme.HTTPS.asString(), serverConnector.getHost(), serverConnector.getLocalPort(), ""));

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
        return "HttpService [_restService=" + _restService
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

    public static Optional<SessionToken> searchSessionToken(HttpServletRequest httpRequest) {

        HttpSession httpSession;
        Object attribute;
        Cookie[] cookies;

        if (Api.isNull(httpRequest)) {
            return Optional.empty();
        }

        httpSession = httpRequest.getSession(false);
        if (httpSession != null) {
            // Look for the attribute
            attribute = httpSession.getAttribute(Setup.SESSION_TOKEN);
            if (attribute != null) {
                if (SessionToken.class.isInstance(attribute) == false) {
                    Api.error("Session Attribute is not a SessionToken", Setup.SESSION_TOKEN);
                    return Optional.empty();
                }
                return Optional.of(SessionToken.class.cast(attribute));
            }
        }
        // Look for the Cookie
        cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (Objects.equals(Setup.SESSION_TOKEN, cookie.getName())) {
                    return Optional.ofNullable(SessionToken.with(cookie.getValue()));
                }
            }
        }
        return Optional.empty();
    }

    public static void assignSessionToken(HttpServletRequest httpRequest, SessionToken sessionToken) {

        HttpSession httpSession;

        if (Api.isNull(httpRequest, sessionToken)) {
            return;
        }

        httpSession = httpRequest.getSession(false);
        if (httpSession != null) {
            httpSession.setAttribute(Setup.SESSION_TOKEN, sessionToken);
        }
    }
}

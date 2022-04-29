package soccer.http;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import soccer.Setup;

public class HttpServer extends Server {

    public HttpServer() {
        super(new QueuedThreadPool(Setup.HTTP_THREAD_COUNT, Setup.HTTP_THREAD_COUNT));
    }
}

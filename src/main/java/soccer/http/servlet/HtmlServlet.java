package soccer.http.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;

import soccer.base.Api;
import soccer.base.RestCode;
import soccer.base.RestOutput;
import soccer.base.Result;
import soccer.handler.core.CoreHandler;

@SuppressWarnings("serial")
public abstract class HtmlServlet extends HttpServlet {

    private final String      _url;
    private final CoreHandler _coreHandler;

    public HtmlServlet(String url, CoreHandler coreHandler) {
        super();

        _url = url;
        _coreHandler = coreHandler;
    }

    protected abstract void get(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    public String url() {
        return _url;
    }

    protected CoreHandler coreHandler() {
        return _coreHandler;
    }

    protected RestOutput<Result> commit(List<String> htmlPage, HttpServletResponse httpResponse) {

        PrintWriter printWriter;

        if (Api.isNull(htmlPage, httpResponse)) {
            return RestOutput.badRequest();
        }

        try {
            printWriter = httpResponse.getWriter();
            for (String line : htmlPage) {
                printWriter.println(line);
            }

            httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setDateHeader("Expires", 0);

            return RestOutput.OK;

        } catch (Throwable t) {
            Api.error(t, "Failure to commit HTML Page. INTERNAL FAILURE", httpResponse, this);
            return RestOutput.internalFailure();
        }
    }

    protected void handleErrorCode(int errorStatusCode, String message, HttpServletResponse httpResponse) {

        try {
            httpResponse.sendError(errorStatusCode, message);
        } catch (IOException e) {
            Api.error(e, "Failure to sendError", errorStatusCode, message, this);
        }
    }

    protected void handleErrorCode(RestCode errorRestCode, String message, HttpServletResponse httpResponse) {

        try {
            httpResponse.sendError(httpStatusOfRestCode(errorRestCode), message);
        } catch (IOException e) {
            Api.error(e, "Failure to handleErrorCode", errorRestCode, message, this);
        }
    }

    protected void handleErrorCode(RestCode errorRestCode, HttpServletResponse httpResponse) {

        handleErrorCode(errorRestCode, "", httpResponse);
    }

    protected void commitErrorCode(int errorStatusCode, HttpServletResponse httpResponse) {

        // No Error Page generated
        httpResponse.setStatus(errorStatusCode);
    }

    protected void commitErrorCode(RestCode errorRestCode, HttpServletResponse httpResponse) {

        commitErrorCode(httpStatusOfRestCode(errorRestCode), httpResponse);
    }

    @Override
    protected final void doGet(HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) throws ServletException, IOException {

        try {
            // Process the GET request
            get(httpRequest, httpResponse);
        } catch (Throwable t) {
            Api.error(t, "Failure to process GET request", httpRequest, this);
            handleErrorCode(RestCode.INTERNAL_FAILURE, httpResponse);
            return;
        }
    }

    @Override
    public String toString() {
        return "HtmlServlet [_url=" + _url + "]";
    }

    public static int httpStatusOfRestCode(RestCode restCode) {

        switch (restCode) {
        case BAD_REQUEST:
            return HttpStatus.BAD_REQUEST_400;
        case FORBIDDEN:
            return HttpStatus.FORBIDDEN_403;
        case NOT_FOUND:
            return HttpStatus.NOT_FOUND_404;
        case TIMEOUT:
            return HttpStatus.REQUEST_TIMEOUT_408;
        case NOT_AVAILABLE:
            return HttpStatus.SERVICE_UNAVAILABLE_503;
        case INTERNAL_FAILURE:
        default:
            return HttpStatus.INTERNAL_SERVER_ERROR_500;
        }
    }

}
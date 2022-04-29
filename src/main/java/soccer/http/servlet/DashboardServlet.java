package soccer.http.servlet;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;

import soccer.Setup;
import soccer.base.Api;
import soccer.base.RestCode;
import soccer.base.RestOutput;
import soccer.base.Result;
import soccer.handler.core.CoreHandler;

@SuppressWarnings("serial")
public class DashboardServlet extends HtmlServlet {

    public DashboardServlet(CoreHandler coreHandler) {
        super("/dashboard", coreHandler);
    }

    @Override
    protected void get(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        RestOutput<List<String>> htmlPageOutput;
        List<String> htmlPage;
        RestOutput<Result> resultOutput;

        if (Api.isNull(httpRequest, httpResponse)) {
            Api.error("get attributes are null", httpRequest, httpResponse, this);
            return;
        }

        // Build the HTML Page
        htmlPageOutput = Api.loadHtmlPage(Setup.DASHBOARD_PAGE);
        if (RestOutput.isNOK(htmlPageOutput)) {
            Api.error("loadHtmlPage for get DashboardServlet is NOT OK", htmlPageOutput, this);
            commitErrorCode(htmlPageOutput.restCode(), httpResponse);
            return;
        }
        htmlPage = htmlPageOutput.output();

        // Commit the response
        resultOutput = commit(htmlPage, httpResponse);
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("commit failed. INTERNAL FAILURE", resultOutput, htmlPage, this);
            commitErrorCode(RestCode.INTERNAL_FAILURE, httpResponse);
            return;
        }

        httpResponse.setStatus(HttpStatus.OK_200);
    }
}
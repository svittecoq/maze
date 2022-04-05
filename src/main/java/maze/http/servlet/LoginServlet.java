package maze.http.servlet;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;

import maze.Setup;
import maze.base.Api;
import maze.base.RestCode;
import maze.base.RestOutput;
import maze.base.Result;
import maze.handler.core.CoreHandler;

@SuppressWarnings("serial")
public class LoginServlet extends HtmlServlet {

    public LoginServlet(CoreHandler coreHandler) {
        super("/login", coreHandler);
    }

    @Override
    protected void get(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        RestOutput<Result> resultOutput;
        RestOutput<List<String>> loginPageOutput;
        List<String> loginPage;

        if (Api.isNull(httpRequest, httpResponse)) {
            Api.error("get attributes for LoginServlet are null", httpRequest, httpResponse, this);
            return;
        }

        // Load the Login Page
        loginPageOutput = Api.loadHtmlPage(Setup.LOGIN_PAGE);
        if (RestOutput.isNOK(loginPageOutput)) {
            Api.error("loadHtmlPage for get LoginServlet is NOT OK", loginPageOutput, this);
            commitErrorCode(loginPageOutput.restCode(), httpResponse);
            return;
        }
        loginPage = loginPageOutput.output();

        // Return the Login Page
        resultOutput = commit(loginPage, httpResponse);
        if (RestOutput.isNOK(resultOutput)) {
            Api.error("commit failed. INTERNAL FAILURE", resultOutput, loginPage, this);
            commitErrorCode(RestCode.INTERNAL_FAILURE, httpResponse);
            return;
        }

        httpResponse.setStatus(HttpStatus.OK_200);
    }
}

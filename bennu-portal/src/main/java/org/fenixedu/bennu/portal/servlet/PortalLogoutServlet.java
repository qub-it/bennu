package org.fenixedu.bennu.portal.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.portal.BennuPortalConfiguration;

/**
 * Specialized servlet that logs the current user out.
 *
 * If CAS is enabled, the user is redirected to the CAS logout page, after
 * being logged out locally.
 *
 * If CAS is not enabled, the user is redirected to the application's root.
 *
 * @author João Carvalho (joao.pedro.carvalho@tecnico.ulisboa.pt)
 *
 */
@WebServlet({ "/logout", "/logout/*" })
public class PortalLogoutServlet extends HttpServlet {

    private static final String SAML_RESPONSE = "SAMLResponse";
    private static final String SAML_REQUEST = "SAMLRequest";

    @Override
    protected void doGet(javax.servlet.http.HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Logout locally
        Authenticate.logout(req, resp);

        if (!hasSamlPackage(request)) {
            resp.sendRedirect(StringUtils.defaultIfBlank(BennuPortalConfiguration.getConfiguration().logoutURL(),
                    req.getContextPath() + "/"));
        }
    }

    private static boolean hasSamlPackage(final HttpServletRequest request) {
        return request.getParameter(SAML_RESPONSE) != null || request.getParameter(SAML_REQUEST) != null;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 *
 * @author claas
 */
// @Component
class WwwAuthenticateFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(WwwAuthenticateFilter.class);

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain fc
    ) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        var path = httpRequest.getRequestURI();
        var session = httpRequest.getSession();

        logger.info(
                "%s %s".formatted(
                        session.getId(),
                        path
                )
        );

        final String[] whitelistPaths = {
            "/arachne/login",
            "/arachne/error",
            "/arachne/offline-stub.html",
            "/arachne/"
        };
        final String[] whitelistPathPrefixes = {
            "/arachne/VAADIN/build/"
        };
        if (httpRequest.getHeader("Authorization") != null
                || Arrays.stream(whitelistPaths).anyMatch(path::equals)
                || Arrays.stream(whitelistPathPrefixes).anyMatch(path::startsWith)) {
            fc.doFilter(request, response);
            return;
        }

        logger.info("Return 401 - UNAUTHORIZED for " + path);
        httpResponse.addHeader("WWW-Authenticate", "Negotiate");
        httpResponse.setContentType("text/html");
        httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        if (path.startsWith("/arachne/api")) {
            httpResponse.addHeader("WWW-Authenticate", "Basic realm=\"Arachne\"");
        }
        var out = httpResponse.getOutputStream();
        String url = "/arachne/login";
        out.println(
                """
                <html>
                <head>
                    <meta http-equiv="Refresh" content="1; url='%s'" />
                </head>
                <body>
                <h1>Unauthorized</h1>
                <p>You will be redirected to <a href="%s">login page</a>.</p>
                </body>
                </html>
                """.formatted(url, url));
    }
}

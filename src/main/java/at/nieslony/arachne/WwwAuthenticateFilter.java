/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WwwAuthenticateFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(WwwAuthenticateFilter.class);

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain fc
    ) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            fc.doFilter(request, response);
            return;
        }

        String path = httpRequest.getRequestURI();
        var session = httpRequest.getSession();

        if (path.equals("/arachne/sso")) {
            logger.info("Enforce sso");
            session.setAttribute("formLogin", "no");
            fc.doFilter(request, response);
            return;
        }

        if (path.equals("/arachne/login")
                || "yes".equals(session.getAttribute("formLogin"))) {
            session.setAttribute("formLogin", "yes");
            fc.doFilter(request, response);
            return;
        }

        String authenticate = httpRequest.getHeader("Authorization");
        if (authenticate != null) {
            logger.info("Try Negotiate");
            fc.doFilter(request, response);
            return;
        }

        logger.info("Return 401 - UNAUTHORIZED for " + path);
        httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        httpResponse.addHeader("WWW-Authenticate", "Negotiate");
        httpResponse.addHeader("WWW-Authenticate", "Basic realm=\"Arachne\"");
        session.setAttribute("tryNegotiate", "yes");
        var out = httpResponse.getOutputStream();
        String url = "/arachne/login";
        out.println(
                """
                <html>
                <head>
                    <meta http-equiv="Refresh" content="5; url='%s'" />
                </head>
                <body>
                <h1>Unauthorized</h1>
                <p>You will be redirected to <a href="%s">login page</a>.</p>
                </body>
                </html>
                """.formatted(url, url));
    }
}

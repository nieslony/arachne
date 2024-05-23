/*
 * Copyright (C) 2024 claas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.nieslony.arachne.auth;

import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 *
 * @author claas
 */
@RestController
public class SsoRestController {

    private static final org.slf4j.Logger logger
            = LoggerFactory.getLogger(SsoRestController.class);

    @GetMapping("/sso")
    @PermitAll
    public RedirectView getSso(HttpServletRequest request) {
        logger.info("Try sso from /sso");
        var session = request.getSession();
        if (session != null) {
            logger.info("Invalidating http session");
            session.invalidate();
        }
        var vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession != null) try {
            logger.info("Invalidating vaadon session");
            vaadinSession.close();
        } catch (IllegalStateException ex) {
            logger.info("Vaadin session already invalidated");
        }
        RedirectView view = new RedirectView("/", true);
        return view;
    }
}

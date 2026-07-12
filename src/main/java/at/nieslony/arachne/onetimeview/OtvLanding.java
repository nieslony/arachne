/*
 * Copyright (C) 2026 claas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package at.nieslony.arachne.onetimeview;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Tag("div")
@Route("otv")
@AnonymousAllowed
@Slf4j
public class OtvLanding
        extends Component
        implements HasUrlParameter<String> {

    private String otvId;

    public OtvLanding() {
    }

    @Override
    public void setParameter(BeforeEvent be, String otvId) {
        this.otvId = otvId;
        log.info("Setting otvId: " + otvId);

        VaadinSession session = VaadinSession.getCurrent();
        session.setAttribute("otvId", otvId);
        session.setAttribute("otvUser", "claas@NIESLONY.INTERNAL");

        String redirect = "/otv/%s/set-otp-token".formatted(otvId);
        //String redirect = "/otv/%s/login".formatted(otvId);
        log.info("Redirecting from %s to %s".formatted(otvId, redirect));

        if (!RouteConfiguration.forSessionScope().isRouteRegistered(SetOtpView.class)) {
            RouteConfiguration.forSessionScope()
                    .setRoute(
                            redirect, //path
                            SetOtpView.class //navigation target
                    );

        }

        UI.getCurrent().navigate(redirect);
    }

}

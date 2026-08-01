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

import at.nieslony.arachne.auth.TotpController;
import at.nieslony.arachne.users.UserModel;
import at.nieslony.arachne.users.UserRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 *
 * @author claas
 */
@PageTitle("Set OTP")
@RolesAllowed("USER")
@Slf4j
public class SetOtpView extends Main {

    @Autowired
    TotpController totpController;

    @Autowired
    UserRepository userRepository;

    @Autowired
    OneTimeViewRepository oneTimeViewRepository;

    public SetOtpView() {
    }

    @PostConstruct
    public void init() {
        String otvId = VaadinSession.getCurrent().getAttribute("otvId").toString();
        log.info("OTV set Token ");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserModel user = userRepository.findByUsername(authentication.getName());

        OneTimeViewModel otvModel = oneTimeViewRepository
                .findById(otvId)
                .orElseThrow(() -> {
                    log.error("There's no OTV woth id " + otvId);
                    return new AccessDeniedException();
                });
        var otvView = totpController.create2FAView(user, () -> {
            otvModel.setVisited(LocalDateTime.now());
            oneTimeViewRepository.save(otvModel);
            VaadinSession.getCurrent().setAttribute(
                    OtvDone.ATTR_SUCCESS_MSG,
                    "Your Authenticator App is successfully attached."
            );
            UI.getCurrent().navigate("/otv/%s/done".formatted(otvId));
        });

        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.add(
                new H1("Attach %s's Authenticator".formatted(user.getDisplayName())),
                otvView
        );
        layout.setSizeFull();

        add(layout);
        setSizeFull();
    }
}

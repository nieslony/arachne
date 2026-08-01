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

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@PageTitle("Congratulations!")
@RolesAllowed("USER")
@Slf4j
public class OtvDone extends Main {

    public static final String ATTR_SUCCESS_MSG = "successMsg";

    @PostConstruct
    public void init() {
        String otvId = VaadinSession.getCurrent().getAttribute("otvId").toString();
        String msg = Objects.requireNonNullElse(
                VaadinSession.getCurrent().getAttribute(ATTR_SUCCESS_MSG).toString(),
                ""
        );

        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.add(
                new H1("Congratulations!"),
                new Text(msg)
        );
        layout.setSizeFull();

        add(layout);
        setSizeFull();
    }

}

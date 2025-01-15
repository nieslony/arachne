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
package at.nieslony.arachne.errorpages;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * @author claas
 */
@Tag(Tag.DIV)
@AnonymousAllowed
public class RouteNotFound
        extends Component
        implements HasErrorParameter<NotFoundException> {

    @Override
    public int setErrorParameter(
            BeforeEnterEvent bee,
            ErrorParameter<NotFoundException> ep
    ) {
        getElement().setText(
                "Page \"%s\" not found."
                        .formatted(bee.getLocation().getPath())
        );

        return HttpServletResponse.SC_NOT_FOUND;
    }
}

/*
 * Copyright (C) 2023 claas
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
package at.nieslony.arachne.pki;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.openvpn.OpenVpnRestController;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Date;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 *
 * @author claas
 */
@Route(value = "certificates", layout = ViewTemplate.class)
@PageTitle("All Certificates | Arachne")
@RolesAllowed("ADMIN")
public class CertificatesView extends VerticalLayout {

    public CertificatesView(
            CertificateRepository certificateReposttory,
            OpenVpnRestController openVpnRestController
    ) {
        Grid<CertificateModel> grid = new Grid<>();
        grid
                .addColumn(CertificateModel::getSubject)
                .setHeader("Subject");
        grid
                .addColumn(CertificateModel::getValidFrom)
                .setHeader("Valid from");
        grid
                .addColumn(CertificateModel::getValidTo)
                .setHeader("Valid to");
        grid
                .addColumn((source) -> {
                    if (source.getRevocationDate() == null) {
                        return "no";
                    } else {
                        return source.getRevocationDate().toString();
                    }
                })
                .setHeader("Is Revoked");
        grid
                .addColumn(CertificateModel::getCertType)
                .setHeader("Type");

        grid
                .addComponentColumn((source) -> {
                    MenuBar menuBar = new MenuBar();
                    menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
                    MenuItem menuItem = menuBar.addItem(new Icon(VaadinIcon.CHEVRON_DOWN));
                    SubMenu actionsMenu = menuItem.getSubMenu();
                    actionsMenu.addItem("Revoke", (e) -> {
                        if (source.getRevocationDate() == null) {
                            source.setRevocationDate(new Date());
                            certificateReposttory.save(source);
                            grid.getDataProvider().refreshItem(source);
                            openVpnRestController.writeCrl();
                        }
                    });

                    return menuBar;
                });

        DataProvider<CertificateModel, Void> dataProvider = DataProvider.fromCallbacks(
                (query) -> {
                    Pageable pageable = PageRequest.of(
                            query.getOffset(),
                            query.getLimit()
                    );
                    var page = certificateReposttory.findAll(pageable);
                    return page.stream();
                },
                (query) -> (int) certificateReposttory.count()
        );
        grid.setDataProvider(dataProvider);

        add(grid);
    }
}

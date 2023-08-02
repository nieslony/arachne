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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
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

    public CertificatesView(CertificateRepository certificateReposttory) {
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
                .addColumn(CertificateModel::getIsRevoked)
                .setHeader("Is Revoked");
        grid
                .addColumn(CertificateModel::getCertType)
                .setHeader("Type");
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

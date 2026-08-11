/*
 * Copyright (C) 2026 claas
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
package at.nieslony.arachne.openvpn.management;

import at.nieslony.arachne.utils.LazyCreate;
import java.nio.file.Path;
import org.springframework.beans.factory.BeanFactory;

/**
 *
 * @author claas
 */
public class OpenVpnSiteManagementIf extends OpenVpnManagementIf {

    LazyCreate<Path> socketPath;

    public OpenVpnSiteManagementIf(BeanFactory beanFactory) {
        super(beanFactory);

        OpenVpnManagementService openVpnManagementService
                = beanFactory.getBean(OpenVpnManagementService.class);
        socketPath = new LazyCreate<>(
                () -> Path.of(openVpnManagementService.getSiteManagemnetSocket())
        );
    }

    @Override
    protected Path getSocketPath() {
        return socketPath.get();
    }

    @Override
    protected String getVpnTypeShort() {
        return "U";
    }

    @Override
    protected void onOpenvpnConnect() {
    }
}

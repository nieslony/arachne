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
package at.nieslony.arachne.openvpn.management;

import at.nieslony.arachne.utils.FolderFactory;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author claas
 */
@Service
public class OpenVpnManagementService {

    @Autowired
    private FolderFactory folderFactory;

    OpenVpnManagement userManagementIf;
    OpenVpnManagement siteManagementIf;

    @PostConstruct
    public void init() {
        userManagementIf = new OpenVpnManagement(Path.of(getUserManagemnetSocket()));
        siteManagementIf = new OpenVpnManagement(Path.of(getSiteManagemnetSocket()));
    }

    public String getSiteManagemnetSocket() {
        return "%s/openvpn-site-management.sock".formatted(
                folderFactory.getOpenVpnRunDir()
        );
    }

    public String getUserManagemnetSocket() {
        return "%s/openvpn-user-management.sock".formatted(
                folderFactory.getOpenVpnRunDir()
        );
    }

    public OpenVpnManagement getUserManagement() {
        return userManagementIf;
    }

    public OpenVpnManagement getSiteManagement() {
        return siteManagementIf;
    }
}

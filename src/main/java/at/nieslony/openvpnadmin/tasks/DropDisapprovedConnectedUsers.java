/*
 * Copyright (C) 2018 Claas Nieslony
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

package at.nieslony.openvpnadmin.tasks;

import at.nieslony.openvpnadmin.AbstractUser;
import at.nieslony.openvpnadmin.beans.LdapSettings;
import at.nieslony.openvpnadmin.beans.LocalUserFactory;
import at.nieslony.openvpnadmin.beans.ManagementInterface;
import at.nieslony.openvpnadmin.beans.Roles;
import at.nieslony.openvpnadmin.exceptions.ManagementInterfaceException;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapUser;
import at.nieslony.utils.classfinder.StaticMemberBean;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

/**
 *
 * @author claas
 */
@ScheduledTaskInfo(
        name = "Drop disapproved connected users",
        description = "Drop connected users who no longer have role user"
)
public class DropDisapprovedConnectedUsers
        implements ScheduledTask
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @StaticMemberBean
    private static ManagementInterface managementInterface;

    static public void setManagementInterface(ManagementInterface mi) {
        managementInterface = mi;
    }

    @StaticMemberBean
    private static LocalUserFactory localUserFactory;

    static public void setLocalUserFactory(LocalUserFactory luf) {
        localUserFactory = luf;
    }

    @StaticMemberBean
    private static LdapSettings ldapSettings;

    static public void setLdapSettings(LdapSettings ls) {
        ldapSettings = ls;
    }

    @StaticMemberBean
    private static Roles roles;

    static public void setRoles(Roles r) {
        roles = r;
    }

    @Override
    public void run() {
        List<ManagementInterface.UserStatus> connectedUsers = new LinkedList<>();

        if (managementInterface == null) {
            logger.severe("ManagementInterface == null");
            return;
        }

        try {
            managementInterface.getStatus(connectedUsers);
            if (connectedUsers.isEmpty()) {
                logger.info("There are no connected user, no user to drop");
            }
            for (ManagementInterface.UserStatus us : connectedUsers) {
                String username = us.getUser();

                AbstractUser user = null;
                user = localUserFactory.findUser(username);
                if (user == null) {
                    try {
                        user = ldapSettings.findVpnUser(username);
                    }
                    catch (NamingException | NoSuchLdapUser ex) {
                        logger.warning(String.format("Cannot find LDAP user: %s", ex.getMessage()));
                    }
                }
                if (user == null) {
                    logger.info(String.format("User %s is unknown, dropping from VPN.", username));
                    managementInterface.killUser(username);
                }
                else {
                    if (!roles.hasUserRole(user, "user")) {
                        logger.info(String.format(
                                "User %s has no longer role user, dropping from VPN.", username));
                        managementInterface.killUser(username);
                    }
                    else {
                        logger.info(String.format("User %s still has role user", username));
                    }
                }
            }
        }
        catch (IOException | ManagementInterfaceException | LoginException ex) {
            logger.warning(String.format("Cannot drop users: %s", ex.getMessage()));
        }
    }
}

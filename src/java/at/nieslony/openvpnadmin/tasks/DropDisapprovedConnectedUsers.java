/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.tasks;

import at.nieslony.openvpnadmin.AbstractUser;
import at.nieslony.openvpnadmin.beans.LdapSettings;
import at.nieslony.openvpnadmin.beans.LocalUserFactory;
import at.nieslony.openvpnadmin.beans.ManagementInterface;
import at.nieslony.openvpnadmin.beans.Roles;
import at.nieslony.openvpnadmin.exceptions.ManagementInterfaceException;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapUser;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.naming.NamingException;

/**
 *
 * @author claas
 */
@ScheduledTaskInfo(
        name = "Drop disapprioved connected users",
        description = "Drop connected users who no longer have role user"
)
public class DropDisapprovedConnectedUsers
        implements ScheduledTask
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ScheduledTaskMemberBean
    private static ManagementInterface managementInterface;

    static public void setManagementInterface(ManagementInterface mi) {
        managementInterface = mi;
    }

    @ScheduledTaskMemberBean
    private static LocalUserFactory localUserFactory;

    static public void setLocalUserFactory(LocalUserFactory luf) {
        localUserFactory = luf;
    }

    @ScheduledTaskMemberBean
    private static LdapSettings ldapSettings;

    static public void setLdapSettings(LdapSettings ls) {
        ldapSettings = ls;
    }

    @ScheduledTaskMemberBean
    private static Roles roles;

    static public void setRoles(Roles r) {
        roles = r;
    }

    @Override
    public void run() {
        List<ManagementInterface.UserStatus> connectedUsers = new LinkedList<>();

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
                    logger.info(String.format("User %s is unknown, dropping from VPN."));
                    managementInterface.killUser(username);
                }
                else {
                    if (!roles.hasUserRole(user, "user")) {
                        logger.info(String.format(
                                "User %s has no longer role user, dropping from VPN."));
                        managementInterface.killUser(username);
                    }
                    else {
                        logger.info("User %s still has role user");
                    }
                }
            }
        }
        catch (IOException | ManagementInterfaceException ex) {
            logger.warning(String.format("Cannot drop users: %s", ex.getMessage()));
        }
    }
}

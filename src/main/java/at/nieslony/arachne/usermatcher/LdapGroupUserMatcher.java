/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.usermatcher;

import at.nieslony.arachne.ldap.LdapGroup;
import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.ldap.LdapUserSource;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.users.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@UserMatcherDescription(
        description = "Member of LDAP Group",
        parameterLabel = "LDAP Group",
        ignoreInternalUser = true
)
public class LdapGroupUserMatcher extends UserMatcher {

    private static final Logger logger = LoggerFactory.getLogger(LdapGroupUserMatcher.class);

    public LdapGroupUserMatcher(String groupname) {
        super(groupname);
    }

    @Override
    public boolean isUserMatching(UserModel user) {
        logger.info("Try to match " + user.getUsername());
        Settings settings = Settings.getInstance();
        LdapSettings ldapSettings = settings.getSettings(LdapSettings.class);
        String userSourceName = user.getExternalProvider();
        if (userSourceName == null || !userSourceName.equals(LdapUserSource.getName())) {
            logger.info(
                    "User %s is not a LDAP user -> user doesn't match"
                            .formatted(user.getUsername()));
            return false;
        }
        if (!ldapSettings.isEnableLdapUserSource()) {
            logger.info("LDAP user source not enabled -> user does't match");
            return false;
        }
        try {
            LdapGroup ldapGroup = ldapSettings.getGroup(this.parameter);
            if (ldapGroup == null) {
                logger.info("Group %s not found".formatted(parameter));
                return false;
            }
            return ldapGroup.hasMember(user);
        } catch (Exception ex) {
            logger.error("Cannot connect to LDAP server: " + ex.getMessage());
        }
        return false;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.usermatcher;

import at.nieslony.arachne.ldap.LdapController;
import at.nieslony.arachne.ldap.LdapGroup;
import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.ldap.LdapUserSource;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.users.UserModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;

/**
 *
 * @author claas
 */
@UserMatcherDescription(
        description = "Member of LDAP Group",
        parameterLabel = "LDAP Group",
        ignoreInternalUser = true
)
@Slf4j
public class LdapGroupUserMatcher extends UserMatcher {

    private final LdapController ldapController;

    public LdapGroupUserMatcher(BeanFactory beanFactory, String groupname) {
        super(beanFactory, groupname);
        ldapController = beanFactory.getBean(LdapController.class);
    }

    @Override
    public boolean isUserMatching(UserModel user) {
        log.info("Try to match " + user.getUsername());
        Settings settings = Settings.getInstance();
        LdapSettings ldapSettings = settings.getSettings(LdapSettings.class);
        String userSourceName = user.getExternalProvider();
        if (userSourceName == null || !userSourceName.equals(LdapUserSource.getName())) {
            log.info(
                    "User %s is not a LDAP user -> user doesn't match"
                            .formatted(user.getUsername()));
            return false;
        }
        if (!ldapSettings.isEnableLdapUserSource()) {
            log.info("LDAP user source not enabled -> user does't match");
            return false;
        }
        try {
            LdapGroup ldapGroup = ldapController
                    .getGroup(this.parameter);
            if (ldapGroup == null) {
                log.info("Group %s not found".formatted(parameter));
                return false;
            }
            return ldapGroup.hasMember(user);
        } catch (Exception ex) {
            log.error("Cannot connect to LDAP server: " + ex.getMessage());
        }
        return false;
    }
}

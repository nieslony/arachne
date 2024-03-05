/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

import at.nieslony.arachne.ldap.LdapUserSource;
import at.nieslony.arachne.roles.RoleRuleModel;
import at.nieslony.arachne.roles.RolesCollector;
import at.nieslony.arachne.settings.Settings;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 *
 * @author claas
 */
@Service
public class ArachneUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(ArachneUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolesCollector rolesCollector;

    @Autowired
    private Settings settings;

    @Autowired
    private LdapUserSource ldapUserSource;

    public Set<ArachneUser> findAllUsersMatchRoleRule(RoleRuleModel roleRule) {

        return null;
    }

    public void ensureUpdated(ArachneUser user, int expirationTimeout) {
        if (user.isExpired(expirationTimeout)) {
            logger.info("User is expired, updating");
            String externalProvider = user.getExternalProvider();
            if (externalProvider != null) {
                if (user.getExternalProvider().equals(LdapUserSource.getName())) {
                    user.update(ldapUserSource.findUser(user.getUsername()));
                }
            }
            Set<String> roles = rolesCollector.findRolesForUser(user);
            user.setRoles(roles);
            userRepository.save(user);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        logger.info("Searching for user " + username);
        UserSettings userSettings = settings.getSettings(UserSettings.class);
        int ldapCacheMaxMins = userSettings.getExpirationTimeout();

        ArachneUser user = userRepository.findByUsername(username);
        if (user == null) {
            logger.info("User not found, try LDAP");
            user = ldapUserSource.findUser(username);
            if (user == null) {
                throw new UsernameNotFoundException("User %s not found".formatted(username));
            }
            Set<String> roles = rolesCollector.findRolesForUser(user);
            user.setRoles(roles);
            userRepository.save(user);
        } else {
            ensureUpdated(user, ldapCacheMaxMins);
        }

        logger.info("User %s has roles %s".formatted(
                username,
                user.getRoles().toString())
        );

        UserDetails userDetails = new ArachneUserDetails(user);
        return userDetails;
    }
}

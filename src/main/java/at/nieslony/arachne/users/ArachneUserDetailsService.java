/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.ldap.LdapUser;
import at.nieslony.arachne.roles.RolesCollector;
import at.nieslony.arachne.settings.Settings;
import com.vaadin.flow.server.VaadinSession;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Searching for user " + username);

        VaadinSession session = VaadinSession.getCurrent();

        ArachneUser user = userRepository.findByUsername(username);
        if (user != null) {
            Set<String> roles = rolesCollector.findRolesForUser(username);
            logger.info("User %s has roles %s".formatted(username, roles.toString()));

            UserDetails userDetails = new ArachneUserDetails(user, roles);

            return userDetails;
        }
        try {
            LdapSettings ldapSettings = new LdapSettings(settings);
            LdapTemplate ldap = ldapSettings.getLdapTemplate();

            LdapUser ldapUser = ldapSettings.getUser(username);
            logger.info("Found " + ldapUser.toString());

            Set<String> roles = rolesCollector.findRolesForUser(username);
            logger.info("User %s has roles %s".formatted(username, roles.toString()));

            UserDetails userDetails = new ArachneUserDetails(ldapUser, roles);
            return userDetails;
        } catch (Exception ex) {
            throw new UsernameNotFoundException(
                    "LDAP User %s not found".formatted(username),
                    ex);
        }
    }
}

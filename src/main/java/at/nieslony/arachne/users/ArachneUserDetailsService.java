/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.ldap.LdapUser;
import at.nieslony.arachne.ldap.LdapUserCacheModel;
import at.nieslony.arachne.ldap.LdapUserCacheRepository;
import at.nieslony.arachne.roles.RolesCollector;
import at.nieslony.arachne.settings.Settings;
import com.vaadin.flow.server.VaadinSession;
import java.util.Optional;
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
    private LdapUserCacheRepository ldapUserCacheRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Searching for user " + username);
        int ldapCacheMaxMins = 60;

        VaadinSession session = VaadinSession.getCurrent();

        ArachneUser user = userRepository.findByUsername(username);
        if (user != null) {
            Set<String> roles = rolesCollector.findRolesForUser(username, true);
            logger.info("Internal User %s has roles %s".formatted(username, roles.toString()));

            UserDetails userDetails = new ArachneUserDetails(user, roles);

            return userDetails;
        }
        try {
            LdapSettings ldapSettings = new LdapSettings(settings);
            LdapUserCacheModel lucm = null;

            if (ldapSettings.isCacheEnabled()) {
                Optional<LdapUserCacheModel> olucm = ldapUserCacheRepository.findByUsername(username);
                if (olucm.isPresent()) {
                    lucm = olucm.get();
                    logger.info(
                            "Found user %s in cache: %s"
                                    .formatted(username, lucm.toString())
                    );
                    if (!lucm.isExpired(ldapSettings.getCacheTimeOut())) {
                        logger.info("User %s is not expired".formatted(username));
                        UserDetails userDetails = new ArachneUserDetails(lucm);
                        return userDetails;
                    }
                } else {
                    logger.info("User %s is expired, update required".formatted(username));
                    lucm = new LdapUserCacheModel();
                }
            }

            LdapUser ldapUser = ldapSettings.getUser(username);
            logger.info("Found in LDAP " + ldapUser.toString());

            Set<String> roles = rolesCollector.findRolesForUser(username, false);
            logger.info("LDAP User %s has roles %s".formatted(username, roles.toString()));

            if (ldapSettings.isCacheEnabled() && lucm != null) {
                lucm.update(ldapUser, roles);
                ldapUserCacheRepository.save(lucm);
            }

            UserDetails userDetails = new ArachneUserDetails(ldapUser, roles);
            return userDetails;
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            throw new UsernameNotFoundException(
                    "LDAP User %s not found".formatted(username),
                    ex);
        }
    }
}

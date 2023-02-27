/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

import at.nieslony.arachne.roles.RolesCollector;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ArachneUser user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        Set<String> roles = rolesCollector.findRolesForUser(username);
        logger.info("User %s has roles %s".formatted(username, roles.toString()));

        UserDetails userDetails
                = User
                        .withUsername(user.getUsername())
                        .password(user.getPassword())
                        .roles(roles.toArray(new String[0]))
                        .build();
        return userDetails;
    }
}

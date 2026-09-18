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
package at.nieslony.arachne.users;

import at.nieslony.arachne.ldap.LdapUserSource;
import at.nieslony.arachne.roles.RolesCollector;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ArachneUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolesCollector rolesCollector;

    @Autowired
    private LdapUserSource ldapUserSource;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isEmpty()) {
            String msg = "No username supplied";
            log.error(msg);
            throw new UsernameNotFoundException(msg);
        }
        UserModel user;

        log.info("Try to find internal user \"%s\"".formatted(username));
        user = userRepository.findByUsername(username);
        if (user != null) {
            Set<String> roles = rolesCollector.findRolesForUser(user);
            user.setRoles(roles);
            user = userRepository.save(user);
            log.info("Found internal user %s".formatted(user.toString()));

            return new ArachneUserDetails(user);
        }

        log.info("Try to find LDAP user \"%s\"".formatted(username));
        user = ldapUserSource.findUser(username);
        if (user != null) {
            log.info("Found LDAP user " + user.toString());
            return new ArachneUserDetails(user);
        }

        String msg = "Userser %s not found".formatted(username);
        log.info(msg);
        throw new UsernameNotFoundException(msg);
    }

}

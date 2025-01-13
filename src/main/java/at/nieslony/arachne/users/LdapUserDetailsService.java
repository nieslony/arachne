/*
 * Copyright (C) 2024 claas
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
package at.nieslony.arachne.users;

import at.nieslony.arachne.ldap.LdapUserSource;
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
public class LdapUserDetailsService implements UserDetailsService {

    @Autowired
    private LdapUserSource ldapUserSource;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isEmpty()) {
            String msg = "No username supplied";
            log.error(msg);
            throw new UsernameNotFoundException(msg);
        }
        log.info("Try to find LDAP user \"%s\"".formatted(username));
        UserModel user = ldapUserSource.findUser(username);
        if (user == null) {
            String msg = "LDAP user %s not found".formatted(username);
            log.info(msg);
            throw new UsernameNotFoundException(msg);
        }

        log.info("Found user " + user.toString());
        return new ArachneUserDetails(user);
    }
}

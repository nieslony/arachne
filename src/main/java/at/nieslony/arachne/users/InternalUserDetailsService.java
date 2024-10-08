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

import at.nieslony.arachne.roles.RolesCollector;
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
public class InternalUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(
            InternalUserDetailsService.class
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolesCollector rolesCollector;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isEmpty()) {
            String msg = "No username supplied";
            logger.error(msg);
            throw new UsernameNotFoundException(msg);
        }
        logger.info("Try to find internal user \"%s\"".formatted(username));
        UserModel user = userRepository.findByUsername(username);
        if (user == null) {
            String msg = "Internal user %s not found".formatted(username);
            logger.info(msg);
            throw new UsernameNotFoundException(msg);
        }
        Set<String> roles = rolesCollector.findRolesForUser(user);
        user.setRoles(roles);
        user = userRepository.save(user);
        logger.info("Found internal user %s".formatted(user.toString()));

        return new ArachneUserDetails(user);
    }
}

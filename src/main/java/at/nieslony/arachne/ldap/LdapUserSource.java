/*
 * Copyright (C) 2023 claas
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
package at.nieslony.arachne.ldap;

import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.users.ExternalUserSource;
import at.nieslony.arachne.users.UserModel;
import at.nieslony.arachne.users.UserRepository;
import at.nieslony.arachne.users.UserSettings;
import java.security.SecureRandom;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
public class LdapUserSource implements ExternalUserSource {

    private static final Logger logger = LoggerFactory.getLogger(LdapUserSource.class);

    @Autowired
    private Settings settings;

    @Autowired
    UserRepository userRepository;

    static public String getName() {
        return "Ldap";
    }

    @Override
    public UserModel findUser(String username) {
        LdapSettings ldapSettings = settings.getSettings(LdapSettings.class);
        UserSettings userSettings = settings.getSettings(UserSettings.class);
        int ldapCacheMaxMins = userSettings.getExpirationTimeout();

        UserModel user = userRepository.findByUsernameAndExternalProvider(
                username,
                getName()
        );
        if (user == null) {
            logger.info("User %s not found in database, getting from LDAP"
                    .formatted(username)
            );
            user = ldapSettings.getUser(username);
        }
        if (user == null) {
            logger.info("User %s neither found in database not LDAP"
                    .formatted(username)
            );
            return null;
        }
        user.setExternalProvider(getName());
        user.setPassword(createRandomPassword());
        if (user.isExpired(ldapCacheMaxMins)) {
            update(user);
        }

        return user;
    }

    private String createRandomPassword() {
        return new SecureRandom()
                .ints(32, 127)
                .filter(i -> Character.isLetterOrDigit(i))
                .limit(64)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    @Override
    public List<UserModel> findMatchingUsers(String userPattern) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void update(UserModel user) {
        String username = user.getUsername();
        if (!user.getExternalProvider().equals(getName())) {
            logger.info("I'm  not a %s external provider. I'm %s"
                    .formatted(user.getExternalProvider(), getName())
            );
            return;
        }

        UserModel oldUser = userRepository.findByUsernameAndExternalProvider(
                username,
                getName()
        );
        if (oldUser == null) {
            logger.info("Creating new user " + username);
            userRepository.save(user);
        } else {
            UserSettings userSettings = settings.getSettings(UserSettings.class);
            int ldapCacheMaxMins = userSettings.getExpirationTimeout();
            if (user.isExpired(ldapCacheMaxMins)) {
                logger.info("User %s is expired, updating".formatted(user.getUsername()));
                oldUser.update(user);
                logger.info("Saving user " + username);
                userRepository.save(oldUser);
            } else {
                logger.info("User %s is not expired, no update"
                        .formatted(user.getUsername())
                );
            }
        }
    }
}

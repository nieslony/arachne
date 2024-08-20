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
import at.nieslony.arachne.users.UserModel;
import at.nieslony.arachne.users.ExternalUserSource;
import at.nieslony.arachne.users.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.CommunicationException;
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
        UserModel user = null;
        try {
            user = ldapSettings.getUser(username);
        } catch (CommunicationException ex) {
            logger.error("Cannot connect to LDAP server: " + ex.getMessage());
        }
        if (user == null) {
            return null;
        }
        user.setPassword("");
        return user;
    }

    @Override
    public List<UserModel> findMatchingUsers(String userPattern) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void update(UserModel user) {
        if (user.getExternalProvider().equals(getName())) {
            return;
        }

        UserModel oldUser = userRepository.findByUsername(user.getUsername());
        if (oldUser.getExternalProvider().equals(getName())) {
            return;
        }

        oldUser.setDisplayName(user.getDisplayName());
        oldUser.setEmail(user.getEmail());
        userRepository.save(oldUser);
    }
}

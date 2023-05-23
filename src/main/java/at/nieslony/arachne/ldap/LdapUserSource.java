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
import at.nieslony.arachne.users.ArachneUser;
import at.nieslony.arachne.users.ExternalUserSource;
import at.nieslony.arachne.users.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
public class LdapUserSource implements ExternalUserSource {

    @Autowired
    private Settings settings;

    @Autowired
    UserRepository userRepository;

    static public String getName() {
        return "Ldap";
    }

    @Override
    public ArachneUser findUser(String username) {
        LdapSettings ldapSettings = new LdapSettings(settings);
        ArachneUser user = ldapSettings.getUser(username);
        user.setPassword("");
        return user;
    }

    @Override
    public List<ArachneUser> findMatchingUsers(String userPattern) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void update(ArachneUser user) {
        if (user.getExternalProvider().equals(getName())) {
            return;
        }

        ArachneUser oldUser = userRepository.findByUsername(user.getUsername());
        if (oldUser.getExternalProvider().equals(getName())) {
            return;
        }

        oldUser.setDisplayName(user.getDisplayName());
        oldUser.setEmail(user.getEmail());
        userRepository.save(oldUser);
    }
}

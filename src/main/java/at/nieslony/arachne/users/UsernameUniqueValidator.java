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
package at.nieslony.arachne.users;

import com.vaadin.flow.function.SerializablePredicate;

/**
 *
 * @author claas
 */
public class UsernameUniqueValidator implements SerializablePredicate<String> {

    private Long userId = null;
    private UserRepository userRepository;

    public UsernameUniqueValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @Override
    public boolean test(String username) {
        ArachneUser user = userRepository.findByUsername(username);
        if (userId == null) {
            if (user != null) {
                return false;
            }
        } else {
            if (user != null && user.getId() != this.userId) {
                return false;
            }
        }

        return true;
    }

    public static String getErrorMsg() {
        return "Username is not unique";
    }
}

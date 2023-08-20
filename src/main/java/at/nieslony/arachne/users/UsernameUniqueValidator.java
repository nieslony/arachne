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

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;

/**
 *
 * @author claas
 */
public class UsernameUniqueValidator implements Validator<String> {

    private final UserRepository userRepository;

    public UsernameUniqueValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public ValidationResult apply(String username, ValueContext vc) {
        ArachneUser user = userRepository.findByUsername(username);
        if (user != null) {
            return ValidationResult.error("User already exists");
        } else {
            return ValidationResult.ok();
        }
    }
}

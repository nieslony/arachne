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
public class UsernameValidator implements Validator<String> {

    @Override
    public ValidationResult apply(String username, ValueContext vc) {
        if (!username.matches("^[a-z].*$")) {
            return ValidationResult.error("Username does not start with letter");
        }
        if (!username.matches("^[a-z0-9_.\\-]+$")) {
            return ValidationResult.error("Allowed characters: a-z, 0-9, ._-");
        }

        return ValidationResult.ok();
    }
}

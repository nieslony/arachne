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
public class UsernameValidator implements SerializablePredicate<String> {

    private static final String msg = "Must start with lowercase letter, followed by a-z 0-9 - . -";

    @Override
    public boolean test(String username) {
        if (!username.matches("^[a-z].*$")) {
            return false;
        }
        if (!username.matches("^[a-z0-9_.\\-]+$")) {
            return false;
        }

        return true;
    }

    public static String getErrorMsg() {
        return msg;
    }
}

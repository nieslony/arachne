/*
 * Copyright (C) 2025 claas
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
package at.nieslony.arachne.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author claas
 */
public class SystemUsers {

    public record User(
            String username,
            int userId,
            int primaryGroup,
            String gecos,
            String homeDir,
            String shell
            ) {

    }

    private static final Map<String, User> users = new HashMap<>();

    static private void loadUsers() {
        users.clear();

        Path path = Path.of("/etc/passwd");
        try {
            Files.lines(path)
                    .forEach((line) -> {
                        String[] fields = line.split(":");
                        users.put(fields[0], new User(
                                fields[0],
                                Integer.parseInt(fields[2]),
                                Integer.parseInt(fields[3]),
                                fields[4],
                                fields[5],
                                fields[6]
                        ));
                    });
        } catch (IOException ex) {

        }
    }

    public static User getUser(String userName) {
        if (users.isEmpty()) {
            loadUsers();
        }
        return users.get(userName);
    }
}

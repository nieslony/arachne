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
package at.nieslony.arachne.pki;

import at.nieslony.arachne.settings.Settings;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
public class PkiSettings {

    private final static String SK_DH_PARAMS = "pki.dhParams";

    private String dhParams;

    public PkiSettings() {
    }

    public PkiSettings(Settings settings) {
        dhParams = settings.get(SK_DH_PARAMS, "");
    }

    public void save(Settings settings) {
        settings.put(SK_DH_PARAMS, dhParams);
    }
}

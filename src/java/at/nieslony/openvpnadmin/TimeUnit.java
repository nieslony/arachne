/*
 * Copyright (C) 2018 Claas Nieslony
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

package at.nieslony.openvpnadmin;

/**
 *
 * @author claas
 */
public enum TimeUnit {
    SEC("secs",    1000),
    MIN("mins",    1000 * 60),
    HOUR("hours",  1000 * 60 * 60),
    DAY("days",    1000 * 60 * 60 * 24),
    WEEK("weeks",  1000 * 60 * 60 * 7),
    MONTH("month", 1000 * 60 * 60 * 30),
    YEAR("years",  1000 * 60 * 60 * 365);

    final private String timeUnitName;
    long value;

    private TimeUnit(String name, long value) {
        timeUnitName = name;
        this.value = value;
    }

    public String getUnitName() {
        return timeUnitName;
    }

    public long getValue() {
        return value;
    }
}

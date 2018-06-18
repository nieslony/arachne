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
abstract public class AbstractUser {
    private String username;
    private String givenName;
    private String surName;
    private String fullName;
    private String email;

    public String getUsername() {
        return username;
    }

    public String getGivenName() {
        return givenName == null ? "" : givenName;
    }

    public String getSurName() {
        return surName == null ? "" : surName;
    }

    public String getFullName() {
        return fullName == null ? "" : fullName;
    }

    public String getEmail() {
        return email == null ? "" : email;
    }

    public void setUsername(String un) {
        username = un == null ? "" : un;
    }

    public void setGivenName(String gn) {
        givenName = gn == null ? "" : gn;
    }

    public void setFullName(String fn) {
        fullName = fn == null ? "" : fn;
    }

    public void setEmail(String em) {
        email = em == null ? "" : em;
    }

    public void setSurName(String sn) {
        surName = sn == null ? "" : sn;
    }

    public void setPassword(String pwd) {
    }

    abstract public boolean isImmutable();
    abstract public boolean auth(String password);
    abstract public String getUserTypeStr();

    public void save()
            throws Exception
    {
    }

}

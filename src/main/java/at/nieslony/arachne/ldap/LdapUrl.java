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

import at.nieslony.arachne.apiindex.ShowApiType;
import at.nieslony.arachne.utils.net.MutableUrl;
import at.nieslony.arachne.utils.net.UrlParseException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ShowApiType(String.class)
@Slf4j
public class LdapUrl extends MutableUrl {

    static final private SchemaDescr[] LDAP_SCHEMATA = {
        new SchemaDescr("ldap", 389),
        new SchemaDescr("ldaps", 636)
    };

    public LdapUrl() {
        super(LDAP_SCHEMATA);
    }

    public LdapUrl(String url) throws UrlParseException {
        super(LDAP_SCHEMATA, url);
    }

    public LdapUrl(String protocol, String host, int port) throws UrlParseException {
        super(LDAP_SCHEMATA, protocol, host, port);
    }
}

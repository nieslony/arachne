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
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Getter
@Setter
@EqualsAndHashCode
@ShowApiType(String.class)
public class LdapUrl implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(LdapUrl.class);

    private LdapProtocol protocol;
    private String host;
    private int port;

    public LdapUrl(String url) {
        Pattern pattetn = Pattern.compile(
                "(ldap|ldaps)://([a-z0-9][a-z0-9.\\-]*):([0-9]+)"
        );
        Matcher matcher = pattetn.matcher(url);
        matcher.find();
        protocol = LdapProtocol.valueOf(matcher.group(1).toUpperCase());
        host = matcher.group(2);
        port = Integer.parseInt(matcher.group(3));
    }

    public LdapUrl(LdapProtocol protocol, String host, int port) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

    @Override
    public String toString() {
        return "%s://%s:%d".formatted(protocol.toString(), host, port);
    }
}

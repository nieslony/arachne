/*
 * Copyright (C) 2026 claas
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
package at.nieslony.arachne;

import at.nieslony.arachne.ldap.LdapUrl;
import at.nieslony.arachne.utils.net.UrlParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author claas
 */
public class MutableUrlTest {

    @Test
    public void testParseUrl() {
        String url = "ldap://example.com:123";
        Assertions.assertDoesNotThrow(() -> {
            var up = new LdapUrl(url);
        });
        LdapUrl up;
        try {
            up = new LdapUrl(url);
        } catch (UrlParseException ex) {
            Assertions.fail();
            return;
        }
        Assertions.assertEquals("ldap", up.getSchema());
        Assertions.assertEquals("example.com", up.getHost());
        Assertions.assertEquals(123, up.getPort());
    }

    @Test
    public void testToString() {
        String url = "ldap://example.com:123";
        LdapUrl up;
        try {
            up = new LdapUrl(url);
        } catch (UrlParseException ex) {
            Assertions.fail();
            return;
        }
        Assertions.assertEquals(url, up.toString());
    }

    @Test
    public void testSetValues() {
        String url = "ldap://example.com:123";
        LdapUrl up;
        try {
            up = new LdapUrl(url);
            up.setSchema("ldap");
        } catch (UrlParseException ex) {
            Assertions.fail();
            return;
        }

        Assertions.assertEquals("ldap://example.com:123", up.toString());

        up.setHost("www.example.com");
        Assertions.assertEquals("ldap://www.example.com:123", up.toString());

        up.setPort(443);
        Assertions.assertEquals("ldap://www.example.com:443", up.toString());

        Assertions.assertThrows(
                UrlParseException.class,
                () -> up.setSchema("http")
        );
    }
}

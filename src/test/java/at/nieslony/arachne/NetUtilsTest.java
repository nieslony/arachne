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
package at.nieslony.arachne;

import at.nieslony.arachne.utils.net.NetUtils;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author claas
 */
public class NetUtilsTest {

    @Test
    public void testMaskLen2Mask() {
        Assertions.assertEquals("255.255.255.255", NetUtils.maskLen2Mask(32));
        Assertions.assertEquals("255.255.255.0", NetUtils.maskLen2Mask(24));
        Assertions.assertEquals("255.255.0.0", NetUtils.maskLen2Mask(16));
    }

    @Test
    public void testIsSubnetOf()
            throws NumberFormatException, UnknownHostException {
        Assertions.assertTrue(
                NetUtils.isSubnetOf("192.168.100.1", "192.168.100.0/24")
        );
        Assertions.assertFalse(
                NetUtils.isSubnetOf("192.168.101.1", "192.168.100.0/24")
        );
        Assertions.assertTrue(
                NetUtils.isSubnetOf("192.168.100.0/24", "192.168.0.0/16")
        );
        Assertions.assertFalse(
                NetUtils.isSubnetOf("192.168.0.0/16", "192.168.100.0/24")
        );
        Assertions.assertThrows(UnknownHostException.class, () -> {
            NetUtils.isSubnetOf("192.168.100.1", "192.1682100.0/24");
        });
        Assertions.assertThrows(NumberFormatException.class, () -> {
            NetUtils.isSubnetOf("192.168.100.1", "192.1682100.0/notamask");
        });
    }

    @Test
    public void testFilterSubnets() {
        Assertions.assertIterableEquals(
                List.of(
                        "192.168.100.1",
                        "192.168.100.2"
                ),
                NetUtils.filterSubnets(List.of(
                        "192.168.100.1",
                        "192.168.100.2"
                ))
        );
        Assertions.assertIterableEquals(
                List.of(
                        "192.168.100.0/24"
                ),
                NetUtils.filterSubnets(List.of(
                        "192.168.100.1",
                        "192.168.100.0/24"
                ))
        );
        Assertions.assertIterableEquals(
                List.of(
                        "192.168.0.0/16"
                ),
                NetUtils.filterSubnets(List.of(
                        "192.168.0.0/16",
                        "192.168.100.0/24"
                ))
        );
        Assertions.assertIterableEquals(
                List.of(
                        "192.168.100.0/24",
                        "192.168.110.0/24"
                ),
                NetUtils.filterSubnets(List.of(
                        "192.168.100.0/24",
                        "192.168.110.0/24"
                ))
        );
        Assertions.assertIterableEquals(
                List.of(
                        "192.168.100.0/24"
                ),
                NetUtils.filterSubnets(List.of(
                        "192.168.100.2",
                        "192.168.100.0/24",
                        "192.168.100.1"
                ))
        );
    }
}

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
package at.nieslony.arachne.utils;

import lombok.Getter;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Getter
@ToString
public class SrvRecord {

    final private int port;
    final private String hostname;
    final private int priority;
    final private int weight;

    public SrvRecord(String dnsEntry) {
        // _service._proto.name. ttl IN SRV priority weight port target.
        // _sip._tcp.example.com. 86400 IN SRV 0 5 5060 sipserver.example.com.
        String[] values = dnsEntry.split(" ");
        priority = Integer.parseInt(values[4]);
        weight = Integer.parseInt(values[5]);
        port = Integer.parseInt(values[6]);
        hostname = values[7];
    }
}

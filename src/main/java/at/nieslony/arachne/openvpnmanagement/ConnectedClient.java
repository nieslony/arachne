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
package at.nieslony.arachne.openvpnmanagement;

import java.util.Date;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Getter
@ToString
public class ConnectedClient {

    private static final Logger logger = LoggerFactory.getLogger(ConnectedClient.class);

    public ConnectedClient(String statusLine) {
        logger.info(
                "Initializing: >>%s<<"
                        .formatted(statusLine.replaceAll("\t", "|"))
        );
        String[] splitStatusLine = statusLine.split("\t");
        int i = 1;
        // CLIENT_LIST|claas@NIESLONY.LAN|172.24.71.152:54062|192.168.100.2||3570|3179|2023-07-22 22:24:49|1690057489|claas|1|0|AES-256-GCM
        commonName = splitStatusLine[i++];
        realAddress = splitStatusLine[i++];
        virtualAddress = splitStatusLine[i++];
        virtualV6Address = splitStatusLine[i++];
        bytesReceived = Long.parseLong(splitStatusLine[i++]);
        bytesSent = Long.parseLong(splitStatusLine[i++]);
        i++;
        connectedSince = new Date(
                Long.parseLong(splitStatusLine[i++]) * 1000
        );
        username = splitStatusLine[i++];
        clientId = Integer.parseInt(splitStatusLine[i++]);
        peerId = Integer.parseInt(splitStatusLine[i++]);
        dataChannelCipher = splitStatusLine[i++];
    }

    private final String commonName;
    private final String realAddress;
    private final String virtualAddress;
    private final String virtualV6Address;
    private final long bytesReceived;
    private final long bytesSent;
    private final Date connectedSince;
    private final String username;
    private final int clientId;
    private final int peerId;
    private final String dataChannelCipher;
}

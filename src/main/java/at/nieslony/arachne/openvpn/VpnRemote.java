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
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.utils.net.TransportProtocol;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author claas
 */
@Getter
@Setter
public class VpnRemote {

    public VpnRemote() {
    }

    public VpnRemote(String remoteHost, int port, TransportProtocol transportProtocol) {
        this.remoteHost = remoteHost;
        this.port = port;
        this.transportProtocol = transportProtocol;
    }

    private String remoteHost = "";
    private int port = 1;
    private TransportProtocol transportProtocol = TransportProtocol.TCP;

    @Override
    public String toString() {
        return "%s:%d/%s".formatted(remoteHost, port, transportProtocol.toString());
    }
}

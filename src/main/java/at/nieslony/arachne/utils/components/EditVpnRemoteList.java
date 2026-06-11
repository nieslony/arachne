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
package at.nieslony.arachne.utils.components;

import at.nieslony.arachne.openvpn.VpnRemote;
import at.nieslony.arachne.utils.net.TransportProtocol;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Slf4j
public class EditVpnRemoteList extends GenericEditableListBox<VpnRemote, EditVpnRemote> {

    EditVpnRemote editVpnRemote;
    List<TransportProtocol> allowedProtocols;

    public EditVpnRemoteList(String label) {
        super(label, new EditVpnRemote());
        editVpnRemote = (EditVpnRemote) editField;
        editVpnRemote.setValueChangeMode(ValueChangeMode.EAGER);
        itemsField.setItemLabelGenerator((item) -> {
            if (allowedProtocols.size() > 1) {
                return "%s:%d/%s".formatted(
                        item.getRemoteHost(),
                        item.getPort(),
                        item.getTransportProtocol().toString()
                );
            } else {
                return "%s:%d".formatted(
                        item.getRemoteHost(),
                        item.getPort()
                );
            }
        });
        itemsField.setItemEnabledProvider((item) -> {
            return allowedProtocols.contains(item.getTransportProtocol());
        });
    }

    public void setAllowedProtocols(List<TransportProtocol> allowedProtocols) {
        this.allowedProtocols = allowedProtocols;
        editVpnRemote.setAllowedProtocols(allowedProtocols);
    }
}

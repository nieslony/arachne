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
import at.nieslony.arachne.openvpn.vpnsite.RemoteNetwork;
import at.nieslony.arachne.utils.net.TransportProtocol;
import at.nieslony.arachne.utils.validators.HostnameValidator;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.HasValidator;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Slf4j
public class EditVpnRemote extends AbstractCompositeField<HorizontalLayout, EditVpnRemote, VpnRemote>
        implements HasValidator<RemoteNetwork>, HasSize {

    Binder<VpnRemote> binder = new Binder<>();

    public EditVpnRemote() {
        super(new VpnRemote());

        TextField remoteHostField = new TextField("Remote Host");
        remoteHostField.setClearButtonVisible(true);

        IntegerField portField = new IntegerField("Port");
        portField.setMin(1);
        portField.setMax(65535);
        portField.setStepButtonsVisible(true);
        portField.setMaxWidth(10, Unit.REM);

        Select<TransportProtocol> protocolField = new Select<>();
        protocolField.setLabel("Protocol");
        protocolField.setItems(TransportProtocol.values());
        protocolField.setMaxWidth(6, Unit.REM);

        binder.forField(remoteHostField)
                .asRequired()
                .withValidator(new HostnameValidator()
                        .withEmptyAllowed(false)
                        .withIpAllowed(true)
                        .withResolvableRequired(true)
                )
                .bind(VpnRemote::getRemoteHost, VpnRemote::setRemoteHost);
        binder.forField(portField)
                .asRequired()
                .bind(VpnRemote::getPort, VpnRemote::setPort);
        binder.forField(protocolField)
                .asRequired()
                .bind(VpnRemote::getTransportProtocol, VpnRemote::setTransportProtocol);

        getContent().add(
                remoteHostField,
                new Text(":"),
                portField,
                new Text("/"),
                protocolField
        );
        getContent().setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        getContent().setFlexGrow(0, portField, protocolField);
        getContent().setFlexGrow(1, remoteHostField);
        getContent().setPadding(false);
    }

    @Override
    protected void setPresentationValue(VpnRemote vpnRemote) {
        binder.setBean(vpnRemote);
        binder.validate();
    }
}

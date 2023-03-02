/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.settings.SettingsRepository;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.security.RolesAllowed;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "openvpn_user", layout = ViewTemplate.class)
@PageTitle("OpenVPN User | Arachne")
@RolesAllowed("ADMIN")
public class OpenVpnUserView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnUserView.class);

    @Data
    public class NicInfo {

        private String ipAddress;
        private String nicName;

        public NicInfo(String ipAddress, String nicName) {
            this.ipAddress = ipAddress;
            this.nicName = nicName;
        }

        public NicInfo() {
        }

        @Override
        public String toString() {
            return "%s - %s".formatted(ipAddress, nicName);
        }
    }

    @Data
    public class NetMask {

        private int bits;
        private String mask;

        public NetMask() {
        }

        public NetMask(int bits) {
            this.bits = bits;
            this.mask = OpenVpnUserSettings.bits2Subnetmask(bits);
        }

        @Override
        public String toString() {
            return "%d - %s".formatted(bits, mask);
        }
    }

    private SettingsRepository settingsRepository;

    public OpenVpnUserView(
            SettingsRepository settingsRepository,
            OpenVpnRestController openvpnRestController,
            Pki pki
    ) {
        this.settingsRepository = settingsRepository;

        TextField name = new TextField("Network Manager configuration name");
        name.setWidth(50, Unit.EM);

        Select<NicInfo> ipAddresse = new Select<>();
        ipAddresse.setItems(findAllNics());
        ipAddresse.setLabel("Listen on");
        ipAddresse.setWidth(20, Unit.EM);

        IntegerField port = new IntegerField("Port");
        port.setMin(1);
        port.setMax(65534);
        port.setStepButtonsVisible(true);

        Select<String> protocol = new Select<>();
        protocol.setItems("TCP", "UDP");
        protocol.setLabel("Protocol");

        TextField connectToHost = new TextField("Connect to host");

        Select<String> interfaceType = new Select<>();
        interfaceType.setItems("tun", "tap");
        interfaceType.setLabel("Interface Type");

        TextField interfaceName = new TextField("Interface Name");

        TextField clientNetwork = new TextField("Client Network");

        Select<NetMask> clientMask = new Select<>();
        clientMask.setItems(
                IntStream
                        .range(1, 32)
                        .boxed()
                        .map(i -> new NetMask(i))
                        .collect(Collectors.toList())
        );
        clientMask.setLabel("Subnet Mask");

        IntegerField keepaliveInterval = new IntegerField("Keepalive Interval");
        Div suffix;
        suffix = new Div();
        suffix.setText("seconds");
        keepaliveInterval.setSuffixComponent(suffix);
        keepaliveInterval.setMin(1);
        keepaliveInterval.setStepButtonsVisible(true);
        keepaliveInterval.setWidth(12, Unit.EM);

        IntegerField keepaliveTimeout = new IntegerField("Keepalive timeout");
        suffix = new Div();
        suffix.setText("seconds");
        keepaliveTimeout.setSuffixComponent(suffix);
        keepaliveTimeout.setMin(1);
        keepaliveTimeout.setStepButtonsVisible(true);
        keepaliveTimeout.setWidth(12, Unit.EM);

        Button saveSettings = new Button("Save Settings");

        Binder<OpenVpnUserSettings> binder = new Binder(OpenVpnUserSettings.class);
        binder.forField(name)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getVpnName, OpenVpnUserSettings::setVpnName);
        binder.forField(ipAddresse)
                .asRequired("Value required")
                .bind(
                        (s) -> {
                            return findNicByIp(s.getListenIp());
                        },
                        (s, v) -> {
                            s.setListenIp(v.getIpAddress());
                        });
        binder.forField(port)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getListenPort, OpenVpnUserSettings::setListenPort);
        binder.forField(protocol)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getListenProtocol, OpenVpnUserSettings::setListenProtocol);
        binder.forField(connectToHost)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getRemote, OpenVpnUserSettings::setRemote);
        binder.forField(interfaceType)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getDeviceType, OpenVpnUserSettings::setDeviceType);
        binder.forField(interfaceName)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getDeviceName, OpenVpnUserSettings::setDeviceName);
        binder.forField(clientNetwork)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getClientNetwork, OpenVpnUserSettings::setClientNetwork);
        binder.forField(clientMask)
                .asRequired("Value required")
                .bind(
                        (source) -> {
                            int mask = source.getClientMask();
                            return new NetMask(mask);
                        },
                        (s, v) -> {
                            s.setClientMask(v.getBits());
                        }
                );
        binder.forField(keepaliveInterval)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getKeepaliveInterval, OpenVpnUserSettings::setKeepaliveInterval);
        binder.forField(keepaliveTimeout)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getKeepaliveTimeout, OpenVpnUserSettings::setKeepaliveTimeout);
        OpenVpnUserSettings settings = new OpenVpnUserSettings(settingsRepository);
        binder.setBean(new OpenVpnUserSettings(settingsRepository));

        binder.addStatusChangeListener((sce) -> {
            saveSettings.setEnabled(!sce.hasValidationErrors());
        });

        saveSettings.addClickListener((t) -> {
            if (binder.writeBeanIfValid(settings)) {
                settings.save(settingsRepository);
                openvpnRestController.writeOpenVpnUserServerConfig(settings);
            }
        });

        binder.validate();

        add(
                new HorizontalLayout(
                        ipAddresse,
                        port,
                        protocol
                ),
                connectToHost,
                new HorizontalLayout(
                        interfaceType,
                        interfaceName
                ),
                new HorizontalLayout(
                        clientNetwork,
                        clientMask
                ),
                new HorizontalLayout(
                        keepaliveInterval,
                        keepaliveTimeout
                ),
                saveSettings
        );

    }

    public List<NicInfo> findAllNics() {
        List<NicInfo> allNics = new LinkedList<>();
        allNics.add(new NicInfo("0.0.0.0", "All Interfaces"));
        Enumeration<NetworkInterface> foundNics;
        try {
            foundNics = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            logger.error("Cannot retrieve list of NICs: " + ex.getMessage());
            return null;
        }
        for (NetworkInterface nic : Collections
                .list(foundNics)) {
            for (InetAddress inetAddress : Collections.list(nic.getInetAddresses())) {
                if (inetAddress instanceof Inet4Address) {
                    allNics.add(new NicInfo(
                            inetAddress.getHostAddress(),
                            nic.getName()));
                }
            }
        }

        return allNics;
    }

    NicInfo findNicByIp(String ipAddress) {
        return findAllNics()
                .stream()
                .filter((t) -> t.ipAddress.equals(ipAddress))
                .findFirst()
                .get();
    }
}

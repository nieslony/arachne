/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.settings.Settings;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
            this.mask = NetUtils.maskLen2Mask(bits);
        }

        @Override
        public String toString() {
            return "%d - %s".formatted(bits, mask);
        }
    }

    private Settings settings;

    public OpenVpnUserView(
            Settings settings,
            OpenVpnRestController openvpnRestController,
            Pki pki
    ) {
        this.settings = settings;
        OpenVpnUserSettings vpnSettings = new OpenVpnUserSettings(settings);

        TextField name = new TextField("Network Manager configuration name");
        name.setWidth(50, Unit.EM);
        name.setValueChangeMode(ValueChangeMode.EAGER);

        Select<NicInfo> ipAddresse = new Select<>();
        ipAddresse.setItems(findAllNics());
        ipAddresse.setLabel("Listen on");
        ipAddresse.setWidth(20, Unit.EM);

        IntegerField port = new IntegerField("Port");
        port.setMin(1);
        port.setMax(65534);
        port.setStepButtonsVisible(true);
        port.setValueChangeMode(ValueChangeMode.EAGER);
        port.setWidth(8, Unit.EM);

        Select<String> protocol = new Select<>();
        protocol.setItems("TCP", "UDP");
        protocol.setLabel("Protocol");
        protocol.setWidth(8, Unit.EM);

        HorizontalLayout listenLayout = new HorizontalLayout();
        listenLayout.add(ipAddresse, port, protocol);
        listenLayout.setFlexGrow(1, ipAddresse);

        TextField connectToHost = new TextField("Connect to host");
        connectToHost.setValueChangeMode(ValueChangeMode.EAGER);

        Select<String> interfaceType = new Select<>();
        interfaceType.setItems("tun", "tap");
        interfaceType.setLabel("Interface Type");
        interfaceType.setWidth(8, Unit.EM);

        TextField interfaceName = new TextField("Interface Name");
        interfaceName.setValueChangeMode(ValueChangeMode.EAGER);

        HorizontalLayout interfaceLayout = new HorizontalLayout();
        interfaceLayout.add(interfaceType, interfaceName);
        interfaceLayout.setFlexGrow(1, interfaceName);

        TextField clientNetwork = new TextField("Client Network");
        clientNetwork.setValueChangeMode(ValueChangeMode.EAGER);

        Select<NetMask> clientMask = new Select<>();
        clientMask.setItems(
                IntStream
                        .range(1, 32)
                        .boxed()
                        .map(i -> new NetMask(i))
                        .collect(Collectors.toList())
        );
        clientMask.setLabel("Subnet Mask");

        HorizontalLayout clientNetLayout = new HorizontalLayout();
        clientNetLayout.add(clientNetwork, clientMask);
        clientNetLayout.setFlexGrow(1, clientNetwork, clientMask);

        IntegerField keepaliveInterval = new IntegerField("Keepalive Interval");
        Div suffix;
        suffix = new Div();
        suffix.setText("seconds");
        keepaliveInterval.setSuffixComponent(suffix);
        keepaliveInterval.setMin(1);
        keepaliveInterval.setStepButtonsVisible(true);
        keepaliveInterval.setWidth(12, Unit.EM);
        keepaliveInterval.setValueChangeMode(ValueChangeMode.EAGER);

        IntegerField keepaliveTimeout = new IntegerField("Keepalive timeout");
        suffix = new Div();
        suffix.setText("seconds");
        keepaliveTimeout.setSuffixComponent(suffix);
        keepaliveTimeout.setMin(1);
        keepaliveTimeout.setStepButtonsVisible(true);
        keepaliveTimeout.setWidth(12, Unit.EM);
        keepaliveInterval.setValueChangeMode(ValueChangeMode.EAGER);

        HorizontalLayout keepaliveLayout = new HorizontalLayout();
        keepaliveLayout.add(keepaliveInterval, keepaliveTimeout);

        ListBox<String> pushDnsServersField = new ListBox<>();
        pushDnsServersField.setHeight(30, Unit.EX);
        TextField editDnsServerField = new TextField();
        editDnsServerField.setValueChangeMode(ValueChangeMode.EAGER);
        Button addDnsServerButton = new Button(
                "Add",
                e -> {
                    List<String> dnsServers = vpnSettings.getPushDnsServers();
                    dnsServers.add(editDnsServerField.getValue());
                    pushDnsServersField.setItems(dnsServers);
                });
        Button updateDnsServerButton = new Button(
                "Update",
                e -> {
                    List<String> dnsServers = new LinkedList<>(vpnSettings.getPushDnsServers());
                    dnsServers.remove(pushDnsServersField.getValue());
                    dnsServers.add(editDnsServerField.getValue());
                    pushDnsServersField.setItems(dnsServers);
                    vpnSettings.setPushDnsServers(dnsServers);
                });
        updateDnsServerButton.setEnabled(false);
        Button removeDnsServerButton = new Button(
                "Remove",
                e -> {
                    var dnsServers = new LinkedList<>(vpnSettings.getPushDnsServers());

                    dnsServers.remove(pushDnsServersField.getValue());
                    pushDnsServersField.setItems(dnsServers);
                    vpnSettings.setPushDnsServers(dnsServers);
                });
        removeDnsServerButton.setEnabled(false);
        VerticalLayout pushDnsServersLayout = new VerticalLayout(
                pushDnsServersField,
                editDnsServerField,
                new HorizontalLayout(
                        addDnsServerButton,
                        updateDnsServerButton,
                        removeDnsServerButton
                )
        );
        pushDnsServersLayout.setMargin(true);
        pushDnsServersField.setWidthFull();
        editDnsServerField.setWidthFull();

        ListBox<String> pushRoutesField = new ListBox<>();
        pushRoutesField.setHeight(30, Unit.EX);
        pushRoutesField.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.Background.PRIMARY_10
        );
        TextField editRoutesField = new TextField();
        editRoutesField.setValueChangeMode(ValueChangeMode.EAGER);
        Button addRoutesButton = new Button(
                "Add",
                e -> {
                    List<String> routes = vpnSettings.getPushRoutes();
                    routes.add(editRoutesField.getValue());
                    pushRoutesField.setItems(routes);
                });
        Button updateRoutesButton = new Button(
                "Update",
                e -> {
                    List<String> routes = new LinkedList<>(vpnSettings.getPushRoutes());
                    routes.remove(pushRoutesField.getValue());
                    routes.add(editRoutesField.getValue());
                    pushRoutesField.setItems(routes);
                    vpnSettings.setPushRoutes(routes);
                });
        updateRoutesButton.setEnabled(false);
        Button removeRoutesButton = new Button(
                "Remove",
                e -> {
                    var routes = new LinkedList<>(vpnSettings.getPushRoutes());

                    routes.remove(pushRoutesField.getValue());
                    pushRoutesField.setItems(routes);
                    vpnSettings.setPushRoutes(routes);
                });
        removeDnsServerButton.setEnabled(false);
        VerticalLayout pushRoutesLayout = new VerticalLayout(
                pushRoutesField,
                editRoutesField,
                new HorizontalLayout(
                        addRoutesButton,
                        updateRoutesButton,
                        removeRoutesButton
                )
        );
        pushRoutesField.setWidthFull();
        editRoutesField.setWidthFull();

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

        binder.setBean(vpnSettings);
        pushDnsServersField.setItems(vpnSettings.getPushDnsServers());
        pushRoutesField.setItems(vpnSettings.getPushRoutes());

        binder.addStatusChangeListener((sce) -> {
            saveSettings.setEnabled(!sce.hasValidationErrors());
        });
        AtomicReference<String> editDnsServer = new AtomicReference<>("");
        binder.forField(editDnsServerField)
                .withValidator(new IpValidator(), "Not a valid IP Address")
                .bind(
                        ip -> {
                            return editDnsServer.get();
                        },
                        (ip, v) -> {
                            editDnsServer.set(v);
                        }
                );
        AtomicReference<String> editRoutes = new AtomicReference<>("");
        binder.forField(editRoutesField)
                .bind(
                        ip -> {
                            return editRoutes.get();
                        },
                        (ip, v) -> {
                            editRoutes.set(v);
                        }
                );

        saveSettings.addClickListener((t) -> {
            if (binder.writeBeanIfValid(vpnSettings)) {
                vpnSettings.save(settings);
                openvpnRestController.writeOpenVpnUserServerConfig(vpnSettings);
            }
        });

        pushDnsServersField.addValueChangeListener((e) -> {
            if (e.getValue() != null) {
                editDnsServerField.setValue(e.getValue());
                updateDnsServerButton.setEnabled(true);
                removeDnsServerButton.setEnabled(true);
            } else {
                editDnsServerField.setValue("");
                updateDnsServerButton.setEnabled(false);
                removeDnsServerButton.setEnabled(false);
            }
        });
        pushRoutesField.addValueChangeListener((e) -> {
            if (e.getValue() != null) {
                editRoutesField.setValue(e.getValue());
                updateRoutesButton.setEnabled(true);
                removeRoutesButton.setEnabled(true);
            } else {
                editRoutesField.setValue("");
                updateRoutesButton.setEnabled(false);
                removeRoutesButton.setEnabled(false);
            }
        });

        binder.validate();

        FormLayout formLayout = new FormLayout();
        formLayout.add(name);
        formLayout.add(listenLayout);
        formLayout.add(connectToHost);
        formLayout.add(interfaceLayout);
        formLayout.add(clientNetLayout);
        formLayout.add(keepaliveLayout);
        formLayout.add(pushDnsServersLayout, pushRoutesLayout);

        add(formLayout, saveSettings);
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

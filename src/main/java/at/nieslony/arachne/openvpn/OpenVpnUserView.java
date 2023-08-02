/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.firewall.FirewallBasicsSettings;
import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.EditableListBox;
import at.nieslony.arachne.utils.HostnameValidator;
import at.nieslony.arachne.utils.IpValidator;
import at.nieslony.arachne.utils.NetMask;
import at.nieslony.arachne.utils.SubnetValidator;
import at.nieslony.arachne.utils.TransportProtocol;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.renderer.ComponentRenderer;
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
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

    @Getter
    @Setter
    @EqualsAndHashCode
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

    private Settings settings;
    private OpenVpnUserSettings vpnSettings;
    private Binder<OpenVpnUserSettings> binder;

    public OpenVpnUserView(
            Settings settings,
            OpenVpnRestController openvpnRestController,
            Pki pki
    ) {
        this.settings = settings;
        vpnSettings = new OpenVpnUserSettings(settings);
        binder = new Binder(OpenVpnUserSettings.class);

        Button saveSettings = new Button("Save Settings");
        saveSettings.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        binder.addStatusChangeListener((sce) -> {
            saveSettings.setEnabled(!sce.hasValidationErrors());
        });
        saveSettings.addClickListener((t) -> {
            if (binder.writeBeanIfValid(vpnSettings)) {
                FirewallBasicsSettings firewallBasicsSettings = new FirewallBasicsSettings(settings);
                vpnSettings.save(settings);
                openvpnRestController.writeOpenVpnUserServerConfig(vpnSettings);
                openvpnRestController.writeOpenVpnPluginConfig(
                        vpnSettings,
                        firewallBasicsSettings
                );
            }
        });

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Basics", createBasicsPage());
        tabSheet.add("DNS", createDnsPage());
        tabSheet.add("Routing", createRoutingPage());
        tabSheet.add("Authentication", createAuthPage());

        add(
                tabSheet,
                saveSettings);

        binder.setBean(vpnSettings);
        binder.validate();
    }

    private Component createAuthPage() {
        ComboBox<OpenVpnUserSettings.AuthType> authTypeField = new ComboBox<>("Authentication Type");
        authTypeField.setItems(OpenVpnUserSettings.AuthType.values());
        authTypeField.setWidthFull();

        TextField authPamServiceField = new TextField();
        TextField authHttpUrlField = new TextField();

        RadioButtonGroup<OpenVpnUserSettings.PasswordVerificationType> passwordVerificationTypeField
                = new RadioButtonGroup<>("Password Verification Type");
        passwordVerificationTypeField.setItems(
                OpenVpnUserSettings.PasswordVerificationType.values()
        );
        passwordVerificationTypeField.setRenderer(
                new ComponentRenderer<>(type -> {
                    Text typeField = new Text(type.toString());
                    Component valueField = switch (type) {
                        case HTTP_URL -> {
                            HorizontalLayout hl
                                    = new HorizontalLayout(
                                            authHttpUrlField,
                                            new Text("/api/auth")
                                    );
                            hl.setAlignItems(Alignment.BASELINE);
                            hl.setPadding(false);
                            hl.setFlexGrow(1, authHttpUrlField);
                            yield hl;
                        }
                        case PAM ->
                            authPamServiceField;
                    };
                    HorizontalLayout layout = new HorizontalLayout(
                            typeField,
                            valueField
                    );
                    layout.setAlignItems(Alignment.BASELINE);
                    layout.setFlexGrow(1, valueField);
                    return new Div(layout);
                })
        );
        passwordVerificationTypeField.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        passwordVerificationTypeField.setWidthFull();

        authTypeField.addValueChangeListener((e) -> {
            passwordVerificationTypeField.setEnabled(
                    e.getValue() != OpenVpnUserSettings.AuthType.CERTIFICATE
            );
        });

        passwordVerificationTypeField.addValueChangeListener(
                (e) -> {
                    switch (e.getValue()) {
                        case HTTP_URL -> {
                            authHttpUrlField.setEnabled(true);
                            authPamServiceField.setEnabled(false);
                        }
                        case PAM -> {
                            authHttpUrlField.setEnabled(false);
                            authPamServiceField.setEnabled(true);
                        }
                    }
                });

        binder.forField(authTypeField)
                .bind(OpenVpnUserSettings::getAuthType, OpenVpnUserSettings::setAuthType);
        binder.forField(passwordVerificationTypeField)
                .bind(OpenVpnUserSettings::getPasswordVerificationType, OpenVpnUserSettings::setPasswordVerificationType);
        binder.forField(authPamServiceField)
                .bind(OpenVpnUserSettings::getAuthPamService, OpenVpnUserSettings::setAuthPamService);
        binder.forField(authHttpUrlField)
                .bind(OpenVpnUserSettings::getAuthHttpUrl, OpenVpnUserSettings::setAuthHttpUrl);

        VerticalLayout layout = new VerticalLayout(
                authTypeField,
                passwordVerificationTypeField
        );
        layout.setMinWidth(50, Unit.EM);
        return layout;
    }

    final private Component createBasicsPage() {
        TextField name = new TextField("Network Manager configuration name");
        name.setWidthFull();
        name.setValueChangeMode(ValueChangeMode.EAGER);

        TextField clientConfigName = new TextField("Client Config Filename");
        clientConfigName.setWidthFull();
        clientConfigName.setValueChangeMode(ValueChangeMode.EAGER);

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

        Select<TransportProtocol> protocol = new Select<>();
        protocol.setItems(TransportProtocol.values());
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

        binder.forField(name)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getVpnName, OpenVpnUserSettings::setVpnName);
        binder.forField(clientConfigName)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getClientConfigName, OpenVpnUserSettings::setClientConfigName);
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
                .withValidator(new SubnetValidator(() -> {
                    NetMask mask = clientMask.getValue();
                    if (mask == null) {
                        return 0;
                    } else {
                        return mask.getBits();
                    }
                }))
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

        clientMask.addValueChangeListener((e) -> binder.validate());

        FormLayout formLayout = new FormLayout();
        formLayout.add(name);
        formLayout.add(clientConfigName);
        formLayout.add(listenLayout);
        formLayout.add(connectToHost);
        formLayout.add(interfaceLayout);
        formLayout.add(clientNetLayout);
        formLayout.add(keepaliveLayout);

        return formLayout;
    }

    private Component createDnsPage() {
        HorizontalLayout layout = new HorizontalLayout();

        ListBox<String> pushDnsServersField = new ListBox<>();
        pushDnsServersField.setHeight(30, Unit.EX);
        pushDnsServersField.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Background.PRIMARY_10
        );
        NativeLabel pushDnsServersLabel = new NativeLabel("Push DNS Servers");
        pushDnsServersLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.BODY);

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
                pushDnsServersLabel,
                pushDnsServersField,
                editDnsServerField,
                new HorizontalLayout(
                        addDnsServerButton,
                        updateDnsServerButton,
                        removeDnsServerButton
                )
        );
        pushDnsServersField.setWidthFull();
        editDnsServerField.setWidthFull();
        pushDnsServersLayout.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.MEDIUM
        );

        pushDnsServersField.setItems(vpnSettings.getPushDnsServers());

        AtomicReference<String> editDnsServer = new AtomicReference<>("");
        binder.forField(editDnsServerField)
                .withValidator(new IpValidator())
                .bind(
                        ip -> {
                            return editDnsServer.get();
                        },
                        (ip, v) -> {
                            editDnsServer.set(v);
                        }
                );

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

        EditableListBox searchDomainsField = new EditableListBox("Search Domains") {
            @Override
            protected Validator<String> getValidator() {
                return new HostnameValidator();
            }

        };
        binder.bind(
                searchDomainsField,
                OpenVpnUserSettings::getDnsSearch,
                OpenVpnUserSettings::setDnsSearch
        );

        layout.add(
                pushDnsServersLayout,
                searchDomainsField
        );

        return layout;
    }

    private Component createRoutingPage() {
        VerticalLayout layout = new VerticalLayout();

        ListBox<String> pushRoutesField = new ListBox<>();
        pushRoutesField.setHeight(30, Unit.EX);
        pushRoutesField.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Background.PRIMARY_10
        );

        NativeLabel pushRoutesLabel = new NativeLabel("Push Routes");
        pushRoutesLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.BODY);

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
        removeRoutesButton.setEnabled(false);
        VerticalLayout pushRoutesLayout = new VerticalLayout(
                pushRoutesLabel,
                pushRoutesField,
                editRoutesField,
                new HorizontalLayout(
                        addRoutesButton,
                        updateRoutesButton,
                        removeRoutesButton
                )
        );
        pushRoutesLayout.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.MEDIUM
        );
        pushRoutesField.setWidthFull();
        editRoutesField.setWidthFull();

        pushRoutesField.setItems(vpnSettings.getPushRoutes());

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

        Checkbox routeInternetThroughVpn
                = new Checkbox("Route Internet Traffic through VPN");
        binder.bind(routeInternetThroughVpn, "internetThrouphVpn");

        layout.add(
                pushRoutesLayout,
                routeInternetThroughVpn
        );

        return layout;
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

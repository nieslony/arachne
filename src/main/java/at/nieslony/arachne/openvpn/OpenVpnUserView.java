/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.firewall.UserFirewallBasicsSettings;
import at.nieslony.arachne.openvpnmanagement.ArachneDbus;
import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.utils.components.EditableListBox;
import at.nieslony.arachne.utils.components.ShowNotification;
import at.nieslony.arachne.utils.net.NetMask;
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.net.NicInfo;
import at.nieslony.arachne.utils.net.NicUtils;
import at.nieslony.arachne.utils.net.TransportProtocol;
import at.nieslony.arachne.utils.validators.HostnameValidator;
import at.nieslony.arachne.utils.validators.IpValidator;
import at.nieslony.arachne.utils.validators.SubnetValidator;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "userVpn/settings", layout = ViewTemplate.class)
@PageTitle("OpenVPN User VPN")
@RolesAllowed("ADMIN")
public class OpenVpnUserView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnUserView.class);

    private OpenVpnUserSettings vpnSettings;
    private Binder<OpenVpnUserSettings> binder;

    private final ArachneDbus arachneDbus;

    public OpenVpnUserView(
            Settings settings,
            OpenVpnRestController openvpnRestController,
            ArachneDbus arachneDbus,
            Pki pki
    ) {
        this.arachneDbus = arachneDbus;

        vpnSettings = settings.getSettings(OpenVpnUserSettings.class);
        binder = new Binder<>(OpenVpnUserSettings.class);

        Button saveSettings = new Button("Save Settings");
        saveSettings.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        binder.addStatusChangeListener((sce) -> {
            saveSettings.setEnabled(!sce.hasValidationErrors());
        });
        saveSettings.addClickListener((t) -> {
            if (binder.writeBeanIfValid(vpnSettings)) {
                UserFirewallBasicsSettings firewallBasicsSettings
                        = settings.getSettings(UserFirewallBasicsSettings.class);
                try {
                    vpnSettings.save(settings);
                    openvpnRestController.writeOpenVpnPluginUserConfig(
                            vpnSettings,
                            firewallBasicsSettings
                    );
                    openvpnRestController.writeOpenVpnUserServerConfig(vpnSettings);
                    arachneDbus.restartServer(ArachneDbus.ServerType.USER);
                    ShowNotification.info("OpenVpn restarted with new configuration");
                } catch (SettingsException ex) {
                    logger.error("Cannot save openvpn user settings: " + ex.getMessage());
                } catch (DBusException | DBusExecutionException ex) {
                    String header = "Cannot restart openVpn";
                    logger.error(header + ": " + ex.getMessage());
                    ShowNotification.error(header, ex.getMessage());
                }
            }
        });

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Basics", createBasicsPage());
        tabSheet.add("DNS", createDnsPage());
        tabSheet.add("Routing", createRoutingPage());
        tabSheet.add("Authentication", createAuthPage());

        add(
                tabSheet,
                saveSettings
        );
        setPadding(false);

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

    private Component createBasicsPage() {
        TextField name = new TextField("Network Manager configuration name");
        name.setWidthFull();
        name.setValueChangeMode(ValueChangeMode.EAGER);

        TextField clientConfigName = new TextField("Client Config Filename");
        clientConfigName.setWidthFull();
        clientConfigName.setValueChangeMode(ValueChangeMode.EAGER);

        Select<NicInfo> ipAddresse = new Select<>();
        ipAddresse.setItems(NicUtils.findAllNics());
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

        Checkbox mtuTestField = new Checkbox("MTU Test");

        ComboBox<Integer> statusUpdateIntervalField = new ComboBox<>("Status Update Interval (secs)");
        statusUpdateIntervalField.setItems(
                10, 20, 30, 45, 60, 120
        );

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
                            return NicUtils.findNicByIp(s.getListenIp());
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
        binder.bind(
                mtuTestField,
                OpenVpnUserSettings::getMtuTest,
                OpenVpnUserSettings::setMtuTest
        );
        binder.bind(statusUpdateIntervalField,
                OpenVpnUserSettings::getStatusUpdateSecs,
                OpenVpnUserSettings::setStatusUpdateSecs
        );

        clientMask.addValueChangeListener((e) -> binder.validate());
        protocol.addValueChangeListener((e) -> {
            mtuTestField.setEnabled(e.getValue() == TransportProtocol.UDP);
        });

        FormLayout formLayout = new FormLayout();
        formLayout.add(name);
        formLayout.add(clientConfigName);
        formLayout.add(listenLayout);
        formLayout.add(connectToHost);
        formLayout.add(interfaceLayout);
        formLayout.add(clientNetLayout);
        formLayout.add(keepaliveLayout);
        formLayout.add(mtuTestField);
        formLayout.add(statusUpdateIntervalField);

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

        EditableListBox editDnsServerField = new EditableListBox("Push DNS Servers") {
            @Override
            protected Validator<String> getValidator() {
                return new IpValidator();
            }
        };
        editDnsServerField.setDefaultValuesSupplier(
                "Default DNS Servers",
                () -> NetUtils.getDnsServers()
        );
        binder.bind(
                editDnsServerField,
                OpenVpnUserSettings::getPushDnsServers,
                OpenVpnUserSettings::setPushDnsServers
        );

        EditableListBox searchDomainsField = new EditableListBox("Search Domains") {
            @Override
            protected Validator<String> getValidator() {
                return new HostnameValidator();
            }
        };
        searchDomainsField.setDefaultValuesSupplier(
                "Default Search Domains",
                () -> NetUtils.getDefaultSearchDomains()
        );
        binder.bind(
                searchDomainsField,
                OpenVpnUserSettings::getDnsSearch,
                OpenVpnUserSettings::setDnsSearch
        );

        layout.add(
                editDnsServerField,
                searchDomainsField
        );

        return layout;
    }

    private Component createRoutingPage() {
        VerticalLayout layout = new VerticalLayout();

        EditableListBox pushRoutesField = new EditableListBox("Push Routes") {
            @Override
            protected Validator<String> getValidator() {
                return new SubnetValidator(false);
            }
        };
        pushRoutesField.setDefaultValuesSupplier(
                "Default Routes",
                () -> NetUtils.getDefaultPushRoutes()
        );

        Checkbox routeInternetThroughVpn
                = new Checkbox("Route Internet Traffic through VPN");
        binder.bind(routeInternetThroughVpn, "internetThrouphVpn");

        layout.add(
                pushRoutesField,
                routeInternetThroughVpn
        );

        return layout;
    }
}

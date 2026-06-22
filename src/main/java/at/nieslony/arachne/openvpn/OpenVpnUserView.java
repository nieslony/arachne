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
import at.nieslony.arachne.utils.components.EditVpnRemoteList;
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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;

/**
 *
 * @author claas
 */
@Route(value = "userVpn/settings", layout = ViewTemplate.class)
@PageTitle("OpenVPN User VPN")
@RolesAllowed("ADMIN")
@Slf4j
public class OpenVpnUserView extends VerticalLayout {

    private OpenVpnUserSettings vpnSettings;
    private Binder<OpenVpnUserSettings> binder;

    private IntegerField portField;
    private Select<TransportProtocol> protocol;

    public OpenVpnUserView(
            Settings settings,
            OpenVpnController openvpnRestController,
            ArachneDbus arachneDbus,
            Pki pki
    ) {
        vpnSettings = settings.getSettings(OpenVpnUserSettings.class);
        binder = new Binder<>(OpenVpnUserSettings.class);

        Button saveSettings = new Button("Save and restart VPN");
        saveSettings.setTooltipText("All active conections will reconnect after restart");
        saveSettings.addThemeVariants(ButtonVariant.PRIMARY);
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
                    log.error("Cannot save openvpn user settings: " + ex.getMessage());
                } catch (DBusException | DBusExecutionException ex) {
                    String header = "Cannot restart openVpn";
                    log.error(header + ": " + ex.getMessage());
                    ShowNotification.error(header, ex.getMessage());
                }
            }
        });

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Basics", createBasicsPage());
        tabSheet.add("Connection Details", createConnectionDetailsPage());
        tabSheet.add("DNS", createDnsPage());
        tabSheet.add("Routing", createRoutingPage());
        tabSheet.add("Authentication", createAuthPage());

        add(
                tabSheet,
                saveSettings
        );
        setPadding(false);

        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        log.debug("Additional connectors:");
        tomcat.getAdditionalConnectors().forEach(con -> {
            log.debug("    %s://%s:%d".formatted(
                    con.getScheme(),
                    "???",
                    con.getPort()
            ));
        });

        log.debug("Tomcat connector: %s://%s:%d/%s".formatted(
                tomcat.getSsl(),
                "???",
                tomcat.getPort(),
                tomcat.getContextPath()
        ));

        if (vpnSettings.getAuthHttpUrl() == null) {
            vpnSettings.setAuthHttpUrl(getDefaultAuthUrl());
        }

        binder.setBean(vpnSettings);
        binder.validate();
    }

    private Component createAuthPage() {
        Select<OpenVpnUserSettings.AuthType> authTypeField = new Select<>();
        authTypeField.setLabel("Authentication Type");
        authTypeField.setItems(OpenVpnUserSettings.AuthType.values());
        authTypeField.setWidthFull();

        Select<OpenVpnUserSettings.OtpRequired> authOtpRequired = new Select<>();
        authOtpRequired.setLabel("OTP Required");
        authOtpRequired.setItems(OpenVpnUserSettings.OtpRequired.values());
        authOtpRequired.setWidthFull();

        TextField authOtpIssuerField = new TextField("OTP Issuer");
        authOtpIssuerField.setWidthFull();
        authOtpIssuerField.setClearButtonVisible(true);

        TextField authOtpPromptField = new TextField("OTP Prompt");
        authOtpPromptField.setWidthFull();
        authOtpPromptField.setClearButtonVisible(true);

        Checkbox authOtpShow = new Checkbox("Show OTP while typing");

        TextField authHttpUrlField = new TextField("HTTP Authentication URL");
        Button defaultAuthHttpUrlButton = new Button(
                VaadinIcon.REFRESH.create(),
                e -> authHttpUrlField.setValue(getDefaultAuthUrl())
        );
        HorizontalLayout authHttpUrlLayout
                = new HorizontalLayout(
                        authHttpUrlField,
                        new Text("/api/login"),
                        defaultAuthHttpUrlButton
                );
        authHttpUrlLayout.setFlexGrow(1, authHttpUrlField);
        authHttpUrlLayout.setAlignItems(Alignment.BASELINE);
        authHttpUrlLayout.setWidthFull();

        Select<OpenVpnUserSettings.NetworkManagerRememberPassword> nmRememberPassword
                = new Select<>();
        nmRememberPassword.setLabel("NetworkManager: Remember Password");
        nmRememberPassword.setItems(
                OpenVpnUserSettings.NetworkManagerRememberPassword.values()
        );
        nmRememberPassword.setWidthFull();

        authTypeField.addValueChangeListener((e) -> {
            authHttpUrlField.setEnabled(
                    e.getValue() != OpenVpnUserSettings.AuthType.CERTIFICATE
            );
            nmRememberPassword.setEnabled(
                    e.getValue() != OpenVpnUserSettings.AuthType.CERTIFICATE
            );
        });

        authOtpRequired.addValueChangeListener(e -> {
            boolean enable = !e.getValue().equals(OpenVpnUserSettings.OtpRequired.NEVER);
            authOtpIssuerField.setEnabled(enable);
            authOtpPromptField.setEnabled(enable);
            authOtpShow.setEnabled(enable);
        });

        binder.forField(authTypeField)
                .bind(OpenVpnUserSettings::getAuthType, OpenVpnUserSettings::setAuthType);
        binder.forField(authOtpRequired)
                .bind(OpenVpnUserSettings::getAuthOtpRequired, OpenVpnUserSettings::setAuthOtpRequired);
        binder.forField(authOtpIssuerField)
                .bind(OpenVpnUserSettings::getAuthOtpIssuer, OpenVpnUserSettings::setAuthOtpIssuer);
        binder.forField(authOtpPromptField)
                .bind(OpenVpnUserSettings::getAuthOtpPrompt, OpenVpnUserSettings::setAuthOtpPrompt);
        binder.forField(authOtpShow)
                .bind(OpenVpnUserSettings::getAuthOtpShow, OpenVpnUserSettings::setAuthOtpShow);
        binder.forField(authHttpUrlField)
                .bind(OpenVpnUserSettings::getAuthHttpUrl, OpenVpnUserSettings::setAuthHttpUrl);
        binder.forField(nmRememberPassword)
                .bind(
                        OpenVpnUserSettings::getNetworkManagerRememberPassword,
                        OpenVpnUserSettings::setNetworkManagerRememberPassword
                );

        VerticalLayout layout = new VerticalLayout(
                authTypeField,
                authHttpUrlLayout,
                authOtpRequired,
                authOtpIssuerField,
                authOtpPromptField,
                authOtpShow,
                nmRememberPassword
        );
        layout.setMinWidth(50, Unit.EM);
        return layout;
    }

    private Component createBasicsPage() {
        TextField name = new TextField("Configuration name");
        name.setWidthFull();
        name.setValueChangeMode(ValueChangeMode.EAGER);
        Tooltip nameHints = name.getTooltip()
                .withManual(true)
                .withPosition(Tooltip.TooltipPosition.END)
                .withText("""
                          Placeholders
                          %h - remote hostname
                          %u - username (e.g. doe@EXAMPLE.COM)
                          %U - username without realm (e.g. doe)
                          """);
        Button showTooltip = new Button(
                new Icon(VaadinIcon.INFO_CIRCLE),
                (e) -> nameHints.setOpened(!nameHints.isOpened())
        );
        showTooltip.addThemeVariants(ButtonVariant.TERTIARY);
        name.setSuffixComponent(showTooltip);

        Select<NicInfo> ipAddresse = new Select<>();
        ipAddresse.setItems(NicUtils.findAllNics());
        ipAddresse.setLabel("Listen on");
        ipAddresse.setWidth(20, Unit.EM);

        portField = new IntegerField("Port");
        portField.setMin(1);
        portField.setMax(65534);
        portField.setStepButtonsVisible(true);
        portField.setValueChangeMode(ValueChangeMode.EAGER);
        portField.setWidth(8, Unit.EM);

        protocol = new Select<>();
        protocol.setItems(TransportProtocol.values());
        protocol.setLabel("Protocol");
        protocol.setWidth(8, Unit.EM);

        FormLayout.FormRow listenRow = new FormLayout.FormRow();
        listenRow.add(ipAddresse, 2);
        listenRow.add(portField);
        listenRow.add(protocol);

        EditVpnRemoteList vpnRemoteField = new EditVpnRemoteList("VPN Remotes");
        vpnRemoteField.setDefaultValuesSupplier(
                "Guess from local and public IPs",
                () -> getDefaultVpnRemotes()
        );
        vpnRemoteField.setEnableReorder(true);

        Select<String> interfaceType = new Select<>();
        interfaceType.setItems("tun", "tap");
        interfaceType.setLabel("Interface Type");
        interfaceType.setWidth(8, Unit.EM);

        TextField interfaceName = new TextField("Interface Name");
        interfaceName.setValueChangeMode(ValueChangeMode.EAGER);

        FormLayout.FormRow interfaceRow = new FormLayout.FormRow();
        interfaceRow.add(interfaceType);
        interfaceRow.add(interfaceName, 3);

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

        FormLayout.FormRow clientNetRow = new FormLayout.FormRow();
        clientNetRow.add(clientNetwork, 2);
        clientNetRow.add(clientMask, 2);

        IntegerField connectionTimeoutField = new IntegerField("Connection Timeout");
        connectionTimeoutField.setMin(0);
        connectionTimeoutField.setMax(60 * 60);
        connectionTimeoutField.setSuffixComponent(new Div("seconds"));
        connectionTimeoutField.setStepButtonsVisible(true);

        IntegerField connectionRetryCount = new IntegerField("Retry count");
        connectionRetryCount.setMin(1);
        connectionRetryCount.setMax(9999);
        connectionRetryCount.setClearButtonVisible(true);
        connectionRetryCount.setPlaceholder("Unlimited");
        connectionRetryCount.setStepButtonsVisible(true);
        connectionRetryCount.setRequired(false);

        FormLayout.FormRow retryRow = new FormLayout.FormRow();
        retryRow.add(connectionTimeoutField, 2);
        retryRow.add(connectionRetryCount, 2);
        ComboBox< Integer> statusUpdateIntervalField = new ComboBox<>("User Status Update Interval (secs)");

        statusUpdateIntervalField.setItems(
                10, 20, 30, 45, 60, 120
        );

        binder.forField(name)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getVpnName, OpenVpnUserSettings::setVpnName);
        binder.forField(ipAddresse)
                .asRequired("Value required")
                .bind(
                        (s) -> {
                            return NicUtils.findNicByIp(s.getListenIp());
                        },
                        (s, v) -> {
                            s.setListenIp(v.getIpAddress());
                        });
        binder.forField(portField)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getListenPort, OpenVpnUserSettings::setListenPort);
        binder.forField(protocol)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getListenProtocol, OpenVpnUserSettings::setListenProtocol);
        binder.forField(vpnRemoteField)
                .asRequired((List<VpnRemote> value, ValueContext vc) -> {
                    if (value.isEmpty()) {
                        return ValidationResult.error("List of remotes cannot be empty");
                    }
                    return ValidationResult.ok();
                })
                .bind(OpenVpnUserSettings::getRemoteList, OpenVpnUserSettings::setRemoteList);
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
        binder.forField(connectionTimeoutField)
                .asRequired()
                .bind(OpenVpnUserSettings::getConnectionTimeout, OpenVpnUserSettings::setConnectionTimeout);
        binder.forField(connectionRetryCount)
                .bind(OpenVpnUserSettings::getConnectRetryMax, OpenVpnUserSettings::setConnectRetryMax);
        binder.bind(statusUpdateIntervalField,
                OpenVpnUserSettings::getStatusUpdateSecs,
                OpenVpnUserSettings::setStatusUpdateSecs
        );

        protocol.addValueChangeListener((e) -> {
            vpnRemoteField.setAllowedProtocols(List.of(e.getValue()));
        });
        clientMask.addValueChangeListener((e) -> binder.validate());

        FormLayout formLayout = new FormLayout(
                listenRow,
                interfaceRow,
                clientNetRow,
                retryRow,
                statusUpdateIntervalField);
        formLayout.setAutoResponsive(true);
        formLayout.setExpandFields(true);
        formLayout.setMinColumns(4);

        HorizontalLayout detailsLayout = new HorizontalLayout(
                formLayout,
                vpnRemoteField
        );
        detailsLayout.setFlexGrow(1, formLayout, vpnRemoteField);

        VerticalLayout layout = new VerticalLayout(
                name,
                detailsLayout
        );

        return layout;
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
        binder.bind(pushRoutesField,
                OpenVpnUserSettings::getPushRoutes,
                OpenVpnUserSettings::setPushRoutes
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

    private List<VpnRemote> getDefaultVpnRemotes() {
        int port = portField.getValue();
        TransportProtocol prot = protocol.getValue();
        var privateStream = NicUtils.findAllNics()
                .stream()
                .filter(nic -> !nic.getIpAddress().equals("0.0.0.0"))
                .filter(nic -> !nic.getIpAddress().startsWith("127.0"))
                .map((nic)
                        -> new VpnRemote(
                        nic.getIpAddress(),
                        port,
                        prot
                )
                );
        var publicStream = Stream
                .of(
                        new VpnRemote(NetUtils.myPublicIpAddress(), port, prot),
                        new VpnRemote(NetUtils.myPublicHostname(), port, prot),
                        new VpnRemote(NetUtils.myHostname(), port, prot)
                )
                .filter(rem -> rem != null);
        var l = Stream.concat(privateStream, publicStream)
                .sorted((vr1, vr2) -> {
                    int compHostNames = vr1.getRemoteHost().compareTo(vr2.getRemoteHost());
                    if (compHostNames != 0) {
                        return compHostNames;
                    }
                    if (vr1.getPort() < vr2.getPort()) {
                        return -1;
                    }
                    if (vr1.getPort() > vr2.getPort()) {
                        return 1;
                    }
                    return vr1.getTransportProtocol().name().compareTo(
                            vr2.getTransportProtocol().name()
                    );
                })
                .distinct()
                .toList();
        return l;
    }

    private Component createConnectionDetailsPage() {
        Select<OpenVpnUserSettings.MtuMode> mtuModeSelect = new Select<>();
        mtuModeSelect.setLabel("MTU Mode");
        mtuModeSelect.setItems(OpenVpnUserSettings.MtuMode.values());

        IntegerField mtuField = new IntegerField("MTU");
        mtuField.setStepButtonsVisible(true);

        IntegerField fragmentField = new IntegerField("Fragment");
        fragmentField.setStepButtonsVisible(true);
        fragmentField.setClearButtonVisible(true);
        fragmentField.setPlaceholder("Default Value");

        HorizontalLayout mtuLayout = new HorizontalLayout(
                mtuField,
                fragmentField
        );
        mtuLayout.setMargin(false);
        mtuLayout.setPadding(false);

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

        Select<OpenVpnUserSettings.TlsVersion> tlsVersionMinField = new Select<>();
        tlsVersionMinField.setLabel("Minimum TLS version");
        tlsVersionMinField.setItems(
                OpenVpnUserSettings.TlsVersion.V_1_0,
                OpenVpnUserSettings.TlsVersion.V_1_1,
                OpenVpnUserSettings.TlsVersion.V_1_2
        );

        Select<OpenVpnUserSettings.TlsVersion> tlsVersionMaxField = new Select<>();
        tlsVersionMaxField.setLabel("Maximum TLS version");
        tlsVersionMaxField.setItems(OpenVpnUserSettings.TlsVersion.values());

        binder.forField(mtuModeSelect)
                .bind(OpenVpnUserSettings::getMtuMode, OpenVpnUserSettings::setMtuMode);
        binder.forField(mtuField)
                .bind(OpenVpnUserSettings::getTunMtu, OpenVpnUserSettings::setTunMtu);
        binder.forField(fragmentField)
                .bind(OpenVpnUserSettings::getFragment, OpenVpnUserSettings::setFragment);
        binder.forField(keepaliveInterval)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getKeepaliveInterval, OpenVpnUserSettings::setKeepaliveInterval);
        binder.forField(keepaliveTimeout)
                .asRequired("Value required")
                .bind(OpenVpnUserSettings::getKeepaliveTimeout, OpenVpnUserSettings::setKeepaliveTimeout);
        binder.forField(tlsVersionMinField)
                .asRequired()
                .bind(OpenVpnUserSettings::getTlsVersionMin, OpenVpnUserSettings::setTlsVersionMin);
        binder.forField(tlsVersionMaxField)
                .asRequired()
                .bind(OpenVpnUserSettings::getTlsVersionMax, OpenVpnUserSettings::setTlsVersionMax);

        mtuModeSelect.setItemEnabledProvider((item) -> {
            if (item == OpenVpnUserSettings.MtuMode.AUTO) {
                return protocol.getValue() == TransportProtocol.UDP;
            }
            return true;
        });

        mtuModeSelect.addValueChangeListener((e) -> {
            if (e.getValue() == OpenVpnUserSettings.MtuMode.MANUAL) {
                mtuField.setEnabled(true);
                fragmentField.setEnabled(protocol.getValue() != TransportProtocol.TCP);
            } else {
                mtuField.setEnabled(false);
                fragmentField.setEnabled(false);
            }
        });

        protocol.addValueChangeListener((e) -> {
            var v = mtuModeSelect.getValue();
            mtuModeSelect.getDataProvider().refreshAll();
            mtuModeSelect.setValue(v);
        });

        FormLayout layout = new FormLayout();
        layout.setAutoResponsive(true);
        layout.setExpandFields(true);

        FormLayout.FormRow mtuModeRow = new FormLayout.FormRow();
        mtuModeRow.add(mtuModeSelect, 2);
        layout.addFormRow(mtuModeRow);

        FormLayout.FormRow mtuRow = new FormLayout.FormRow();
        mtuRow.add(mtuField, fragmentField);

        FormLayout.FormRow keepaliveRow = new FormLayout.FormRow();
        keepaliveRow.add(keepaliveInterval, keepaliveTimeout);

        FormLayout.FormRow tlsVersionRow = new FormLayout.FormRow();
        tlsVersionRow.add(tlsVersionMinField, tlsVersionMaxField);

        layout.add(
                mtuModeRow,
                mtuRow,
                keepaliveRow,
                tlsVersionRow
        );

        return layout;
    }

    private String getDefaultAuthUrl() {
        VaadinServletRequest request = VaadinServletRequest.getCurrent();
        String url = "%s://%s:%d%s".formatted(
                request.getScheme(),
                request.getLocalName(),
                request.getLocalPort(),
                request.getServletContext().getContextPath()
        );
        return url;
    }
}

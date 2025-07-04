
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.openvpn.sitevpnupload.SiteConfigUploader;
import at.nieslony.arachne.openvpn.vpnsite.EditRemoteNetwork;
import at.nieslony.arachne.openvpn.vpnsite.RemoteNetwork;
import at.nieslony.arachne.openvpn.vpnsite.SiteVerification;
import at.nieslony.arachne.openvpnmanagement.ArachneDbus;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.ssh.AddSshKeyDialog;
import at.nieslony.arachne.ssh.SshKeyEntity;
import at.nieslony.arachne.ssh.SshKeyRepository;
import at.nieslony.arachne.utils.components.EditableListBox;
import at.nieslony.arachne.utils.components.GenericEditableListBox;
import at.nieslony.arachne.utils.components.ShowNotification;
import at.nieslony.arachne.utils.net.NetMask;
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.net.NicInfo;
import at.nieslony.arachne.utils.net.NicUtils;
import at.nieslony.arachne.utils.net.TransportProtocol;
import at.nieslony.arachne.utils.validators.ConditionalValidator;
import at.nieslony.arachne.utils.validators.HostnameValidator;
import at.nieslony.arachne.utils.validators.IpValidator;
import at.nieslony.arachne.utils.validators.SubnetValidator;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.olli.ClipboardHelper;

/**
 *
 * @author claas
 */
@Route(value = "site2siteVpn/settings", layout = ViewTemplate.class)
@PageTitle("OpenVPN Site to Site VPN")
@RolesAllowed("ADMIN")
public class OpenVpnSiteView extends VerticalLayout {

    enum OnDefSiteEnabled {
        DefSiteEnabled,
        DefSiteDisabled
    }

    private class ComponentEnabler implements Consumer<Boolean> {

        final private HasEnabled component;
        final private Predicate<Boolean> condition;

        ComponentEnabler(OnDefSiteEnabled onDefSiteEnabled, Supplier<Boolean> condition, HasEnabled component) {
            this.component = component;
            this.condition = (isDefaultSite) -> switch (onDefSiteEnabled) {
                case DefSiteDisabled ->
                    !isDefaultSite && condition.get();
                case DefSiteEnabled ->
                    isDefaultSite && condition.get();
            };
        }

        ComponentEnabler(OnDefSiteEnabled onDefSiteEnabled, HasEnabled component) {
            this.component = component;
            this.condition = (isDefaultSite) -> switch (onDefSiteEnabled) {
                case DefSiteDisabled ->
                    !isDefaultSite;
                case DefSiteEnabled ->
                    isDefaultSite;
            };
        }

        ComponentEnabler(Checkbox inheritedCheckbox, HasEnabled component) {
            this.component = component;
            this.condition = (isDefaultSite) -> {
                inheritedCheckbox.setEnabled(!isDefaultSite);
                return isDefaultSite || !inheritedCheckbox.getValue();
            };
        }

        @Override
        public void accept(Boolean isDefaultSite) {
            Boolean enable = condition.test(isDefaultSite);
            component.setEnabled(enable);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnSiteView.class);

    private final Binder<OpenVpnSiteSettings> binder;
    private final Binder<VpnSite> siteBinder;
    private final OpenVpnSiteSettings openVpnSiteSettings;
    private final Settings settings;
    private final OpenVpnController openVpnRestController;
    private final SshKeyRepository sshKeyRepository;
    private final AddSshKeyDialog addSshKeyDialog;

    private boolean siteModified = false;
    private final List<ComponentEnabler> nonDefaultComponents;

    private Select<VpnSite> sites;
    private Button deleteButton;
    private MenuBar siteConfigMenu;

    private TextField remoteHostField;

    private Checkbox inheritDnsServers;
    private EditableListBox dnsServers;
    private Checkbox inheritPushDomains;
    private EditableListBox pushDomains;

    private Checkbox inheritPushRoutes;
    private EditableListBox pushRoutes;
    private Checkbox inheritRouteInternet;
    private Checkbox routeInternet;

    private ComboBox<SshKeyEntity> sshKeys;
    private TextArea sshPrivateKey;
    private TextField sshPublicKey;
    ClipboardHelper copySshPrivateKey;

    private final SiteConfigUploader siteConfigUploader;
    private final VpnSiteController vpnSiteController;
    private final ArachneDbus arachneDbus;

    public OpenVpnSiteView(
            Settings settings,
            OpenVpnController openVpnRestController,
            SiteConfigUploader siteConfigUploader,
            SshKeyRepository sshKeyRepository,
            ArachneDbus arachneDbus,
            VpnSiteController vpnSiteController
    ) {
        this.settings = settings;
        this.openVpnRestController = openVpnRestController;
        this.siteConfigUploader = siteConfigUploader;
        this.nonDefaultComponents = new LinkedList<>();
        this.sshKeyRepository = sshKeyRepository;
        this.addSshKeyDialog = new AddSshKeyDialog((keyEntity) -> {
            keyEntity = sshKeyRepository.save(keyEntity);
            sshKeys.setValue(keyEntity);
        });
        this.vpnSiteController = vpnSiteController;
        this.arachneDbus = arachneDbus;

        binder = new Binder<>(OpenVpnSiteSettings.class);
        siteBinder = new Binder<>(VpnSite.class);
        openVpnSiteSettings = settings.getSettings(OpenVpnSiteSettings.class);
    }

    @PostConstruct
    public void init() {
        TabSheet tabs = new TabSheet();
        tabs.add("Basics", createPageBasics());
        tabs.add("Sites", createPageSites());
        tabs.add("SSH keys", createPageSshKeys());
        tabs.setWidthFull();
        tabs.addSelectedChangeListener((t) -> {
            if (t.getSelectedTab().getLabel().equals("Sites")) {
                var oldValue = sites.getOptionalValue();
                var allSites = vpnSiteController.getAll();
                sites.setItems(allSites);
                if (oldValue.isPresent()) {
                    VpnSite value = vpnSiteController.getSite(oldValue.get(), allSites);
                    sites.setValue(value);
                } else {
                    VpnSite defSite = vpnSiteController.getDefaultSite(allSites);
                    sites.setValue(defSite);
                }
            }
        });

        add(tabs);

        binder.setBean(openVpnSiteSettings);
        binder.validate();
        enableNonDefaultCopmponents(true);
        setPadding(false);
    }

    private Component createPageBasics() {
        Select<NicInfo> ipAddresse = new Select<>();
        ipAddresse.setItems(NicUtils.findAllNics());
        ipAddresse.setLabel("Listen on");
        ipAddresse.setWidth(20, Unit.EM);
        binder.forField(ipAddresse)
                .bind(
                        (s) -> {
                            return NicUtils.findNicByIp(s.getListenIp());
                        },
                        (s, v) -> {
                            s.setListenIp(v.getIpAddress());
                        });

        IntegerField port = new IntegerField("Port");
        port.setMin(1);
        port.setMax(65534);
        port.setStepButtonsVisible(true);
        port.setValueChangeMode(ValueChangeMode.EAGER);
        port.setWidth(8, Unit.EM);
        binder.bind(port, "listenPort");

        Select<TransportProtocol> protocol = new Select<>();
        protocol.setItems(TransportProtocol.values());
        protocol.setLabel("Protocol");
        protocol.setWidth(8, Unit.EM);
        binder.bind(protocol, "listenProtocol");

        HorizontalLayout listenLayout = new HorizontalLayout();
        listenLayout.add(
                ipAddresse,
                new Text(":"),
                port,
                new Text("/"),
                protocol);
        listenLayout.setAlignItems(Alignment.BASELINE);
        listenLayout.setFlexGrow(1, ipAddresse);

        TextField connectToHost = new TextField("Connect to host");
        connectToHost.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(connectToHost)
                .asRequired()
                .withValidator(
                        new HostnameValidator()
                                .withIpAllowed(true)
                                .withResolvableRequired(true)
                )
                .bind(
                        OpenVpnSiteSettings::getConnectToHost,
                        OpenVpnSiteSettings::setConnectToHost
                );

        Select<String> interfaceType = new Select<>();
        interfaceType.setItems("tun", "tap");
        interfaceType.setLabel("Interface Type");
        interfaceType.setWidth(8, Unit.EM);
        binder.bind(
                interfaceType,
                OpenVpnSiteSettings::getDeviceType,
                OpenVpnSiteSettings::setDeviceType
        );

        TextField interfaceName = new TextField("Interface Name");
        interfaceName.setValueChangeMode(ValueChangeMode.EAGER);
        binder.bind(
                interfaceName,
                OpenVpnSiteSettings::getDeviceName,
                OpenVpnSiteSettings::setDeviceName
        );

        HorizontalLayout interfaceLayout = new HorizontalLayout();
        interfaceLayout.add(interfaceType, interfaceName);
        interfaceLayout.setFlexGrow(1, interfaceName);

        TextField siteNetwork = new TextField("Site Network");
        siteNetwork.setValueChangeMode(ValueChangeMode.EAGER);
        binder.bind(siteNetwork,
                OpenVpnSiteSettings::getSiteNetwork,
                OpenVpnSiteSettings::setSiteNetwork
        );

        Select<NetMask> siteNetworkMask = new Select<>();
        siteNetworkMask.setItems(
                IntStream
                        .range(1, 32)
                        .boxed()
                        .map(i -> new NetMask(i))
                        .collect(Collectors.toList())
        );
        siteNetworkMask.setLabel("Subnet Mask");
        binder.forField(siteNetworkMask)
                .bind(
                        (source) -> {
                            int mask = source.getSiteNetworkMask();
                            return new NetMask(mask);
                        },
                        (s, v) -> {
                            s.setSiteNetworkMask(v.getBits());
                        }
                );

        HorizontalLayout siteNetLayout = new HorizontalLayout();
        siteNetLayout.add(siteNetwork, siteNetworkMask);
        siteNetLayout.setFlexGrow(1, siteNetwork, siteNetworkMask);

        IntegerField keepaliveInterval = new IntegerField("Keepalive Interval");
        Div suffix;
        suffix = new Div();
        suffix.setText("seconds");
        keepaliveInterval.setSuffixComponent(suffix);
        keepaliveInterval.setMin(1);
        keepaliveInterval.setStepButtonsVisible(true);
        keepaliveInterval.setWidth(12, Unit.EM);
        keepaliveInterval.setValueChangeMode(ValueChangeMode.EAGER);
        binder.bind(
                keepaliveInterval,
                OpenVpnSiteSettings::getKeepaliveInterval,
                OpenVpnSiteSettings::setKeepaliveInterval
        );

        IntegerField keepaliveTimeout = new IntegerField("Keepalive timeout");
        suffix = new Div();
        suffix.setText("seconds");
        keepaliveTimeout.setSuffixComponent(suffix);
        keepaliveTimeout.setMin(1);
        keepaliveTimeout.setStepButtonsVisible(true);
        keepaliveTimeout.setWidth(12, Unit.EM);
        keepaliveInterval.setValueChangeMode(ValueChangeMode.EAGER);
        binder.bind(
                keepaliveTimeout,
                OpenVpnSiteSettings::getKeepaliveTimeout,
                OpenVpnSiteSettings::setKeepaliveTimeout
        );

        HorizontalLayout keepaliveLayout = new HorizontalLayout();
        keepaliveLayout.add(keepaliveInterval, keepaliveTimeout);

        Checkbox mtuTestField = new Checkbox("MTU Test");
        binder.bind(
                mtuTestField,
                OpenVpnSiteSettings::getMtuTest,
                OpenVpnSiteSettings::setMtuTest
        );

        Button saveBasicsButton = new Button("Save Basics and Restart Site VPN",
                (e) -> onSaveBasics()
        );
        saveBasicsButton.setTooltipText("All sites will reconnect after openVPN restart");
        binder.addStatusChangeListener((sce) -> {
            saveBasicsButton.setEnabled(sce.getBinder().isValid());
        });
        saveBasicsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        FormLayout formLayout = new FormLayout();
        formLayout.add(listenLayout,
                connectToHost,
                interfaceLayout,
                siteNetLayout,
                keepaliveLayout,
                mtuTestField
        );
        VerticalLayout layout = new VerticalLayout(
                formLayout, saveBasicsButton
        );
        layout.setMargin(false);
        layout.setPadding(false);

        return layout;
    }

    private Component createPageSites() {
        VerticalLayout layout = new VerticalLayout();

        sites = new Select<>();
        sites.setItemLabelGenerator((site) -> site.label());
        sites.setLabel("Sites");
        sites.setWidthFull();

        Button renameButton = new Button(
                "Rename...",
                (e) -> {
                    setNameDescDialog(
                            sites.getValue(),
                            (site) -> {
                                try {
                                    site = vpnSiteController.addSite(site);
                                    var allSites = vpnSiteController.getAll();
                                    sites.setItems(allSites);
                                    var setSite = vpnSiteController.getSite(site, allSites);
                                    sites.setValue(setSite);
                                } catch (VpnSiteController.OnlyOneDefaultSiteAllowed ex) {
                                    logger.error(ex.getMessage());
                                }
                            }
                    );
                }
        );

        deleteButton = new Button("Delete...", (e) -> {
            if (sites.getValue().getId() == 0) {
                logger.warn("Cannot remove site id=0");
                return;
            }
            ConfirmDialog dlg = new ConfirmDialog();
            String conName = sites.getValue().getName();
            dlg.setHeader("Delete VPN Site \"%s\"".formatted(conName));
            dlg.setText("""
                        Really delete VPN site \"%s\"?
                        This action cannot be undone."""
                    .formatted(conName));
            dlg.setCancelable(true);
            dlg.addConfirmListener((ce) -> {
                VpnSite site = sites.getValue();
                vpnSiteController.deleteSite(site);
                List<VpnSite> allSites = vpnSiteController.getAll();
                sites.setItems(allSites);
                sites.setValue(vpnSiteController.getDefaultSite(allSites));
            });
            dlg.open();
        });
        nonDefaultComponents.add(new ComponentEnabler(
                OnDefSiteEnabled.DefSiteDisabled,
                deleteButton
        ));

        Button addButton = new Button("Add...", (e) -> {
            setNameDescDialog(null, (site) -> {
                var allSites = vpnSiteController.getAll();
                sites.setItems(allSites);
                sites.setValue(vpnSiteController.getSite(site, allSites));
                siteBinder.validate();
                logger.info("Created: " + site.toString());
            });
        });

        HorizontalLayout sitesLayout = new HorizontalLayout(
                sites,
                renameButton,
                deleteButton,
                addButton
        );
        sitesLayout.setFlexGrow(1, sites);
        sitesLayout.setAlignItems(Alignment.BASELINE);
        sitesLayout.setWidthFull();

        siteConfigMenu = new MenuBar();
        siteConfigMenu.addThemeVariants(MenuBarVariant.LUMO_DROPDOWN_INDICATORS);
        MenuItem siteConfigItem = siteConfigMenu.addItem("Site Config");
        SubMenu subMenu = siteConfigItem.getSubMenu();

        DownloadHandler dhl = DownloadHandler.fromInputStream((de) -> {
            StringWriter contentWriter = new StringWriter();
            openVpnRestController.writeOpenVpnSiteRemoteConfig(
                    sites.getValue().getId(),
                    contentWriter
            );
            byte[] config = contentWriter.toString().getBytes();

            String fileName = openVpnRestController.getOpenVpnSiteRemoteConfigName(
                    openVpnSiteSettings,
                    sites.getValue()
            );
            String contentType = "application/x-openvpn-profile";
            var resp = new DownloadResponse(
                    new ByteArrayInputStream(config),
                    fileName,
                    contentType,
                    config.length
            );
            return resp;
        });
        Anchor downloadComponent = new Anchor(dhl, "Download");

        subMenu.addItem(downloadComponent);
        subMenu.addItem("Upload to site...", (e) -> {
            siteConfigUploader.openDialog(sites.getValue());
        });
        subMenu.addItem("View...", (e) -> {
            StringWriter cfgWriter = new StringWriter();
            openVpnRestController.writeOpenVpnSiteRemoteConfig(
                    sites.getValue().getId(),
                    cfgWriter
            );
            createDialogRemoteConfig(cfgWriter.toString()).open();
        });
        nonDefaultComponents.add(new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, siteConfigMenu));
        nonDefaultComponents.add(new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, downloadComponent));

        TabSheet siteSettingsTab = new TabSheet();
        siteSettingsTab.add("Connection", createPageSitesConnection());
        siteSettingsTab.add("DNS", createPageSitesDns());
        siteSettingsTab.add("Routes", createPageSitesRoutes());
        siteSettingsTab.setWidthFull();

        Button saveSitesButton = new Button("Save Site", (e) -> onSaveSite());
        saveSitesButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(
                sitesLayout,
                siteConfigMenu,
                siteSettingsTab,
                saveSitesButton
        );
        layout.setMargin(false);
        layout.setPadding(false);

        sites.addValueChangeListener(this::onChangeSite);

        siteBinder.addValueChangeListener((e) -> {
            siteModified |= e.isFromClient();
            saveSitesButton.setEnabled(siteBinder.isValid());
        });

        siteBinder.addStatusChangeListener((sce) -> {
            saveSitesButton.setEnabled(sce.getBinder().isValid());
        });

        return layout;
    }

    private void setNameDescDialog(
            VpnSite site,
            Consumer<VpnSite> onOk
    ) {
        Dialog dlg = new Dialog();
        if (site == null) {
            dlg.setHeaderTitle("Add VPN Site");
        } else {
            dlg.setHeaderTitle("Rename VPN Site");
        }

        TextField nameField = new TextField("Connection Name");
        nameField.setRequired(true);
        nameField.setValue(site != null ? site.getName() : "New Site");

        TextField descriptionField = new TextField("Description");
        descriptionField.setValue(site != null ? site.getDescription() : "");

        VerticalLayout layout = new VerticalLayout(
                nameField,
                descriptionField
        );
        layout.setMargin(false);
        layout.setPadding(false);

        dlg.add(layout);

        Button okButton = new Button("OK", (e) -> {
            dlg.close();
            if (site == null) {
                onOk.accept(vpnSiteController.addSite(
                        nameField.getValue(),
                        descriptionField.getValue()
                ));
            } else {
                site.setName(nameField.getValue());
                site.setDescription(descriptionField.getValue());
                onOk.accept(site);
            }
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", (e) -> {
            dlg.close();
        });

        dlg.getFooter().add(cancelButton, okButton);
        dlg.open();
    }

    private Component createPageSitesDns() {
        dnsServers = new EditableListBox("DNS Servers") {
            @Override
            protected Validator<String> getValidator() {
                return new IpValidator();
            }
        };
        dnsServers.setDefaultValuesSupplier(
                "Default DNS Servers",
                () -> NetUtils.getDnsServers()
        );

        siteBinder.bind(
                dnsServers,
                VpnSite::getPushDnsServers,
                VpnSite::setPushDnsServers
        );
        inheritDnsServers = new Checkbox("Inherit");
        inheritDnsServers.addValueChangeListener((e) -> {
            dnsServers.setEnabled(!e.getValue() || siteBinder.getBean().getId() == 0);
        });
        siteBinder.bind(
                inheritDnsServers,
                VpnSite::isInheritDnsServers,
                VpnSite::setInheritDnsServers
        );
        nonDefaultComponents.add(new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, inheritDnsServers));
        nonDefaultComponents.add(new ComponentEnabler(inheritDnsServers, dnsServers));

        pushDomains = new EditableListBox("Push Domains") {
            @Override
            protected Validator<String> getValidator() {
                return new HostnameValidator();
            }
        };
        pushDomains.setDefaultValuesSupplier(
                "Default Search Domains",
                () -> NetUtils.getDefaultSearchDomains()
        );
        siteBinder.bind(
                pushDomains,
                VpnSite::getPushSearchDomains,
                VpnSite::setPushSearchDomains
        );
        inheritPushDomains = new Checkbox("Inherit");
        inheritPushDomains.addValueChangeListener((e) -> {
            pushDomains.setEnabled(!e.getValue() || siteBinder.getBean().getId() == 0);
        });
        siteBinder.bind(
                inheritPushDomains,
                VpnSite::isInheritPushDomains,
                VpnSite::setInheritPushDomains
        );
        nonDefaultComponents.add(new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, inheritPushDomains));
        nonDefaultComponents.add(new ComponentEnabler(inheritPushDomains, pushDomains));

        HorizontalLayout layout = new HorizontalLayout(
                new VerticalLayout(
                        inheritDnsServers,
                        dnsServers
                ),
                new VerticalLayout(
                        inheritPushDomains,
                        pushDomains
                )
        );
        layout.setMaxWidth(60, Unit.EM);

        return layout;
    }

    private Component createPageSitesRoutes() {
        pushRoutes = new EditableListBox("Push Routes") {
            @Override
            protected Validator<String> getValidator() {
                return new SubnetValidator(true);
            }
        };
        pushRoutes.setDefaultValuesSupplier(
                "Default Push Routes",
                () -> NetUtils.getDefaultPushRoutes()
        );

        siteBinder.bind(
                pushRoutes,
                VpnSite::getPushRoutes,
                VpnSite::setPushRoutes
        );
        inheritPushRoutes = new Checkbox("Inherit");
        inheritPushRoutes.addValueChangeListener((e) -> {
            boolean enabled = !e.getValue() || sites.getValue().getId() == 0;
            pushRoutes.setEnabled(enabled);
        });
        siteBinder.bind(
                inheritPushRoutes,
                VpnSite::isInheritPushRoutes,
                VpnSite::setInheritPushRoutes
        );
        nonDefaultComponents.add(new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, inheritPushRoutes));
        nonDefaultComponents.add(new ComponentEnabler(inheritPushRoutes, pushRoutes));

        routeInternet = new Checkbox("Route Internet Traffic through VPN");
        siteBinder.bind(
                routeInternet,
                VpnSite::isRouteInternetThroughVpn,
                VpnSite::setRouteInternetThroughVpn
        );
        inheritRouteInternet = new Checkbox("Inherit");
        inheritRouteInternet.addValueChangeListener((e) -> {
            routeInternet.setEnabled(!e.getValue() || siteBinder.getBean().getId() == 0);
        });
        siteBinder.bind(
                inheritRouteInternet,
                VpnSite::isInheritRouteInternetThroughVpn,
                VpnSite::setInheritRouteInternetThroughVpn
        );
        nonDefaultComponents.add(new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, inheritRouteInternet));
        nonDefaultComponents.add(new ComponentEnabler(inheritRouteInternet, routeInternet));

        GenericEditableListBox<RemoteNetwork, EditRemoteNetwork> remoteNetworks
                = new GenericEditableListBox<>(
                        "Remote Networks",
                        new EditRemoteNetwork()
                );
        remoteNetworks.setMaxWidth(30, Unit.EM);
        siteBinder.bind(remoteNetworks,
                VpnSite::getRemoteNetworks,
                VpnSite::setRemoteNetworks
        );
        nonDefaultComponents.add(
                new ComponentEnabler(
                        OnDefSiteEnabled.DefSiteDisabled,
                        remoteNetworks
                )
        );

        VerticalLayout routesLayout = new VerticalLayout(
                inheritPushRoutes,
                pushRoutes,
                new HorizontalLayout(
                        inheritRouteInternet,
                        routeInternet
                ),
                remoteNetworks
        );
        routesLayout.setMaxWidth(30, Unit.EM);
        routesLayout.setMargin(false);

        var layout = new HorizontalLayout(
                routesLayout,
                remoteNetworks
        );
        layout.setWrap(true);

        return layout;
    }

    private Component createPageSitesConnection() {
        VerticalLayout layout = new VerticalLayout();

        remoteHostField = new TextField("Remote Host");
        remoteHostField.setWidthFull();
        remoteHostField.setValueChangeMode(ValueChangeMode.EAGER);
        remoteHostField.setWidthFull();
        siteBinder.forField(remoteHostField)
                .asRequired((v, vc) -> {
                    if (siteBinder.getBean().getId() == 0) {
                        return ValidationResult.ok();
                    } else {
                        return v.isEmpty()
                                ? ValidationResult.error("Hostname required")
                                : ValidationResult.ok();
                    }
                })
                .withValidator(new ConditionalValidator<>(
                        () -> siteBinder.getBean().getId() == 0,
                        new HostnameValidator()
                ))
                .bind(
                        VpnSite::getSiteHostname,
                        VpnSite::setSiteHostname
                );
        nonDefaultComponents.add(new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, remoteHostField));

        Select<SiteVerification> siteVerificationField = new Select<>();
        siteVerificationField.setLabel("Site Verification");
        siteVerificationField.setItems(SiteVerification.values());
        siteVerificationField.setWidthFull();
        siteBinder.forField(siteVerificationField)
                .bind(VpnSite::getSiteVerification, VpnSite::setSiteVerification);
        nonDefaultComponents.add(
                new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, siteVerificationField)
        );

        EditableListBox siteIpWhiteList = new EditableListBox("IP Whitelist") {
            @Override
            protected Validator<String> getValidator() {
                return new IpValidator();
            }
        };
        siteBinder.bind(
                siteIpWhiteList,
                VpnSite::getIpWhiteList,
                VpnSite::setIpWhiteList
        );
        nonDefaultComponents.add(new ComponentEnabler(
                OnDefSiteEnabled.DefSiteDisabled,
                ()
                -> siteVerificationField.getValue() != null
                && siteVerificationField.getValue().equals(SiteVerification.WHITELIST),
                siteIpWhiteList
        ));

        layout.add(
                remoteHostField,
                siteVerificationField,
                siteIpWhiteList
        );
        layout.setMaxWidth(30, Unit.EM);

        siteVerificationField.addValueChangeListener((e) -> {
            siteIpWhiteList.setEnabled(
                    e.getValue() != null
                    && e.getValue().equals(SiteVerification.WHITELIST)
            );
        });

        return layout;
    }

    private Component createPageSshKeys() {
        sshKeys = new ComboBox<>("SSH Keys");
        List<SshKeyEntity> sshKeyItems = sshKeyRepository.findAll();
        sshKeys.setItems(sshKeyItems);
        sshKeys.setWidthFull();
        sshKeys.setItemLabelGenerator((item) -> item.getLabel());

        Button createNewKeyPair = new Button("Create new Key Pair...",
                (e) -> addSshKeyDialog.open()
        );

        Button deleteKeyPair = new Button("Delete", (e) -> {
            SshKeyEntity entity = sshKeys.getValue();
            if (entity != null) {
                sshKeyRepository.delete(entity);
                sshKeys.setItems(sshKeyRepository.findAll());
            }
        });

        HorizontalLayout sshKeysLayout = new HorizontalLayout(
                sshKeys,
                createNewKeyPair,
                deleteKeyPair
        );
        sshKeysLayout.setWidthFull();
        sshKeysLayout.setAlignItems(Alignment.BASELINE);

        sshPrivateKey = new TextArea("Private Key");
        sshPrivateKey.setHeight(40, Unit.EX);
        sshPrivateKey.setWidth(42, Unit.EM);
        sshPrivateKey.setReadOnly(true);

        sshPublicKey = new TextField("Public Key");
        sshPublicKey.setWidthFull();
        sshPublicKey.setReadOnly(true);
        copySshPrivateKey = new ClipboardHelper(
                "",
                new Button(VaadinIcon.COPY.create())
        );
        HorizontalLayout pubKeylayout = new HorizontalLayout(
                sshPublicKey,
                copySshPrivateKey
        );
        pubKeylayout.setWidthFull();
        pubKeylayout.setAlignItems(Alignment.BASELINE);

        VerticalLayout layout = new VerticalLayout(
                sshKeysLayout,
                new Scroller(sshPrivateKey),
                pubKeylayout
        );

        sshKeys.addValueChangeListener((e) -> {
            SshKeyEntity entity = e.getValue();
            if (entity != null) {
                sshPrivateKey.setValue(entity.getPrivateKey());
                sshPublicKey.setValue(entity.getPublicKey());
            } else {
                sshPrivateKey.setValue("");
                sshPublicKey.setValue("");
            }
        });

        if (!sshKeyItems.isEmpty()) {
            sshKeys.setValue(sshKeyItems.get(0));
        }

        return layout;
    }

    private void onSaveBasics() {
        try {
            openVpnSiteSettings.save(settings);
            openVpnRestController.writeOpenVpnSiteServerConfig();
            openVpnRestController.writeOpenVpnPluginSiteConfig();
            openVpnRestController.writeCrl();
            arachneDbus.restartServer(ArachneDbus.ServerType.SITE);
            ShowNotification.info("OpenVpn restarted with new configuration");
        } catch (SettingsException ex) {
            logger.error("Cannot save openvpn site vpn: " + ex.getMessage());
        } catch (DBusException | DBusExecutionException ex) {
            String header = "Cannot restart openVpn";
            logger.error(header + ": " + ex.getMessage());
            ShowNotification.error(header, ex.getMessage());
        }
    }

    private void onSaveSite() {
        VpnSite curSite = siteBinder.getBean();

        try {
            vpnSiteController.saveSite(curSite);
            openVpnRestController.prepareSiteClientDir();
            openVpnRestController.writeOpenVpnSiteServerSitesPluginConfig();
            openVpnRestController.writeOpenVpnSiteServerSitesConfig();
            siteModified = false;
        } catch (VpnSiteController.OnlyOneDefaultSiteAllowed ex) {
            logger.error("Cannot save site %s: %s"
                    .formatted(curSite.toString(), ex.getMessage())
            );
        }
    }

    private Dialog createDialogRemoteConfig(String cfg) {
        Dialog dlg = new Dialog("Remote Configuration");

        Pre cfgField = new Pre(cfg);
        dlg.add(cfgField);

        Button closeButton = new Button("Close", (e) -> {
            dlg.close();
        });
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dlg.getFooter().add(closeButton);

        return dlg;
    }

    private void enableNonDefaultCopmponents(boolean isDefaultSite) {
        nonDefaultComponents.forEach((componentEnabler) -> {
            componentEnabler.accept(isDefaultSite);
        });
    }

    private void onChangeSite(
            AbstractField.ComponentValueChangeEvent<Select<VpnSite>, VpnSite> e
    ) {
        Boolean isDefaultSiteSelected
                = e.getValue() != null && e.getValue().isDefaultSite();

        if (siteModified) {
            if (e.isFromClient()) {
                ConfirmDialog dlg = new ConfirmDialog(
                        "Unsaved Changes",
                        "Site has unsaved changes. Save now?",
                        "Save", (ce) -> onSaveSite(),
                        "Reject", (ce) -> {
                            siteModified = false;
                            siteBinder.setBean(e.getOldValue());
                        },
                        "Cancel", (ce) -> {
                            sites.setValue(e.getOldValue());
                        }
                );
                dlg.open();
            }
        } else {
            if (e.getValue() != null) {
                VpnSite site = e.getValue();
                VpnSite defSite = vpnSiteController.getDefaultSite();
                site.updateInheritedValues(defSite);
                siteBinder.setBean(site);
            }
        }
        siteBinder.refreshFields();
        siteBinder.validate();

        enableNonDefaultCopmponents(isDefaultSiteSelected);
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.utils.EditableListBox;
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
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
import jakarta.annotation.security.RolesAllowed;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.olli.FileDownloadWrapper;

/**
 *
 * @author claas
 */
@Route(value = "openvpn-site2site", layout = ViewTemplate.class)
@PageTitle("OpenVPN Site 2 Site | Arachne")
@RolesAllowed("ADMIN")
public class OpenVpnSiteView extends VerticalLayout {

    enum OnDefSiteEnabled {
        DefSiteEnabled,
        DefSiteDisabled
    }

    private class ComponentEnabler implements Consumer<Boolean> {

        final private HasEnabled component;
        final private Checkbox inheritdCheckBox;
        final private OnDefSiteEnabled onDefSiteEnabled;

        ComponentEnabler(OnDefSiteEnabled onDefSiteEnabled, HasEnabled component) {
            this.component = component;
            this.inheritdCheckBox = null;
            this.onDefSiteEnabled = onDefSiteEnabled;
        }

        ComponentEnabler(Checkbox inheritedCheckbox, HasEnabled component) {
            this.component = component;
            this.inheritdCheckBox = inheritedCheckbox;
            this.onDefSiteEnabled = OnDefSiteEnabled.DefSiteEnabled;
        }

        @Override
        public void accept(Boolean isDefaultSite) {
            if (inheritdCheckBox == null) {
                component.setEnabled(
                        switch (onDefSiteEnabled) {
                    case DefSiteDisabled ->
                        !isDefaultSite;
                    case DefSiteEnabled ->
                        isDefaultSite;
                });
            } else {
                component.setEnabled(isDefaultSite || !inheritdCheckBox.getValue());
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnSiteView.class);

    private final Binder<OpenVpnSiteSettings> binder;
    private final Binder<VpnSite> siteBinder;
    private final OpenVpnSiteSettings openVpnSiteSettings;
    private final Settings settings;
    private final OpenVpnRestController openVpnRestController;

    private boolean siteModified = false;
    private final List<ComponentEnabler> nonDefaultComponents;

    private Select<VpnSite> sites;
    private Button deleteButton;
    private MenuBar siteConfigMenu;

    private TextField remoteHostField;
    private TextArea preSharedKeyField;

    private Checkbox inheritDnsServers;
    private EditableListBox dnsServers;
    private Checkbox inheritPushDomains;
    private EditableListBox pushDomains;

    private Checkbox inheritPushRoutes;
    private EditableListBox pushRoutes;
    private Checkbox inheritRouteInternet;
    private Checkbox routeInternet;

    private final SiteConfigUploader siteConfigUploader;

    enum SshAuthType {
        USERNAME_PASSWORD("Username/Password"),
        PRESHARED_KEY("Preshared Key");

        private final String value;

        private SshAuthType(String s) {
            value = s;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public OpenVpnSiteView(
            Settings settings,
            OpenVpnRestController openVpnRestController,
            SiteConfigUploader siteConfigUploader
    ) {
        this.settings = settings;
        this.openVpnRestController = openVpnRestController;
        this.siteConfigUploader = siteConfigUploader;
        this.nonDefaultComponents = new LinkedList<>();

        siteConfigUploader = new SiteConfigUploader();
        binder = new Binder<>(OpenVpnSiteSettings.class);
        siteBinder = new Binder<>(VpnSite.class);
        openVpnSiteSettings = settings.getSettings(OpenVpnSiteSettings.class);

        TabSheet tabs = new TabSheet();
        tabs.add("Basics", createBasicsPage());
        tabs.add("Sites", createSitesPage());
        tabs.setWidthFull();

        Button saveButton = new Button("Save", (t) -> onSaveSite());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(tabs, saveButton);

        binder.setBean(openVpnSiteSettings);
        binder.validate();
        enableNonDefaultCopmponents(true);
    }

    private Component createBasicsPage() {
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
        binder.bind(
                connectToHost,
                OpenVpnSiteSettings::getRemote,
                OpenVpnSiteSettings::setRemote
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

        FormLayout layout = new FormLayout();
        layout.add(listenLayout,
                connectToHost,
                interfaceLayout,
                siteNetLayout,
                keepaliveLayout,
                mtuTestField
        );

        return layout;
    }

    private Component createSitesPage() {
        VerticalLayout layout = new VerticalLayout();

        sites = new Select<>();
        sites.setItemLabelGenerator((site) -> site.label());
        sites.setLabel("Sites");
        sites.setWidthFull();
        sites.setItems(openVpnSiteSettings.getVpnSites());

        Button renameButton = new Button(
                "Rename...",
                (e) -> {
                    setNameDescDialog(
                            sites.getValue(),
                            (site) -> {
                                sites.setItems(openVpnSiteSettings.getVpnSites());
                                sites.setValue(site);
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
                openVpnSiteSettings.deleteSite(settings, site.getId());
                sites.setItems(openVpnSiteSettings.getVpnSites());
                sites.setValue(openVpnSiteSettings.getVpnSite(0));
            });
            dlg.open();
        });
        nonDefaultComponents.add(new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, deleteButton));

        Button addButton = new Button("Add...", (e) -> {
            setNameDescDialog(null, (site) -> {
                sites.setItems(openVpnSiteSettings.getVpnSites());
                sites.setValue(site);
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
        MenuItem siteConfigItem = siteConfigMenu.addItem(new HorizontalLayout(
                new Text("Site Config"),
                new Icon(VaadinIcon.CHEVRON_DOWN)
        ));
        SubMenu subMenu = siteConfigItem.getSubMenu();
        FileDownloadWrapper downloadComponent = new FileDownloadWrapper(
                "bla",
                () -> {
                    StringWriter cfgWriter = new StringWriter();
                    openVpnRestController.writeOpenVpnSiteRemoteConfig(
                            sites.getValue().getId(),
                            cfgWriter
                    );
                    return cfgWriter.toString().getBytes();
                }
        );
        downloadComponent.setText("Download...");
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
            createRemoteConfigDialog(cfgWriter.toString()).open();
        });
        nonDefaultComponents.add(new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, siteConfigMenu));

        TabSheet siteSettingsTab = new TabSheet();
        siteSettingsTab.add("Connection", createSiteConnectionPage());
        siteSettingsTab.add("DNS", createDnsPage());
        siteSettingsTab.add("Routes", createRoutesTab());

        layout.add(
                sitesLayout,
                siteConfigMenu,
                siteSettingsTab
        );
        layout.setMargin(false);
        layout.setPadding(false);

        sites.addValueChangeListener(this::onChangeSite);

        siteBinder.addValueChangeListener((e) -> {
            logger.info("------ Site modified. From client: " + e.isFromClient());
            siteModified |= e.isFromClient();
        });

        sites.setValue(openVpnSiteSettings.getVpnSite(0));
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
                onOk.accept(openVpnSiteSettings.addSite(
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

    private Component createDnsPage() {
        dnsServers = new EditableListBox("DNS Servers") {
            @Override
            protected Validator<String> getValidator() {
                return new IpValidator();
            }
        };
        Button defaultDnsServers = new Button("Default DNS Servers",
                (e) -> dnsServers.setValue(NetUtils.getDnsServers())
        );

        siteBinder.bind(
                dnsServers,
                (source) -> source.getPushDnsServers(),
                (source, value) -> {
                    source.setPushDnsServers(value);
                });
        inheritDnsServers = new Checkbox("Inherit");
        inheritDnsServers.addValueChangeListener((e) -> {
            defaultDnsServers.setEnabled(!e.getValue() || siteBinder.getBean().getId() == 0);
            dnsServers.setEnabled(!e.getValue() || siteBinder.getBean().getId() == 0);
        });
        siteBinder.bind(
                inheritDnsServers,
                VpnSite::isInheritDnsServers,
                VpnSite::setInheritDnsServers
        );
        nonDefaultComponents.add(new ComponentEnabler(inheritDnsServers, defaultDnsServers));
        nonDefaultComponents.add(new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, inheritDnsServers));
        nonDefaultComponents.add(new ComponentEnabler(inheritDnsServers, dnsServers));

        pushDomains = new EditableListBox("Push Domains") {
            @Override
            protected Validator<String> getValidator() {
                return new HostnameValidator();
            }
        };
        Button defaultPushDomains = new Button("Default Push Domains",
                (e) -> pushDomains.setValue(NetUtils.getDefaultSearchDomains())
        );
        siteBinder.bind(
                pushDomains,
                (source) -> source.getPushSearchDomains(),
                (source, value) -> source.setPushSearchDomains(value)
        );
        inheritPushDomains = new Checkbox("Inherit");
        inheritPushDomains.addValueChangeListener((e) -> {
            defaultPushDomains.setEnabled(!e.getValue() || siteBinder.getBean().getId() == 0);
            pushDomains.setEnabled(!e.getValue() || siteBinder.getBean().getId() == 0);
        });
        siteBinder.bind(
                inheritPushDomains,
                VpnSite::isInheritPushDomains,
                VpnSite::setInheritPushDomains
        );
        nonDefaultComponents.add(new ComponentEnabler(inheritPushDomains, defaultPushDomains));
        nonDefaultComponents.add(new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, inheritPushDomains));
        nonDefaultComponents.add(new ComponentEnabler(inheritPushDomains, pushDomains));

        HorizontalLayout layout = new HorizontalLayout(
                new VerticalLayout(
                        inheritDnsServers,
                        defaultDnsServers,
                        dnsServers
                ),
                new VerticalLayout(
                        inheritPushDomains,
                        defaultPushDomains,
                        pushDomains
                )
        );

        return layout;
    }

    private Component createRoutesTab() {
        pushRoutes = new EditableListBox("Push Routes") {
            @Override
            protected Validator<String> getValidator() {
                return new SubnetValidator(true);
            }
        };

        Button defaultPushRoutes = new Button("Defails Push Routes",
                (t) -> pushRoutes.setValue(NetUtils.getDefaultPushRoutes())
        );

        siteBinder.bind(
                pushRoutes,
                (source) -> source.getPushRoutes(),
                (source, value) -> source.setPushRoutes(value)
        );
        inheritPushRoutes = new Checkbox("Inherit");
        inheritPushRoutes.addValueChangeListener((e) -> {
            boolean enabled = !e.getValue() || sites.getValue().getId() == 0;
            logger.info("value=" + e.getValue() + " siteId=" + sites.getValue().getId());
            pushRoutes.setEnabled(enabled);
            defaultPushRoutes.setEnabled(enabled);
        });
        siteBinder.bind(
                inheritPushRoutes,
                VpnSite::isInheritPushRoutes,
                VpnSite::setInheritPushRoutes
        );
        nonDefaultComponents.add(new ComponentEnabler(inheritPushRoutes, defaultPushRoutes));
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

        VerticalLayout layout = new VerticalLayout(
                inheritPushRoutes,
                defaultPushRoutes,
                pushRoutes,
                new HorizontalLayout(
                        inheritRouteInternet,
                        routeInternet
                )
        );

        return layout;
    }

    private Component createSiteConnectionPage() {
        VerticalLayout layout = new VerticalLayout();

        remoteHostField = new TextField("Remote Host");
        remoteHostField.setWidthFull();
        remoteHostField.setValueChangeMode(ValueChangeMode.EAGER);
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
                        VpnSite::getRemoteHost,
                        VpnSite::setRemoteHost
                );
        nonDefaultComponents.add(new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, remoteHostField));

        preSharedKeyField = new TextArea("Preshared Key");
        preSharedKeyField.setMinWidth(80, Unit.EM);
        preSharedKeyField.setHeight(10, Unit.EX);
        siteBinder.bind(preSharedKeyField,
                VpnSite::getPreSharedKey,
                VpnSite::setPreSharedKey
        );
        Button createPSKButton = new Button("Create", (e) -> {
            preSharedKeyField.setValue(OpenVpnSiteSettings.createPreSharedKey());
        });
        nonDefaultComponents.add(new ComponentEnabler(OnDefSiteEnabled.DefSiteDisabled, preSharedKeyField));

        layout.add(
                remoteHostField,
                preSharedKeyField,
                createPSKButton
        );

        return layout;
    }

    private void onSaveSite() {
        try {
            VpnSite curSite = siteBinder.getBean();
            if (curSite != null) {
                logger.info("Saving " + curSite.toString());
                openVpnSiteSettings.getSites().put(curSite.getId(), curSite);
            }
            openVpnSiteSettings.save(settings);
            openVpnRestController.writeOpenVpnSiteServerConfig();
            openVpnRestController.writeOpenVpnSiteServerSitesConfig();
            sites.setItems(openVpnSiteSettings.getVpnSites());
            logger.info(openVpnSiteSettings.getVpnSites().toString());
            siteModified = false;
            sites.setValue(curSite);
        } catch (SettingsException ex) {
            logger.error("Cannot save openvpn site vpn: " + ex.getMessage());
        }
    }

    private Dialog createRemoteConfigDialog(String cfg) {
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
        boolean isDefaultSiteSelected
                = e.getValue() != null && e.getValue().getId() == 0;

        if (siteModified) {
            logger.info("Site modified");
            if (e.isFromClient()) {
                ConfirmDialog dlg = new ConfirmDialog(
                        "Unsaved Changes",
                        "Site has unsaved changes. Save now?",
                        "Save", (ce) -> {
                            VpnSite modValue = siteBinder.getBean();
                            binder.getBean().getSites().put(modValue.getId(), modValue);
                            sites.setItems(binder.getBean().getVpnSites());
                            sites.setValue(e.getValue());

                            siteBinder.setBean(e.getValue());

                            siteModified = false;
                        },
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
            logger.info("Site not modified");
            if (e.getValue() != null) {
                siteBinder.setBean(e.getValue());
            }
        }
        siteBinder.validate();
        siteBinder.refreshFields();
        if (e.getValue() != null) {
            logger.info("Selected site: " + e.getValue().toString());
        } else {
            logger.info("No site selected");
        }
        enableNonDefaultCopmponents(isDefaultSiteSelected);
    }
}
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
import at.nieslony.arachne.utils.net.NicInfo;
import at.nieslony.arachne.utils.net.NicUtils;
import at.nieslony.arachne.utils.net.TransportProtocol;
import at.nieslony.arachne.utils.validators.ConditionalValidator;
import at.nieslony.arachne.utils.validators.HostnameValidator;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "openvpn-site2site", layout = ViewTemplate.class)
@PageTitle("OpenVPN Site 2 Site | Arachne")
@RolesAllowed("ADMIN")
public class OpenVpnSiteView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnSiteView.class);

    private final Binder<OpenVpnSiteSettings> binder;
    private final Binder<VpnSite> siteBinder;
    private final OpenVpnSiteSettings openVpnSiteSettings;
    private final Settings settings;
    private final OpenVpnRestController openVpnRestController;

    private boolean siteModified = false;

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

    public OpenVpnSiteView(
            Settings settings,
            OpenVpnRestController openVpnRestController
    ) {
        this.settings = settings;
        this.openVpnRestController = openVpnRestController;

        binder = new Binder<>(OpenVpnSiteSettings.class);
        siteBinder = new Binder<>(VpnSite.class);
        openVpnSiteSettings = settings.getSettings(OpenVpnSiteSettings.class);

        TabSheet tabs = new TabSheet();
        tabs.add("Basics", createBasicsPage());
        tabs.add("Clients", createClientsPage());
        tabs.setWidthFull();

        Button saveButton = new Button("Save", (t) -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(tabs, saveButton);

        binder.setBean(openVpnSiteSettings);
        binder.validate();
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

        TextField clientNetwork = new TextField("Client Network");
        clientNetwork.setValueChangeMode(ValueChangeMode.EAGER);
        binder.bind(
                clientNetwork,
                OpenVpnSiteSettings::getClientNetwork,
                OpenVpnSiteSettings::setClientNetwork
        );

        Select<NetMask> clientMask = new Select<>();
        clientMask.setItems(
                IntStream
                        .range(1, 32)
                        .boxed()
                        .map(i -> new NetMask(i))
                        .collect(Collectors.toList())
        );
        clientMask.setLabel("Subnet Mask");
        binder.forField(clientMask)
                .bind(
                        (source) -> {
                            int mask = source.getClientMask();
                            return new NetMask(mask);
                        },
                        (s, v) -> {
                            s.setClientMask(v.getBits());
                        }
                );

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
        layout.add(
                listenLayout,
                connectToHost,
                interfaceLayout,
                clientNetLayout,
                keepaliveLayout,
                mtuTestField
        );

        return layout;
    }

    private Component createClientsPage() {
        VerticalLayout layout = new VerticalLayout();

        Select<VpnSite> sites = new Select<>();
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

        Button deleteButton = new Button("Delete", (e) -> {
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

        Button addButton = new Button("Add...", (e) -> {
            setNameDescDialog(null, (site) -> {
                sites.setItems(openVpnSiteSettings.getVpnSites());
                sites.setValue(site);
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

        TabSheet siteSettingsTab = new TabSheet();
        siteSettingsTab.add("Connection", createClientConnectionPage());
        siteSettingsTab.add("DNS", createDnsPage());
        siteSettingsTab.add("Routes", createRoutesTab());

        layout.add(
                sitesLayout,
                siteSettingsTab
        );
        layout.setMargin(false);
        layout.setPadding(false);

        sites.addValueChangeListener((e) -> {
            deleteButton.setEnabled(
                    e.getValue() != null && e.getValue().getId() != 0
            );
            if (siteModified) {
                if (e.isFromClient()) {
                    ConfirmDialog dlg = new ConfirmDialog(
                            "Unsaved Changes",
                            "Site has unsaved changes. Save now?",
                            "Save", (ce) -> {
                                VpnSite modValue = siteBinder.getBean();
                                binder.getBean().getSites().put(modValue.getId(), modValue);
                                sites.setItems(binder.getBean().getVpnSites());
                                sites.setValue(e.getValue());

                                VpnSite tmpSite = e.getValue().toBuilder().build();
                                siteBinder.setBean(tmpSite);

                                siteModified = false;
                            },
                            "Reject", (ce) -> {
                                siteModified = false;

                                VpnSite tmpSite = e.getValue().toBuilder().build();
                                siteBinder.setBean(tmpSite);
                            },
                            "Cancel", (ce) -> {
                                sites.setValue(e.getOldValue());
                            }
                    );
                    dlg.open();
                }
            } else {
                if (e.getValue() != null) {
                    VpnSite tmpSite = e.getValue().toBuilder().build();
                    siteBinder.setBean(tmpSite);
                }
            }
            setInheritBoxesVisible(siteBinder.getBean().getId() != 0);
            siteBinder.validate();
        });

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
        dnsServers = new EditableListBox("DNS Servers");
        siteBinder.bind(
                dnsServers,
                (source) -> source.getPushDnsServers(),
                (source, value) -> {
                    source.setPushDnsServers(value);
                });
        inheritDnsServers = new Checkbox("Inherit");
        inheritDnsServers.addValueChangeListener((e) -> {
            dnsServers.setEnabled(!e.getValue() || siteBinder.getBean().getId() == 0);
        });
        siteBinder.bind(
                inheritDnsServers,
                VpnSite::isInheritDnsServers,
                VpnSite::setInheritDnsServers
        );

        pushDomains = new EditableListBox("Push Domains");
        siteBinder.bind(
                pushDomains,
                (source) -> source.getPushSearchDomains(),
                (source, value) -> source.setPushSearchDomains(value)
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

        return layout;
    }

    private Component createRoutesTab() {
        pushRoutes = new EditableListBox("Push Routes");
        siteBinder.bind(
                pushRoutes,
                (source) -> source.getPushRoutes(),
                (source, value) -> source.setPushRoutes(value)
        );
        inheritPushRoutes = new Checkbox("Inherit");
        inheritPushRoutes.addValueChangeListener((e) -> {
            pushRoutes.setEnabled(!e.getValue() || siteBinder.getBean().getId() == 0);
        });
        siteBinder.bind(
                inheritPushRoutes,
                VpnSite::isInheritPushRoutes,
                VpnSite::setInheritPushRoutes
        );

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

        VerticalLayout layout = new VerticalLayout(
                inheritPushRoutes,
                pushRoutes,
                new HorizontalLayout(
                        routeInternet,
                        inheritRouteInternet
                )
        );

        return layout;
    }

    private Component createClientConnectionPage() {
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

        layout.add(
                remoteHostField,
                preSharedKeyField,
                createPSKButton
        );

        return layout;
    }

    private void save() {
        try {
            VpnSite curSite = siteBinder.getBean();
            if (curSite != null) {
                openVpnSiteSettings.getSites().put(curSite.getId(), curSite);
            }
            openVpnSiteSettings.save(settings);
            openVpnRestController.writeOpenVpnSiteServerConfig();
        } catch (SettingsException ex) {
            logger.error("Cannot save openvpn site vpn: " + ex.getMessage());
        }
    }

    private void setInheritBoxesVisible(boolean visible) {
        remoteHostField.setEnabled(visible);
        preSharedKeyField.setEnabled(visible);

        inheritDnsServers.setVisible(visible);
        inheritPushDomains.setVisible(visible);
        inheritPushRoutes.setVisible(visible);
        inheritRouteInternet.setVisible(visible);
    }
}

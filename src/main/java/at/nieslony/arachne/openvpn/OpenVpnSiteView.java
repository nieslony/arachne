/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.ViewTemplate;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.net.NetMask;
import at.nieslony.arachne.utils.net.NicInfo;
import at.nieslony.arachne.utils.net.NicUtils;
import at.nieslony.arachne.utils.net.TransportProtocol;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author claas
 */
@Route(value = "openvpn-site2site", layout = ViewTemplate.class)
@PageTitle("OpenVPN Site 2 Site | Arachne")
@RolesAllowed("ADMIN")
public class OpenVpnSiteView extends VerticalLayout {

    private final Binder<OpenVpnSiteSettings> binder;
    private final OpenVpnSiteSettings openVpnSiteSettings;

    public OpenVpnSiteView(Settings settings) {
        binder = new Binder<>(OpenVpnSiteSettings.class);
        openVpnSiteSettings = settings.getSettings(OpenVpnSiteSettings.class);

        TabSheet tabs = new TabSheet();
        tabs.add("Basics", createBasicsPage());
        tabs.add("Clients", createClientsPage());
        tabs.setWidthFull();

        Button saveButton = new Button("Save");
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

        Select<OpenVpnSiteSettings.VpnSite> sites = new Select<>();
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
        siteSettingsTab.add("DNS", new VerticalLayout());
        siteSettingsTab.add("Routes", new VerticalLayout());

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
        });

        sites.setValue(openVpnSiteSettings.getVpnSite(0));
        return layout;
    }

    private void setNameDescDialog(
            OpenVpnSiteSettings.VpnSite site,
            Consumer<OpenVpnSiteSettings.VpnSite> onOk
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
}

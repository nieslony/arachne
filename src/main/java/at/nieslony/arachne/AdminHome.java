/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.openvpn.OpenVpnSiteSettings;
import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.openvpn.VpnSiteRepository;
import at.nieslony.arachne.openvpn.management.ManagementException;
import at.nieslony.arachne.openvpn.management.OpenVpnManagement;
import at.nieslony.arachne.openvpn.management.OpenVpnManagementService;
import at.nieslony.arachne.openvpn.management.commands.Status;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.components.YesNoIcon;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author claas
 */
@Route(value = "admin-home", layout = ViewTemplate.class)
@PageTitle("Admin Dashboard")
@RolesAllowed("ADMIN")
@Slf4j
public class AdminHome
        extends VerticalLayout
        implements BeforeEnterObserver, BeforeLeaveObserver {

    @Autowired
    private OpenVpnManagementService openVpnManagementService;

    @Autowired
    VpnSiteRepository vpnSiteRepository;

    @Autowired
    Settings settings;

    private OpenVpnUserSettings openVpnUserSettings;
    private OpenVpnSiteSettings openVpnSiteSettings;
    private Grid<Status.ConnectionStatus> connectedSitesGrid;
    private Grid<Status.ConnectionStatus> connectedUsersGrid;
    private Span msgConnectedUsers;
    private Span msgConnectedSites;

    Text serverVersion;

    @PostConstruct
    public void init() {
        openVpnUserSettings = settings.getSettings(OpenVpnUserSettings.class);
        openVpnSiteSettings = settings.getSettings(OpenVpnSiteSettings.class);

        serverVersion = new Text("");

        Accordion content = new Accordion();
        content.add("Connected Users", createConnectedUsersView());
        content.add("Connected Sites", createConnectedSitesView());
        content.setWidthFull();
        add(
                serverVersion,
                content
        );

        setPadding(false);

        onUpdateServerVersion();
        onRefreshConnectedUsers();
        onRefreshConnectedSites();
    }

    private void onUpdateServerVersion() {
        String versionStr;
        try {
            versionStr = openVpnManagementService
                    .getUserManagement()
                    .version()
                    .longVersion();
        } catch (ManagementException ex) {
            log.error("Cannot connect to management interface: " + ex.getMessage());
            versionStr = "unknown version";
        }

        serverVersion.setText("Running " + versionStr);
    }

    private static String createMsgConnectedUsers(int count) {
        return "%d users connected".formatted(count);
    }

    private void onRefreshConnectedUsers() {
        if (openVpnUserSettings.isAlreadyConfigured()) {
            OpenVpnManagement mgmt = openVpnManagementService.getSiteManagement();
            switch (mgmt.getManagementConnectionStatus()) {
                case Disconnected -> {
                    msgConnectedSites.setText("No connection to Management Interface");
                    connectedSitesGrid.setItems();
                }
                case Hold -> {
                    msgConnectedSites.setText("OpenVpn is in Hold Status");
                    connectedSitesGrid.setItems();
                }
                case Connected -> {
                    try {
                        var status = openVpnManagementService.getUserManagement().status();
                        connectedUsersGrid.setItems(status.connectionStatus());
                        msgConnectedUsers.setText(
                                "%d Users connected".formatted(status.connectionStatus().size())
                        );
                    } catch (ManagementException ex) {
                        log.error("Error getting connected users: " + ex.getMessage());
                        msgConnectedUsers.setText("Cannot get connected users: " + ex.getMessage());
                    }
                }
            }
        } else {
            msgConnectedUsers.setText("User VPN not yet configured");
        }
    }

    private void onRefreshConnectedSites() {
        if (openVpnSiteSettings.isAlreadyConfigured()) {
            OpenVpnManagement mgmt = openVpnManagementService.getSiteManagement();
            switch (mgmt.getManagementConnectionStatus()) {
                case Disconnected -> {
                    msgConnectedSites.setText("No connection to Management Interface");
                    connectedSitesGrid.setItems();
                }
                case Hold -> {
                    msgConnectedSites.setText("OpenVpn is in Hold Status");
                    connectedSitesGrid.setItems();
                }
                case Connected -> {
                    try {
                        log.info("Gettings all configured sites");
                        var status = mgmt.status();
                        msgConnectedSites.setText("%d of %d sites connected".formatted(
                                status.connectionStatus().size(),
                                vpnSiteRepository.count() - 1
                        ));
                        Set<String> siteNames = new HashSet<>();
                        status.connectionStatus().forEach(
                                site -> siteNames.add(site.username())
                        );
                        vpnSiteRepository.findAll()
                                .forEach(site -> {
                                    String siteName = site.getSiteHostname();
                                    if (!siteNames.contains(siteName)
                                            && !site.isDefaultSite()) {
                                        log.debug("Site %s is not connected".formatted(siteName));
                                        status
                                                .connectionStatus()
                                                .add(
                                                        Status.ConnectionStatus
                                                                .notConnected(siteName)
                                                );
                                    }
                                });
                        log.debug("Connextion status: " + status.connectionStatus().toString());
                        connectedSitesGrid.setItems(status.connectionStatus());
                    } catch (ManagementException ex) {
                        log.error("Error getting connected sites: " + ex.getMessage());
                        msgConnectedSites.setText("Error: " + ex.getMessage());
                    }
                }
            }
        } else {
            msgConnectedSites.setText("Site VPN not yet configured");
        }
        log.info("Gettings all configured sites (Done)");
    }

    private Component createConnectedUsersView() {
        VerticalLayout layout = new VerticalLayout();

        Button refreshButton = new Button("Refresh", (e) -> onRefreshConnectedUsers());
        msgConnectedUsers = new Span("");

        HorizontalLayout headerLayout = new HorizontalLayout(
                refreshButton,
                msgConnectedUsers
        );
        headerLayout.setPadding(false);
        headerLayout.setMargin(false);
        headerLayout.setAlignItems(Alignment.BASELINE);

        connectedUsersGrid = new Grid<>();
        connectedUsersGrid.addColumn(Status.ConnectionStatus::username)
                .setHeader("Common Name");
        connectedUsersGrid.addColumn(
                source -> DecimalFormat
                        .getInstance()
                        .format(source.byteReceived()))
                .setHeader("Bytes Received")
                .setTextAlign(ColumnTextAlign.END);
        connectedUsersGrid.addColumn(
                source -> DecimalFormat
                        .getInstance()
                        .format(source.bytesSent()))
                .setHeader("Bytes Sent")
                .setTextAlign(ColumnTextAlign.END);
        connectedUsersGrid.addColumn(
                source -> DateFormat
                        .getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                        .format(source.connectedSince()))
                .setHeader("Connected since");
        connectedUsersGrid.addColumn(source -> source.realAddress().getHostAddress())
                .setHeader("Real Address");
        connectedUsersGrid.addColumn(source -> source.virtualAddress().getHostAddress())
                .setHeader("Virtual Address");

        layout.add(
                headerLayout,
                connectedUsersGrid
        );
        layout.setPadding(false);

        return layout;
    }

    private Component createConnectedSitesView() {
        VerticalLayout layout = new VerticalLayout();

        Button refreshButton = new Button("Refresh", (e) -> onRefreshConnectedSites());
        msgConnectedSites = new Span("");

        HorizontalLayout headerLayout = new HorizontalLayout(
                refreshButton,
                msgConnectedSites
        );
        headerLayout.setPadding(false);
        headerLayout.setMargin(false);
        headerLayout.setAlignItems(Alignment.BASELINE);

        connectedSitesGrid = new Grid<>();
        connectedSitesGrid.addColumn(Status.ConnectionStatus::username)
                .setHeader("Site Name");
        connectedSitesGrid.addColumn(new ComponentRenderer<>(
                (var site) -> {
                    YesNoIcon icon = new YesNoIcon();
                    icon.setValue(site.connectedSince() != null);
                    return icon;
                }))
                .setHeader("Connected")
                .setAutoWidth(true)
                .setFlexGrow(0);
        connectedSitesGrid.addColumn(
                site -> site.connectedSince() != null
                ? DecimalFormat
                        .getInstance()
                        .format(site.byteReceived())
                : "")
                .setHeader("Bytes Received");
        connectedSitesGrid.addColumn(
                site -> site.connectedSince() != null
                ? DecimalFormat
                        .getInstance()
                        .format(site.bytesSent())
                : "")
                .setHeader("Bytes Sent");
        connectedSitesGrid.addColumn(
                site -> site.connectedSince() != null
                ? DateFormat
                        .getDateTimeInstance(
                                DateFormat.SHORT,
                                DateFormat.MEDIUM
                        )
                        .format(site.connectedSince()
                        )
                : "")
                .setHeader("Connected since");
        connectedSitesGrid.addColumn(
                site -> site.connectedSince() != null
                ? site.realAddress().getHostAddress()
                : "")
                .setHeader("Real Address");
        connectedSitesGrid.addColumn(
                site -> site.connectedSince() != null
                ? site.virtualAddress().getHostAddress()
                : "")
                .setHeader("Virtual Address");

        layout.add(
                headerLayout,
                connectedSitesGrid
        );
        layout.setPadding(false);

        return layout;
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        log.info("About to leave, removing status change listener");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent bee) {
    }
}

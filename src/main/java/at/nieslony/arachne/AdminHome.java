/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.openvpn.OpenVpnSiteSettings;
import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.openvpn.VpnSite;
import at.nieslony.arachne.openvpn.VpnSiteRepository;
import at.nieslony.arachne.openvpnmanagement.ArachneDbus;
import at.nieslony.arachne.openvpnmanagement.IFaceConnectedClient;
import at.nieslony.arachne.openvpnmanagement.IFaceOpenVpnStatus;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.components.YesNoIcon;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
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
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;

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

    class ConnectedUsersListener implements Consumer<IFaceOpenVpnStatus> {

        private final UI ui;
        private final Grid<IFaceConnectedClient> grid;

        ConnectedUsersListener(UI ui, Grid<IFaceConnectedClient> grid) {
            this.ui = ui;
            this.grid = grid;
        }

        @Override
        public void accept(IFaceOpenVpnStatus status) {
            try {
                ui.access(() -> {
                    var clients = status.getConnectedClients();
                    grid.setItems(clients);
                    msgConnectedUsers.setText(createMsgConnectedUsers(
                            status.getConnectedClients().size()
                    ));
                    ui.push();
                });
            } catch (UIDetachedException ex) {
                log.warn("Cannot up date grid: UI is detached");
            }
        }
    };

    class SiteStatus {

        private final VpnSite vpnSite;
        private final IFaceConnectedClient connectedClient;

        SiteStatus(VpnSite vpnSite, IFaceConnectedClient connectedClient) {
            this.vpnSite = vpnSite;
            this.connectedClient = connectedClient;
        }

        public VpnSite getVpnSite() {
            return vpnSite;
        }

        public IFaceConnectedClient getConnectedClient() {
            return connectedClient;
        }

        public boolean isConnected() {
            return connectedClient != null;
        }
    }

    class ConnectedSitesListener implements Consumer<IFaceOpenVpnStatus> {

        private final UI ui;
        private final Grid<SiteStatus> grid;

        ConnectedSitesListener(UI ui, Grid<SiteStatus> grid) {
            this.ui = ui;
            this.grid = grid;
        }

        @Override
        public void accept(IFaceOpenVpnStatus status) {
            var knownSites = vpnSiteRepository.findAll();
            var connectedSites = status.getConnectedClients();
            log.debug(
                    "Connected sites on %s: %s"
                            .formatted(
                                    status.getTimeAsDate().toString(),
                                    connectedSites.toString()
                            )
            );

            List<SiteStatus> statusList = new LinkedList<>();
            for (var site : knownSites) {
                if (!site.isDefaultSite()) {
                    var client = connectedSites.stream()
                            .filter((s)
                                    -> s
                                    .getCommonName()
                                    .equals(site.getSiteHostname())
                            )
                            .findFirst()
                            .orElse(null);
                    var siteStatus = new SiteStatus(site, client);
                    log.debug("Site is connected: %b %s"
                            .formatted(siteStatus.isConnected(), site.toString())
                    );
                    statusList.add(siteStatus);
                }
            }
            try {
                ui.access(() -> {
                    grid.setItems(statusList);
                    msgConnectedSites.setText("%d/%d sites connected"
                            .formatted(status.getConnectedClients().size(),
                                    vpnSiteRepository.count() - 1
                            ));
                    ui.push();
                });
            } catch (UIDetachedException ex) {
                log.warn("Cannot up date grid: UI is detached");
            }
        }
    }

    private final VpnSiteRepository vpnSiteRepository;
    private final ArachneDbus arachneDbus;
    private final OpenVpnUserSettings openVpnUserSettings;
    private final OpenVpnSiteSettings openVpnSiteSettings;
    private Grid<IFaceConnectedClient> connectedUsersGrid;
    private Grid<SiteStatus> connectedSitesGrid;
    private Span msgConnectedUsers;
    private Span msgConnectedSites;
    private final Consumer<IFaceOpenVpnStatus> updateConnectedUserListener;
    private final Consumer<IFaceOpenVpnStatus> updateConnectedSitesListener;

    public AdminHome(
            ArachneDbus arachneDbus,
            VpnSiteRepository vpnSiteRepository,
            Settings settings
    ) {
        this.arachneDbus = arachneDbus;
        this.vpnSiteRepository = vpnSiteRepository;
        this.openVpnUserSettings = settings.getSettings(OpenVpnUserSettings.class);
        this.openVpnSiteSettings = settings.getSettings(OpenVpnSiteSettings.class);

        Accordion content = new Accordion();
        content.add("Connected Users", createConnectedUsersView());
        content.add("Connected Sites", createConnectedSitesView());
        content.setWidthFull();
        add(content);

        this.updateConnectedUserListener = new ConnectedUsersListener(UI.getCurrent(), connectedUsersGrid);
        this.updateConnectedSitesListener = new ConnectedSitesListener(UI.getCurrent(), connectedSitesGrid);
        setPadding(false);

        onRefreshConnectedUsers();
        onRefreshConnectedSites();
    }

    @PostConstruct
    public void init() {
        addDetachListener((t) -> {
            log.info("Detaching from AdminHome");
            removeListeners();
            log.info("Detached");
        });
        addAttachListener((t) -> {
            addListeners();
        });
    }

    private static String createMsgConnectedUsers(int count) {
        return "%d users connected".formatted(count);
    }

    private void onRefreshConnectedUsers() {
        try {
            if (openVpnUserSettings.isAlreadyConfigured()) {
                var status = arachneDbus.getServerStatus(ArachneDbus.ServerType.USER);
                updateConnectedUserListener.accept(status);
            } else {
                msgConnectedUsers.setText("User VPN not yet configured");
            }
        } catch (DBusException | DBusExecutionException ex) {
            log.error("Error getting connected users: " + ex.getMessage());
            msgConnectedUsers.setText("DBusError: " + ex.getMessage());
        }
    }

    private void onRefreshConnectedSites() {
        try {
            if (openVpnSiteSettings.isAlreadyConfigured()) {
                var status = arachneDbus.getServerStatus(ArachneDbus.ServerType.SITE);
                updateConnectedSitesListener.accept(status);
            } else {
                msgConnectedSites.setText("Site VPN not yet configured");
            }
        } catch (DBusException | DBusExecutionException ex) {
            log.error("DBus Error: " + ex.getMessage());
            msgConnectedSites.setText("DBusError: " + ex.getMessage());
        }
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
        connectedUsersGrid.addColumn(IFaceConnectedClient::getCommonName)
                .setHeader("Common Name");
        connectedUsersGrid.addColumn(
                source -> DecimalFormat
                        .getInstance()
                        .format(source.getBytesReceived()))
                .setHeader("Bytes Received")
                .setTextAlign(ColumnTextAlign.END);
        connectedUsersGrid.addColumn(
                source -> DecimalFormat
                        .getInstance()
                        .format(source.getBytesSent()))
                .setHeader("Bytes Sent")
                .setTextAlign(ColumnTextAlign.END);
        connectedUsersGrid.addColumn((source) -> DateFormat
                .getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                .format(source.getConnectedSinceAsDate())
        );
        connectedUsersGrid.addColumn(IFaceConnectedClient::getRealAddress)
                .setHeader("Real Address");
        connectedUsersGrid.addColumn(IFaceConnectedClient::getVirtualAddress)
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
        connectedSitesGrid.addColumn(site -> site.getVpnSite().getName())
                .setHeader("Site Name");
        connectedSitesGrid.addColumn(new ComponentRenderer<>(
                (var site) -> {
                    YesNoIcon icon = new YesNoIcon();
                    icon.setValue(site.isConnected());
                    return icon;
                }))
                .setHeader("Connected")
                .setAutoWidth(true)
                .setFlexGrow(0);
        connectedSitesGrid.addColumn(
                site -> site.isConnected()
                ? DecimalFormat
                        .getInstance()
                        .format(site.getConnectedClient().getBytesReceived())
                : "")
                .setHeader("Bytes Received");
        connectedSitesGrid.addColumn(
                site -> site.isConnected()
                ? DecimalFormat
                        .getInstance()
                        .format(site.getConnectedClient().getBytesSent())
                : "")
                .setHeader("Bytes Sent");
        connectedSitesGrid.addColumn(
                site -> site.isConnected()
                ? DateFormat
                        .getDateTimeInstance(
                                DateFormat.SHORT,
                                DateFormat.MEDIUM
                        )
                        .format(site
                                .getConnectedClient()
                                .getConnectedSinceAsDate()
                        )
                : "")
                .setHeader("Connected Since");
        connectedSitesGrid.addColumn(
                site -> site.isConnected()
                ? site.getConnectedClient()
                        .getRealAddress()
                        .split(":")[0]
                : "")
                .setHeader("Real Address");
        connectedSitesGrid.addColumn(
                site -> site.isConnected()
                ? site.getConnectedClient().getVirtualAddress()
                : "")
                .setHeader("Virtual Address");

        layout.add(
                headerLayout,
                connectedSitesGrid
        );
        layout.setPadding(false);

        return layout;
    }

    public void removeListeners() {
        if (openVpnUserSettings.isAlreadyConfigured()) {
            arachneDbus.removeServerStatusChangedListener(
                    ArachneDbus.ServerType.USER,
                    updateConnectedUserListener
            );
        }
        if (openVpnSiteSettings.isAlreadyConfigured()) {
            arachneDbus.removeServerStatusChangedListener(
                    ArachneDbus.ServerType.SITE,
                    updateConnectedSitesListener
            );
        }
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        log.debug("About to leave admin-home, removing connected users/sites listeners");
        removeListeners();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent bee) {
        addListeners();
    }

    private void addListeners() {
        if (openVpnUserSettings.isAlreadyConfigured()) {
            arachneDbus.addServerStatusChangedListener(
                    ArachneDbus.ServerType.USER,
                    updateConnectedUserListener
            );
        }
        if (openVpnSiteSettings.isAlreadyConfigured()) {
            arachneDbus.addServerStatusChangedListener(
                    ArachneDbus.ServerType.SITE,
                    updateConnectedSitesListener
            );
        }
    }
}

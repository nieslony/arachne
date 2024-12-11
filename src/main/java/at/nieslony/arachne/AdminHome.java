/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.openvpn.VpnSite;
import at.nieslony.arachne.openvpn.VpnSiteRepository;
import at.nieslony.arachne.openvpnmanagement.ArachneDbus;
import at.nieslony.arachne.openvpnmanagement.IFaceConnectedClient;
import at.nieslony.arachne.openvpnmanagement.IFaceOpenVpnStatus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "admin-home", layout = ViewTemplate.class)
@PageTitle("Admin Dashboard")
@RolesAllowed("ADMIN")
public class AdminHome
        extends VerticalLayout
        implements BeforeEnterObserver, BeforeLeaveObserver {

    private static final Logger logger = LoggerFactory.getLogger(AdminHome.class);

    class ConnectedClientsListener implements Consumer<IFaceOpenVpnStatus> {

        private final UI ui;
        private final Grid<IFaceConnectedClient> grid;

        ConnectedClientsListener(UI ui, Grid<IFaceConnectedClient> grid) {
            this.ui = ui;
            this.grid = grid;
        }

        @Override
        public void accept(IFaceOpenVpnStatus status) {
            ui.access(() -> {
                var clients = status.getConnectedClients();
                grid.setItems(clients);
                msgConnectedUsers.setText(createMsgConnectedUsers(
                        status.getConnectedClients().size()
                ));
                ui.push();
            });
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

            List<SiteStatus> statusList = new LinkedList<>();
            for (var site : knownSites) {
                if (!site.isDefaultSite()) {
                    logger.info(connectedSites.toString());
                    logger.info(site.toString());
                    var client = connectedSites.stream()
                            .filter((s) -> s.getCommonName().equals(site.getSiteHostname()))
                            .findFirst()
                            .orElse(null);
                    statusList.add(new SiteStatus(site, client));
                }
            }
            grid.setItems(statusList);
        }
    }

    private final VpnSiteRepository vpnSiteRepository;
    private final ArachneDbus arachneDbus;
    private Grid<IFaceConnectedClient> connectedUsersGrid;
    private Grid<SiteStatus> connectedSitesGrid;
    private Span msgConnectedUsers;
    private Span msgConnectedSites;
    private final Consumer<IFaceOpenVpnStatus> updateConnectedUserListener;
    private final Consumer<IFaceOpenVpnStatus> updateConnectedSitesListener;

    public AdminHome(ArachneDbus arachneDbus, VpnSiteRepository vpnSiteRepository) {
        this.arachneDbus = arachneDbus;
        this.vpnSiteRepository = vpnSiteRepository;

        Accordion content = new Accordion();
        content.add("Connected Users", createConnectedUsersView());
        content.add("Connected Sites", createConnectedSitesView());
        content.setWidthFull();
        add(content);

        this.updateConnectedUserListener = new ConnectedClientsListener(UI.getCurrent(), connectedUsersGrid);
        this.updateConnectedSitesListener = new ConnectedSitesListener(UI.getCurrent(), connectedSitesGrid);
        setPadding(false);

        onRefreshConnectedUsers();
        onRefreshConnectedSites();
    }

    @PostConstruct
    public void init() {
        addDetachListener((t) -> {
            logger.info("Detach");
            arachneDbus.removeServerStatusChangedListener(
                    ArachneDbus.ServerType.USER,
                    updateConnectedUserListener
            );
            arachneDbus.removeServerStatusChangedListener(
                    ArachneDbus.ServerType.SITE,
                    updateConnectedUserListener
            );
        });
    }

    private static String createMsgConnectedUsers(int count) {
        return "%d users connected".formatted(count);
    }

    private void onRefreshConnectedUsers() {
        try {
            var status = arachneDbus.getServerStatus(ArachneDbus.ServerType.USER);
            updateConnectedUserListener.accept(status);
            msgConnectedUsers.setText(createMsgConnectedUsers(status.getConnectedClients().size()));
        } catch (DBusException | DBusExecutionException ex) {
            logger.error("Error getting connected users: " + ex.getMessage());
            msgConnectedUsers.setText("DBusError: " + ex.getMessage());
        }
    }

    private void onRefreshConnectedSites() {
        try {
            var status = arachneDbus.getServerStatus(ArachneDbus.ServerType.SITE);
            updateConnectedSitesListener.accept(status);
            msgConnectedSites.setText("%d/%d sites connected"
                    .formatted(status.getConnectedClients().size(),
                            vpnSiteRepository.count() - 1
                    ));
        } catch (DBusException | DBusExecutionException ex) {
            logger.error("DBus Error: " + ex.getMessage());
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
        connectedUsersGrid.addColumn(IFaceConnectedClient::getBytesReceived)
                .setHeader("Bytes Received")
                .setTextAlign(ColumnTextAlign.END);
        connectedUsersGrid.addColumn(IFaceConnectedClient::getBytesSent)
                .setHeader("Bytes Sent")
                .setTextAlign(ColumnTextAlign.END);
        connectedUsersGrid.addColumn(IFaceConnectedClient::getConnectedSinceAsDate)
                .setHeader("Connected since");
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
        connectedSitesGrid.addColumn(
                new ComponentRenderer<>(site -> {
                    if (site.isConnected()) {
                        return VaadinIcon.CHECK.create();
                    } else {
                        return VaadinIcon.CLOSE_SMALL.create();
                    }
                }))
                .setHeader("Connected");
        connectedSitesGrid.addColumn(
                site -> site.isConnected()
                ? site.getConnectedClient().getBytesReceived()
                : "")
                .setHeader("Bytes Received");
        connectedSitesGrid.addColumn(
                site -> site.isConnected()
                ? site.getConnectedClient().getBytesSent()
                : "")
                .setHeader("Bytes Sent");
        connectedSitesGrid.addColumn(
                site -> site.isConnected()
                ? site.getConnectedClient().getConnectedSinceAsDate()
                : "")
                .setHeader("Connected Since");
        connectedSitesGrid.addColumn(
                site -> site.isConnected()
                ? site.getConnectedClient().getRealAddress()
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

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        arachneDbus.removeServerStatusChangedListener(
                ArachneDbus.ServerType.USER,
                updateConnectedUserListener
        );
        arachneDbus.removeServerStatusChangedListener(
                ArachneDbus.ServerType.SITE,
                updateConnectedSitesListener
        );
    }

    @Override
    public void beforeEnter(BeforeEnterEvent bee) {
        arachneDbus.addServerStatusChangedListener(
                ArachneDbus.ServerType.USER,
                updateConnectedUserListener
        );
        arachneDbus.addServerStatusChangedListener(
                ArachneDbus.ServerType.SITE,
                updateConnectedSitesListener
        );
    }
}

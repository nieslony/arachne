/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.openvpn.VpnSiteRepository;
import at.nieslony.arachne.openvpnmanagement.ArachneDbus;
import at.nieslony.arachne.openvpnmanagement.IFaceConnectedClient;
import at.nieslony.arachne.openvpnmanagement.IFaceOpenVpnStatus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
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
                ui.push();
            });
        }
    };

    private final VpnSiteRepository vpnSiteRepository;
    private final ArachneDbus arachneDbus;
    private Grid<IFaceConnectedClient> connectedUsersGrid;
    private Grid<IFaceConnectedClient> connectedSitesGrid;
    private final Consumer<IFaceOpenVpnStatus> updateConnectedUserListener;
    private final Consumer<IFaceOpenVpnStatus> updateConnectedSitesListener;

    public AdminHome(ArachneDbus arachneDbus, VpnSiteRepository vpnSiteRepository) {
        this.arachneDbus = arachneDbus;
        this.vpnSiteRepository = vpnSiteRepository;

        add(
                createConnectedUsersView(),
                createConnectedSitesView()
        );

        this.updateConnectedUserListener = new ConnectedClientsListener(UI.getCurrent(), connectedUsersGrid);
        this.updateConnectedSitesListener = new ConnectedClientsListener(UI.getCurrent(), connectedSitesGrid);
        setPadding(false);
    }

    @PostConstruct
    public void init() {
        addDetachListener((t) -> {
            logger.info("Detach");
            arachneDbus.removeServerStatusChangedListener(
                    ArachneDbus.ServerType.USER,
                    updateConnectedUserListener
            );
        });
    }

    private void onRefreshConnectedUsers() {
        try {
            var connectedUsers = arachneDbus.getServerStatus(ArachneDbus.ServerType.USER).getConnectedClients();
            connectedUsersGrid.setItems(connectedUsers);
        } catch (DBusException | DBusExecutionException ex) {
            logger.error("Error getting connected users: " + ex.getMessage());
        }
    }

    private void onRefreshConnectedSites() {
        vpnSiteRepository.findAll();
    }

    private Component createConnectedUsersView() {
        Button refreshButton = new Button("Refresh", (e) -> onRefreshConnectedUsers());

        VerticalLayout layout = new VerticalLayout();
        NativeLabel connectedUsersLabel = new NativeLabel("Connected Users");
        connectedUsersLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.BODY);

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

        onRefreshConnectedUsers();

        layout.add(
                refreshButton,
                connectedUsersLabel,
                connectedUsersGrid
        );
        layout.setPadding(false);

        return layout;
    }

    private Component createConnectedSitesView() {
        VerticalLayout layout = new VerticalLayout();

        Button refreshButton = new Button("Refresh", (e) -> onRefreshConnectedSites());

        NativeLabel connectedSitesLabel = new NativeLabel("Connected Sites");
        connectedSitesLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.BODY);

        connectedSitesGrid = new Grid<>();
        connectedSitesGrid.addColumn(IFaceConnectedClient::getCommonName)
                .setHeader("Common Name");
        connectedSitesGrid.addColumn(IFaceConnectedClient::getBytesReceived)
                .setHeader("Bytes Received")
                .setTextAlign(ColumnTextAlign.END);
        connectedSitesGrid.addColumn(IFaceConnectedClient::getBytesSent)
                .setHeader("Bytes Sent")
                .setTextAlign(ColumnTextAlign.END);
        connectedSitesGrid.addColumn(IFaceConnectedClient::getConnectedSinceAsDate)
                .setHeader("Connected since");
        connectedSitesGrid.addColumn(IFaceConnectedClient::getRealAddress)
                .setHeader("Real Address");
        connectedSitesGrid.addColumn(IFaceConnectedClient::getVirtualAddress)
                .setHeader("Virtual Address");

        layout.add(
                refreshButton,
                connectedSitesLabel,
                connectedSitesGrid
        );

        return layout;
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        arachneDbus.removeServerStatusChangedListener(
                ArachneDbus.ServerType.USER,
                updateConnectedUserListener
        );
    }

    @Override
    public void beforeEnter(BeforeEnterEvent bee) {
        arachneDbus.addServerStatusChangedListener(
                ArachneDbus.ServerType.USER,
                updateConnectedUserListener
        );
    }
}

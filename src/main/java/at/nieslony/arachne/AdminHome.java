/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

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
                logger.info("Updating users list: " + clients.toString());
                grid.setItems(clients);
                ui.push();
            });
        }
    };

    private final ArachneDbus arachneDbus;
    private Grid<IFaceConnectedClient> connectedUsersGrid;
    private final Consumer<IFaceOpenVpnStatus> updateConnectedUserListener;

    public AdminHome(ArachneDbus arachneDbus) {
        this.arachneDbus = arachneDbus;
        this.updateConnectedUserListener = new ConnectedClientsListener(UI.getCurrent(), connectedUsersGrid);

        add(createConnectedUsersView());
        setPadding(false);
    }

    @PostConstruct
    public void init() {
        addDetachListener((t) -> {
            logger.info("Detach");
            arachneDbus.removeServerUserStatusChangedListener(updateConnectedUserListener);
        });
    }

    private void onRefreshConnectedUsers() {
        try {
            var connectedUsers = arachneDbus.getServerStatus().getConnectedClients();
            connectedUsersGrid.setItems(connectedUsers);
        } catch (DBusException | DBusExecutionException ex) {
            logger.error("Error getting connected users: " + ex.getMessage());
        }
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

        layout.add(refreshButton, connectedUsersLabel, connectedUsersGrid);
        layout.setPadding(false);

        return layout;
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        arachneDbus.removeServerUserStatusChangedListener(updateConnectedUserListener);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent bee) {
        arachneDbus.addServerUserStatusChangedListener(updateConnectedUserListener);
    }
}

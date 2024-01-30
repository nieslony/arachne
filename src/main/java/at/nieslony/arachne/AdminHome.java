/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.openvpnmanagement.ArachneDbus;
import at.nieslony.arachne.openvpnmanagement.IFaceConnectedClient;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.Timer;
import java.util.TimerTask;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "admin-home", layout = ViewTemplate.class)
@PageTitle("Arachne")
@PermitAll
public class AdminHome
        extends VerticalLayout
        implements BeforeEnterObserver, BeforeLeaveObserver {

    private static final Logger logger = LoggerFactory.getLogger(AdminHome.class);

    private final ArachneDbus arachneDbus;
    private Timer refreshConnectedUsersTimer = null;
    private Grid<IFaceConnectedClient> connectedUsersGrid;
    private Select<Integer> autoRefreshSelect;

    private final String TIMER_NAME = "Refresh connected Users";

    public AdminHome(ArachneDbus arachneDbus) {
        this.arachneDbus = arachneDbus;

        add(createConnectedUsersView());

    }

    private TimerTask createRefreshConnectedUsersTask() {
        return new TimerTask() {
            @Override
            public void run() {
                onRefreshConnectedUsers();
            }
        };
    }

    private void onRefreshConnectedUsers() {
        try {
            var connectedUsers = arachneDbus.getServerStatus().getConnectedClients();
            connectedUsersGrid.setItems(connectedUsers);
        } catch (DBusException | DBusExecutionException ex) {
            logger.error(ex.getMessage());
        }
    }

    private Component createConnectedUsersView() {
        Button refreshButton = new Button("Refresh", (e) -> onRefreshConnectedUsers());

        autoRefreshSelect = new Select<>();
        autoRefreshSelect.setItemLabelGenerator(
                (i) -> i == 0
                        ? "No Auto Refresh"
                        : "Auto Refresh %d sec".formatted(i)
        );
        autoRefreshSelect.setItems(0, 10, 20, 30, 45, 60);
        autoRefreshSelect.setValue(30);
        autoRefreshSelect.addValueChangeListener((e) -> {
            if (refreshConnectedUsersTimer != null) {
                refreshConnectedUsersTimer.cancel();
                refreshConnectedUsersTimer.purge();
            }
            long delay = 1000L * autoRefreshSelect.getValue();
            if (delay != 0) {
                refreshConnectedUsersTimer = new Timer(TIMER_NAME);
                refreshConnectedUsersTimer.scheduleAtFixedRate(
                        createRefreshConnectedUsersTask(),
                        delay,
                        delay
                );
            } else {
                refreshConnectedUsersTimer = null;
            }
        });

        HorizontalLayout refreshLayout = new HorizontalLayout(
                refreshButton,
                autoRefreshSelect
        );
        refreshLayout.setMargin(false);
        refreshLayout.setSpacing(false);
        refreshLayout.setAlignItems(Alignment.BASELINE);

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

        layout.add(refreshLayout, connectedUsersLabel, connectedUsersGrid);

        return layout;
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (refreshConnectedUsersTimer != null) {
            refreshConnectedUsersTimer.cancel();
            refreshConnectedUsersTimer.purge();
        }
        refreshConnectedUsersTimer = null;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent bee) {
        long delay = 1000L * autoRefreshSelect.getValue();
        refreshConnectedUsersTimer = new Timer(TIMER_NAME);
        refreshConnectedUsersTimer.scheduleAtFixedRate(
                createRefreshConnectedUsersTask(),
                delay,
                delay);
    }
}

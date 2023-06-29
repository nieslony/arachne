/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.openvpnmanagement.ConnectedClient;
import at.nieslony.arachne.openvpnmanagement.OpenVpnManagement;
import at.nieslony.arachne.openvpnmanagement.OpenVpnManagementException;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route(value = "", layout = ViewTemplate.class)
@PageTitle("Arachne")
@PermitAll
public class MainView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    private final OpenVpnManagement openVpnManagement;

    public MainView(OpenVpnManagement openVpnManagement) {
        this.openVpnManagement = openVpnManagement;

        add(createConnectedUsersView());
    }

    private Component createConnectedUsersView() {
        VerticalLayout layout = new VerticalLayout();
        Grid<ConnectedClient> grid = new Grid<>();
        grid.addColumn(ConnectedClient::getUsername)
                .setHeader("Username");
        grid.addColumn(ConnectedClient::getBytesReceived)
                .setHeader("Bytes Received")
                .setTextAlign(ColumnTextAlign.END);
        grid.addColumn(ConnectedClient::getBytesSent)
                .setHeader("Bytes Sent")
                .setTextAlign(ColumnTextAlign.END);
        grid.addColumn(ConnectedClient::getConnectedSince)
                .setHeader("Connected since");
        grid.addColumn(ConnectedClient::getRealAddress)
                .setHeader("Real Address");
        grid.addColumn(ConnectedClient::getVirtualAddress)
                .setHeader("Virtual Address");

        try {
            grid.setItems(openVpnManagement.getConnectedUsers());
        } catch (OpenVpnManagementException ex) {
            logger.error(ex.getMessage());
        }

        layout.add(grid);

        return layout;
    }
}

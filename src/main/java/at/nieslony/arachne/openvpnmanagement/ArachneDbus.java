/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpnmanagement;

import jakarta.annotation.PostConstruct;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author claas
 */
@Service
public class ArachneDbus {

    private static final Logger logger = LoggerFactory.getLogger(ArachneDbus.class);

    private final String DBUS_BUS_NAME = "at.nieslony.Arachne";

    @Value("${dbusBusType}")
    private String dbusBusType;

    DBusConnection conn;

    public ArachneDbus() {

    }

    @PostConstruct
    public void init() {
        logger.info("Connection to dbus type: " + dbusBusType);
        try {
            conn = DBusConnection.getConnection(switch (dbusBusType) {
                case "session" -> {
                    yield DBusConnection.DBusBusType.SESSION;
                }
                case "system" -> {
                    yield DBusConnection.DBusBusType.SYSTEM;
                }
                default -> {
                    yield null;
                }
            });
        } catch (DBusException ex) {
            logger.error(
                    "Cannot connect to DBUS %s bus: %s"
                            .formatted(dbusBusType, ex.getMessage())
            );
        }
    }

    public void restart() throws DBusExecutionException, DBusException {
        IFaceServer arachneUser = conn.getRemoteObject(
                DBUS_BUS_NAME,
                "/UserVpn",
                IFaceServer.class);
        arachneUser.Restart();
    }
}

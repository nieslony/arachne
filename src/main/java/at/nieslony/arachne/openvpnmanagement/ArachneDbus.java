/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpnmanagement;

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
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
    private final String DBUS_OBJ_PATH_USERVPN = "/UserVpn";

    @Value("${dbusBusType}")
    private String dbusBusType;

    private DBusConnection conn;
    private IFaceServer arachneUser;
    private final Set<Consumer<IFaceOpenVpnStatus>> serverUserStatusListeners;
    private DBusSigHandler<IFaceServer.ServerStatusChanged> sigHandlerUserStatus;

    public ArachneDbus() {
        serverUserStatusListeners = new HashSet<>();

        sigHandlerUserStatus = (t) -> {
            serverUserStatusListeners.forEach((l) -> {
                l.accept(t.getServerStatus());
            });
        };
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
            this.arachneUser = conn.getRemoteObject(DBUS_BUS_NAME,
                    DBUS_OBJ_PATH_USERVPN,
                    IFaceServer.class);
        } catch (DBusException ex) {
            logger.error(
                    "Cannot connect to DBUS %s bus: %s"
                            .formatted(dbusBusType, ex.getMessage())
            );
        }
    }

    public void addServerUserStatusChangedListener(Consumer<IFaceOpenVpnStatus> listener) {
        if (serverUserStatusListeners.isEmpty()) {
            try {
                conn.addSigHandler(
                        IFaceServer.ServerStatusChanged.class,
                        arachneUser,
                        sigHandlerUserStatus
                );
            } catch (DBusException ex) {
                logger.error("Cannot listen on signal: " + ex.getMessage());
                return;
            }
        }
        serverUserStatusListeners.add(listener);
    }

    public void removeServerUserStatusChangedListener(Consumer<IFaceOpenVpnStatus> listener) {
        serverUserStatusListeners.remove(listener);
        if (serverUserStatusListeners.isEmpty()) {
            try {
                conn.removeSigHandler(
                        IFaceServer.ServerStatusChanged.class,
                        sigHandlerUserStatus
                );
            } catch (DBusException ex) {
                logger.error("Cannot remove signal: " + ex.getMessage());
            }
        }
    }

    public void restart() throws DBusExecutionException, DBusException {
        arachneUser.Restart();
    }

    public IFaceOpenVpnStatus getServerStatus() throws DBusExecutionException, DBusException {
        return arachneUser.ServerStatus();
    }
}

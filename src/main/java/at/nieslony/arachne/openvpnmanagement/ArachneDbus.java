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
    private IFaceServer arachneSite;
    private final Set<Consumer<IFaceOpenVpnStatus>> userServerStatusListeners;
    private final Set<Consumer<IFaceOpenVpnStatus>> siteServerStatusListeners;
    private final DBusSigHandler<IFaceServer.ServerStatusChanged> sigHandlerUserStatus;
    private final DBusSigHandler<IFaceServer.ServerStatusChanged> sigHandlerSiteStatus;

    public enum ServerType {
        USER,
        SITE
    }

    public ArachneDbus() {
        userServerStatusListeners = new HashSet<>();
        siteServerStatusListeners = new HashSet<>();

        sigHandlerUserStatus = (t) -> {
            userServerStatusListeners.forEach((l) -> {
                l.accept(t.getServerStatus());
            });
        };
        sigHandlerSiteStatus = (t) -> {
            siteServerStatusListeners.forEach((l) -> {
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

    public void addServerStatusChangedListener(
            ServerType serverType,
            Consumer<IFaceOpenVpnStatus> listener
    ) {
        var statusListener = switch (serverType) {
            case USER ->
                userServerStatusListeners;
            case SITE ->
                siteServerStatusListeners;
        };
        var signalHandlerStatus = switch (serverType) {
            case USER ->
                sigHandlerUserStatus;
            case SITE ->
                sigHandlerSiteStatus;
        };
        if (statusListener.isEmpty()) {
            logger.info("First listener added: Adding signal handler");
            try {
                conn.addSigHandler(
                        IFaceServer.ServerStatusChanged.class,
                        arachneUser,
                        signalHandlerStatus
                );
            } catch (DBusException ex) {
                logger.error("Cannot signal handler: " + ex.getMessage());
                return;
            }
        }
        userServerStatusListeners.add(listener);
    }

    public void removeServerStatusChangedListener(
            ServerType serverType,
            Consumer<IFaceOpenVpnStatus> listener
    ) {
        var statusListener = switch (serverType) {
            case USER ->
                userServerStatusListeners;
            case SITE ->
                siteServerStatusListeners;
        };
        var signalHandlerStatus = switch (serverType) {
            case USER ->
                sigHandlerUserStatus;
            case SITE ->
                sigHandlerSiteStatus;
        };
        statusListener.remove(listener);
        if (statusListener.isEmpty()) {
            logger.info("Last listener removed. Removing signal handler");
            try {
                conn.removeSigHandler(
                        IFaceServer.ServerStatusChanged.class,
                        signalHandlerStatus
                );
            } catch (DBusException ex) {
                logger.error("Cannot remove signal handler: " + ex.getMessage());
            }
        }
    }

    public void restartServer(ServerType serverType) throws DBusExecutionException, DBusException {
        switch (serverType) {
            case SITE ->
                arachneSite.Restart();
            case USER ->
                arachneUser.Restart();
        }
    }

    public IFaceOpenVpnStatus getServerStatus(ServerType serverType) throws DBusExecutionException, DBusException {
        return switch (serverType) {
            case USER ->
                arachneUser.ServerStatus();
            case SITE ->
                arachneSite.ServerStatus();
        };
    }
}

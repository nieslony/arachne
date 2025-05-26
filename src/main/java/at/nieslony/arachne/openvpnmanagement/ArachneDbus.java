/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpnmanagement;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author claas
 */
@Service
@Slf4j
public class ArachneDbus {

    private final String DBUS_BUS_NAME = "at.nieslony.Arachne";
    private final String DBUS_OBJ_PATH_USERVPN = "/UserVpn";
    private final String DBUS_OBJ_PATH_SITEVPN = "/SiteVpn";

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
        log.debug("Creating ArachneDbus service");
        userServerStatusListeners = new HashSet<>();
        siteServerStatusListeners = new HashSet<>();

        sigHandlerUserStatus = (t) -> {
            userServerStatusListeners.forEach((l) -> {
                log.debug("Listener: userServerStatus: " + l.toString());
                try {
                    l.accept(t.getServerStatus());
                } catch (Exception ex) {
                    log.error("Cannot send signal to %s: %s"
                            .formatted(l.toString(), ex.getMessage())
                    );
                }
                log.debug("End Signal: userServerStatus: " + l.toString());
            });
        };
        sigHandlerSiteStatus = (t) -> {
            siteServerStatusListeners.forEach((l) -> {
                log.debug("Listener: siteServerStatus: " + l.toString());
                try {
                    l.accept(t.getServerStatus());
                } catch (Exception ex) {
                    log.error("Cannot send signal to %s: %s"
                            .formatted(l.toString(), ex.getMessage())
                    );
                }
                log.debug("End Signal: siteServerStatus: " + l.toString());
            });
        };
    }

    @PostConstruct
    public void init() {
        log.info("Connection to dbus type: " + dbusBusType);
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
            this.arachneSite = conn.getRemoteObject(DBUS_BUS_NAME,
                    DBUS_OBJ_PATH_SITEVPN,
                    IFaceServer.class);
        } catch (DBusException ex) {
            log.error(
                    "Cannot connect to DBUS %s bus: %s"
                            .formatted(dbusBusType, ex.getMessage())
            );
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("Closing DBus connection");
        try {
            if (conn != null && conn.isConnected()) {
                try {
                    conn.removeSigHandler(
                            IFaceServer.ServerStatusChanged.class,
                            sigHandlerUserStatus);
                    conn.removeSigHandler(
                            IFaceServer.ServerStatusChanged.class,
                            sigHandlerSiteStatus);
                } catch (DBusException ex) {
                    log.warn("Cannot remove signal handler: " + ex.getMessage());
                }
                conn.close();
            }
        } catch (IOException ex) {
            log.error("Cannot close DBus connection: " + ex.getMessage());
        }
    }

    public synchronized void addServerStatusChangedListener(
            ServerType serverType,
            Consumer<IFaceOpenVpnStatus> listener
    ) {
        log.debug("Adding %s listerner".formatted(serverType.toString()));
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
        var server = switch (serverType) {
            case USER ->
                arachneUser;
            case SITE ->
                arachneSite;
        };
        if (statusListener.isEmpty()) {
            log.debug("First %s listener added: Adding signal handler"
                    .formatted(serverType.toString())
            );
            try {
                conn.addSigHandler(
                        IFaceServer.ServerStatusChanged.class,
                        server,
                        signalHandlerStatus
                );
            } catch (DBusException ex) {
                log.error("Cannot signal handler: " + ex.getMessage());
                return;
            }
        }
        statusListener.add(listener);
    }

    public synchronized void removeServerStatusChangedListener(
            ServerType serverType,
            Consumer<IFaceOpenVpnStatus> listener
    ) {
        log.debug("Removing %s listerner".formatted(serverType.toString()));
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
        var server = switch (serverType) {
            case USER ->
                arachneUser;
            case SITE ->
                arachneSite;
        };
        var oldCount = statusListener.size();
        statusListener.remove(listener);
        var newCount = statusListener.size();
        log.debug("Number of %s status listerners reduced: %d → %d"
                .formatted(serverType.toString(), oldCount, newCount)
        );
        if (statusListener.isEmpty()) {
            log.debug("Last %s listener removed. Removing signal handler"
                    .formatted(serverType.toString())
            );
            try {
                conn.removeSigHandler(
                        IFaceServer.ServerStatusChanged.class,
                        server,
                        signalHandlerStatus
                );
            } catch (DBusException ex) {
                log.error("Cannot remove signal handler: " + ex.getMessage());
            }
        }
    }

    public synchronized void restartServer(ServerType serverType) throws DBusExecutionException, DBusException {
        switch (serverType) {
            case SITE ->
                arachneSite.Restart();
            case USER ->
                arachneUser.Restart();
        }
    }

    public synchronized IFaceOpenVpnStatus getServerStatus(ServerType serverType) throws DBusExecutionException, DBusException {
        return switch (serverType) {
            case USER ->
                arachneUser.ServerStatus();
            case SITE ->
                arachneSite.ServerStatus();
        };
    }
}

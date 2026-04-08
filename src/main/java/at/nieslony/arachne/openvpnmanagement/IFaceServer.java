/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package at.nieslony.arachne.openvpnmanagement;

import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;

/**
 *
 * @author claas
 */
@DBusInterfaceName("at.nieslony.Arachne.Server")
public interface IFaceServer extends DBusInterface {

    void Restart();

    IFaceOpenVpnStatus ServerStatus();

    @Slf4j
    public class ServerStatusChanged extends DBusSignal {

        private final IFaceOpenVpnStatus status;

        public ServerStatusChanged(String objectPath, long time, List<IFaceConnectedClient> clients)
                throws DBusException {
            super(objectPath, time, clients);
            status = new IFaceOpenVpnStatus(time, clients);
            log.debug(
                    "New server status from %s at %s: %s"
                            .formatted(
                                    objectPath,
                                    new Date(time).toString(),
                                    status.toString()
                            )
            );
        }

        public IFaceOpenVpnStatus getServerStatus() {
            return status;
        }
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package at.nieslony.arachne.openvpnmanagement;

import java.util.List;
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

    public class ServerStatusChanged extends DBusSignal {

        private final IFaceOpenVpnStatus status;

        public ServerStatusChanged(String objectPath, long time, List<IFaceConnectedClient> clients)
                throws DBusException {
            super(objectPath, time, clients);
            status = new IFaceOpenVpnStatus(time, clients);
        }

        public IFaceOpenVpnStatus getServerStatus() {
            return status;
        }
    }
}

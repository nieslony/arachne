/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package at.nieslony.arachne.openvpnmanagement;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 *
 * @author claas
 */
@DBusInterfaceName("at.nieslony.Arachne.Server")
public interface IFaceServer extends DBusInterface {

    void Restart();

    IFaceOpenVpnStatus ServerStatus();
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpnmanagement;

import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

/**
 *
 * @author claas
 */
@Getter
@Setter
public class IFaceOpenVpnStatus extends Struct {

    /*
    TITLE,OpenVPN 2.6.8 x86_64-redhat-linux-gnu [SSL (OpenSSL)] [LZO] [LZ4] [EPOLL] [PKCS11] [MH/PKTINFO] [AEAD] [DCO]
    TIME,2024-01-30 18:35:01,1706636101
    HEADER,CLIENT_LIST,Common Name,Real Address,Virtual Address,Virtual IPv6 Address,Bytes Received,Bytes Sent,Connected Since,Connected Since (time_t),Username,Client ID,Peer ID,Data Channel Cipher
    HEADER,ROUTING_TABLE,Virtual Address,Common Name,Real Address,Last Ref,Last Ref (time_t)
    GLOBAL_STATS,Max bcast/mcast queue length,0
    GLOBAL_STATS,dco_enabled,0
    END

Caused by: java.lang.ClassCastException: class [Ljava.lang.Object;
    cannot be cast to class at.nieslony.arachne.openvpnmanagement.IFaceOpenVpnStatus ([Ljava.lang.Object;
    is in module java.base of loader 'bootstrap'; a
    t.nieslony.arachne.openvpnmanagement.IFaceOpenVpnStatus is
    in unnamed module of loader org.springframework.boot.devtools.restart.classloader.RestartClassLoader @763755fe)

     */
    @Position(0)
    private long time;

    @Position(1)
    private List<IFaceConnectedClient> connectedClients;

    public IFaceOpenVpnStatus(long time, List<IFaceConnectedClient> connectedClients) {
        super();
        this.time = time;
        this.connectedClients = connectedClients;
    }

    public Date getTimeAsDate() {
        return new Date(time * 1000);
    }
}

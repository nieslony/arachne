/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpnmanagement;

import java.util.Date;
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
public class IFaceConnectedClient extends Struct {

    /*
    TITLE,OpenVPN 2.6.8 x86_64-redhat-linux-gnu [SSL (OpenSSL)] [LZO] [LZ4] [EPOLL] [PKCS11] [MH/PKTINFO] [AEAD] [DCO]
    TIME,2024-01-30 18:35:01,1706636101
--> HEADER,CLIENT_LIST,Common Name,Real Address,Virtual Address,Virtual IPv6 Address,Bytes Received,Bytes Sent,Connected Since,Connected Since (time_t),Username,Client ID,Peer ID,Data Channel Cipher
    HEADER,ROUTING_TABLE,Virtual Address,Common Name,Real Address,Last Ref,Last Ref (time_t)
    GLOBAL_STATS,Max bcast/mcast queue length,0
    GLOBAL_STATS,dco_enabled,0
    END

     */
    @Position(0)
    private String commonName;
    @Position(1)
    private String realAddress;
    @Position(2)
    private String virtualAddress;
    @Position(3)
    private String virtualipV6Address;
    @Position(4)
    private long bytesReceived;
    @Position(5)
    private long bytesSent;
    @Position(6)
    private long connectedSince;
    @Position(7)
    private String username;
    @Position(8)
    private String clientId;
    @Position(9)
    private String peerId;
    @Position(10)
    private String dataChannelCipher;

    public IFaceConnectedClient(
            String commonName,
            String realAddress,
            String virtualAddress,
            String virtualipV6Address,
            long bytesReceived,
            long bytesSent,
            long connectedSince,
            String username,
            String clientId,
            String peerId,
            String dataChannelCipher
    ) {
        super();
        this.commonName = commonName;
        this.realAddress = realAddress;
        this.virtualAddress = virtualAddress;
        this.virtualipV6Address = virtualipV6Address;
        this.bytesReceived = bytesReceived;
        this.bytesSent = bytesSent;
        this.connectedSince = connectedSince;
        this.username = username;
        this.clientId = clientId;
        this.peerId = peerId;
        this.dataChannelCipher = dataChannelCipher;
    }

    public Date getConnectedSinceAsDate() {
        return new Date(connectedSince * 1000);
    }
}

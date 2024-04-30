/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.settings.AbstractSettingsGroup;
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.net.TransportProtocol;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
public class OpenVpnSiteSettings extends AbstractSettingsGroup {

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnSiteSettings.class);

    private String listenIp = "0.0.0.0";
    private int listenPort = 1194;
    private TransportProtocol listenProtocol = TransportProtocol.UDP;
    private Boolean mtuTest = true;
    private String remote = NetUtils.myHostname();
    private String deviceType = "tun";
    private String deviceName = "arachne-site";
    private String siteNetwork = "192.168.101.0";
    private Integer siteNetworkMask = 24;
    private Integer keepaliveTimeout = 60;
    private Integer keepaliveInterval = 10;
    private List<Integer> vpnSiteIds = new LinkedList<>();

    public static String createPreSharedKey() {
        SecureRandom secureRandom;
        try {
            secureRandom = SecureRandom.getInstance("NativePRNG");
        } catch (NoSuchAlgorithmException ex) {
            logger.error("Cannot create secure random: " + ex.getMessage());
            return null;
        }
        byte[] values = new byte[2048 / 8];
        secureRandom.nextBytes(values);
        String keyBase64 = String.join(
                "\n",
                Base64
                        .toBase64String(values)
                        .split("(?<=\\G.{78})")
        );

        return """
               #
               # 2048 bit OpenVPN static key
               #
               -----BEGIN OpenVPN Static key V1-----
               %s
               -----END OpenVPN Static key V1-----
               """.formatted(keyBase64);
    }
}

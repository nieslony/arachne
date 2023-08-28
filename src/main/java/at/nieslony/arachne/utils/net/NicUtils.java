/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
public class NicUtils {

    private static final Logger logger = LoggerFactory.getLogger(NicUtils.class);

    public static List<NicInfo> findAllNics() {
        List<NicInfo> allNics = new LinkedList<>();
        allNics.add(new NicInfo("0.0.0.0", "All Interfaces"));
        Enumeration<NetworkInterface> foundNics;
        try {
            foundNics = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            logger.error("Cannot retrieve list of NICs: " + ex.getMessage());
            return null;
        }
        for (NetworkInterface nic : Collections
                .list(foundNics)) {
            for (InetAddress inetAddress : Collections.list(nic.getInetAddresses())) {
                if (inetAddress instanceof Inet4Address) {
                    allNics.add(new NicInfo(
                            inetAddress.getHostAddress(),
                            nic.getName()));
                }
            }
        }

        return allNics;
    }

    public static NicInfo findNicByIp(String ipAddress) {
        return findAllNics()
                .stream()
                .filter((t) -> t.ipAddress.equals(ipAddress))
                .findFirst()
                .get();
    }

}

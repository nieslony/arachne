/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils;

import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
public class NetUtils {

    private static final Logger logger = LoggerFactory.getLogger(NetUtils.class);

    static public String maskLen2Mask(int len) {
        int mask = 0xffffffff << (32 - len);
        StringBuilder sb = new StringBuilder();

        sb.append((mask >> 24) & 0xff).append(".")
                .append((mask >> 16) & 0xff).append(".")
                .append((mask >> 8) & 0xff).append(".")
                .append(mask & 0xff);
        return sb.toString();
    }

    public static Inet4Address maskInet4Address(Inet4Address inet4Addr, int len) {
        byte[] addr = inet4Addr.getAddress();

        int mask = 0xffffffff << (32 - len);
        addr[0] &= (byte) ((mask >> 24) & 0xff);
        addr[1] &= (byte) ((mask >> 16) & 0xff);
        addr[2] &= (byte) ((mask >> 8) & 0xff);
        addr[3] &= (byte) (mask & 0xff);

        Inet4Address ret = null;
        try {
            ret = (Inet4Address) Inet4Address.getByAddress(addr);
        } catch (UnknownHostException ex) {
        }

        return ret;
    }

    public static List<SrvRecord> srvLookup(String srvType) throws NamingException {
        return srvLookup(srvType, myDomain());
    }

    public static List<SrvRecord> srvLookup(String srvType, String domain)
            throws NamingException {
        String value = null;

        Hashtable env = new Hashtable();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns:");

        DirContext ctx = new InitialDirContext(env);
        Attributes attrs = ctx.getAttributes(
                "_%s._tcp.%s".formatted(srvType, domain),
                new String[]{"SRV"});
        NamingEnumeration en = attrs.getAll();
        List<SrvRecord> srvRecords = new LinkedList<>();
        while (en.hasMore()) {
            Attribute attr = (Attribute) en.next();
            String resultStr = attr.toString().split(": ")[1];
            for (String recordStr : resultStr.split(", *")) {
                SrvRecord srvRecord = new SrvRecord(recordStr);
                srvRecords.add(srvRecord);
            }
        }
        Collections.sort(srvRecords, (r1, r2) -> r1.getPriority() - r2.getPriority());

        return srvRecords;
    }

    public static String myHostname() {
        try {
            return Inet4Address.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "";
        }
    }

    public static String myDomain() {
        try {
            String hostname = Inet4Address.getLocalHost().getHostName();
            return hostname.substring(hostname.indexOf(".") + 1);
        } catch (UnknownHostException e) {
            return "";
        }
    }

    public static String myRealm() {
        String domain = myDomain();
        try {
            Attribute attr = new InitialDirContext().getAttributes(
                    "dns:_kerberos." + domain,
                    new String[]{"TXT"}
            ).get("TXT");

            return attr.get(0).toString();
        } catch (NamingException ex) {
            return "???";
        }
    }

    public static List<String> getDnsServers() {
        List<String> dnsServers = new ArrayList<>();

        try {
            Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            DirContext ictx = new InitialDirContext(env);
            String dnsServersStr = (String) ictx.getEnvironment().get("java.naming.provider.url");

            for (String ent : dnsServersStr.split(" ")) {
                dnsServers.add(ent.split("/")[2]);
            }
        } catch (NamingException ex) {
            logger.warn(String.format("Cannot find DNS : %s", ex.getMessage()));
        }

        return dnsServers;
    }

    public static List<String> getDefaultPushRoutes() {
        List<String> routes = new LinkedList<>();

        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();

                if (!nic.isLoopback()) {
                    for (InterfaceAddress ia : nic.getInterfaceAddresses()) {
                        if (ia.getAddress() instanceof Inet4Address addr) {
                            addr = NetUtils.maskInet4Address(addr, ia.getNetworkPrefixLength());

                            String route = String.format("%s/%s",
                                    addr.toString().substring(1),
                                    ia.getNetworkPrefixLength());

                            routes.add(route);
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            logger.warn(String.format("Cannot find network address: %s", ex.getMessage()));
        }

        return routes;
    }

    public static String defaultBaseDn() {
        List<String> l = Arrays
                .asList(myDomain().split(","));
        l.replaceAll(s -> "dc=" + s);
        return String.join(",", l);
    }
}

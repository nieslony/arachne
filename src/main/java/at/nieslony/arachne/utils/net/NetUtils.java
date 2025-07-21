/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
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
import org.springframework.util.function.ThrowingConsumer;

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

    public static boolean isSubnetOf(String subnet, String of)
            throws NumberFormatException, UnknownHostException {
        String[] subnetSplit = subnet.split("/");
        String[] ofSplit = of.split("/");
        if (subnetSplit.length > 2 || ofSplit.length > 2) {
            return false;
        }
        int subnetMask = subnetSplit.length == 1
                ? 32
                : Integer.parseInt(subnetSplit[1]);
        int ofMask = ofSplit.length == 1
                ? 32
                : Integer.parseInt(ofSplit[1]);
        if (subnetMask <= ofMask) {
            return false;
        }
        InetAddress subnetAddr;
        InetAddress ofAddr;
        subnetAddr = Inet4Address.getByName(subnetSplit[0]);
        ofAddr = Inet4Address.getByName(ofSplit[0]);
        byte[] subnetBytes = subnetAddr.getAddress();
        byte[] ofBytes = ofAddr.getAddress();
        long subnetInt = subnetBytes[0] << 24
                | subnetBytes[1] << 16
                | subnetBytes[2] << 8
                | subnetBytes[3];
        long ofInt = ofBytes[0] << 24
                | ofBytes[1] << 16
                | ofBytes[2] << 8
                | ofBytes[3];
        int mask = 0xffffffff << (32 - ofMask);

        return (ofInt & mask) == (subnetInt & mask);
    }

    public static List<String> filterSubnets(List<String> nets) {
        return nets.stream()
                .filter(n -> {
                    try {
                        for (var n1 : nets) {
                            try {
                                if (n != n1 && NetUtils.isSubnetOf(n, n1)) {
                                    return false;
                                }
                            } catch (UnknownHostException ex) {
                                return false;
                            }
                        }
                    } catch (NumberFormatException ex) {
                        throw new RuntimeException(
                                "Network list contains illegal entry",
                                ex
                        );
                    }
                    return true;
                })
                .toList();
    }

    public static List<MxRecord> mxLookup(String domain) throws NamingException {
        List<MxRecord> mxRecords = new LinkedList<>();

        DirContext ctx = new InitialDirContext();
        Attributes attrs = ctx.getAttributes("dns:/" + domain, new String[]{"MX"});
        NamingEnumeration en = attrs.getAll();
        while (en.hasMore()) {
            Attribute attr = (Attribute) en.next();
            String[] splitAttr = attr.toString().split(" +");
            if (splitAttr.length != 3) {
                logger.warn("Doesn't look like an MX record: " + attr.toString());
                continue;
            }

            mxRecords.add(
                    new MxRecord(
                            Integer.parseInt(splitAttr[1]),
                            splitAttr[2]
                    )
            );
        }

        return mxRecords;
    }

    public static List<MxRecord> mxLookup() throws NamingException {
        return mxLookup(myDomain());
    }

    public static List<SrvRecord> srvLookup(String srvType) throws NamingException {
        return srvLookup(srvType, TransportProtocol.TCP, myDomain());
    }

    public static List<SrvRecord> srvLookup(String srvType, String domain)
            throws NamingException {
        return srvLookup(srvType, TransportProtocol.TCP, domain);
    }

    public static List<SrvRecord> srvLookup(
            String srvType,
            TransportProtocol protocol,
            String domain)
            throws NamingException {
        DirContext ctx = new InitialDirContext();
        String search = "dns:/_%s._%s.%s".formatted(
                srvType,
                protocol.name().toLowerCase(),
                domain
        );
        Attributes attrs = ctx.getAttributes(search, new String[]{"SRV"});
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
            Hashtable<String, String> env = new Hashtable<>();
            env.put(
                    Context.INITIAL_CONTEXT_FACTORY,
                    "com.sun.jndi.dns.DnsContextFactory"
            );
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

    public static List<String> getDefaultSearchDomains() {
        List<String> domains = new LinkedList<>();
        domains.add(myDomain());

        return domains;
    }

    public static String defaultBaseDn() {
        List<String> l = Arrays
                .asList(myDomain().split("\\."));
        l.replaceAll(s -> "dc=" + s);
        return String.join(",", l);
    }

    public static void concatKrb5Conf(String filename, String dest) throws Exception {
        try (FileWriter writer = new FileWriter(dest)) {
            writer.write(concatKrb5Conf(0, filename));
        }
    }

    static String concatKrb5Conf(int level, String filename) throws Exception {
        if (level > 10) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        reader
                .lines()
                .forEach((ThrowingConsumer<String>) line -> {
                    if (line.startsWith("includedir")) {
                        String[] tokens = line.split(" ");

                        File dir = new File(tokens[1]);
                        for (File entry : dir.listFiles()) {
                            if (entry.isFile()) {
                                writer.println(concatKrb5Conf(level + 1, entry.getAbsolutePath()));
                            }
                        }
                    } else {
                        writer.println(line);
                    }
                });

        return sw.toString();
    }
}

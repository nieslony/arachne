/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 *
 * @author claas
 */
public class NetUtils {
    static public String maskLen2Mask(int len) {
        int mask = 0xffffffff << (32 - len);
        StringBuilder sb = new StringBuilder();

        sb  .append((mask >> 24) & 0xff).append(".")
            .append((mask >> 16) & 0xff).append(".")
            .append((mask >>  8) & 0xff).append(".")
            .append( mask        & 0xff);
        return sb.toString();
    }

    public static Inet4Address maskInet4Address(Inet4Address inet4Addr, int len) {
        byte[] addr = inet4Addr.getAddress();

        int mask = 0xffffffff << (32 - len);
        addr[0] &= (byte) ((mask >> 24) & 0xff);
        addr[1] &= (byte) ((mask >> 16) & 0xff);
        addr[2] &= (byte) ((mask >>  8) & 0xff);
        addr[3] &= (byte) ( mask        & 0xff);

        Inet4Address ret = null;
        try {
            ret = (Inet4Address) Inet4Address.getByAddress(addr);
        }
        catch (UnknownHostException ex) {
        }

        return ret;
    }

    public static String srvLookup(String srvType) throws NamingException {
        return srvLookup(srvType, myDomain());
    }

    public static String srvLookup(String srvType, String domain)
        throws NamingException {
        String value = null;

        Hashtable env = new Hashtable();
        env.put("java.naming.factory.initial","com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns:");

        DirContext ctx = new InitialDirContext(env);
        Attributes attrs = ctx.getAttributes("_" + srvType + "._tcp." + domain, new String[] { "SRV" });
        NamingEnumeration en = attrs.getAll();
        int prio = -1;
        while (en.hasMore()) {
            Attribute attr = (Attribute) en.next();
            String[] values = attr.toString().split(" ");
            int p = Integer.parseInt(values[1]);
            if (p > prio) {
                value = values[4].substring(0, values[4].length()-1);
            }
        }

        return value;
    }

    public static String myDomain() {
        String domain = "";
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            domain = hostname.substring(hostname.indexOf(".") + 1);
        }
        catch (Exception e) {
        }

        return domain;
    }
}

/*
 * Copyright (C) 2018 Claas Nieslony
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.nieslony.openvpnadmin.beans;

import at.nieslony.databasepropertiesstorage.PropertyGroup;
import at.nieslony.openvpnadmin.beans.base.UserVpnBase;
import at.nieslony.utils.NetUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

@ApplicationScoped
@Named
public class UserVpn
    extends UserVpnBase
    implements Serializable
{
    public UserVpn() {
    }

    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Inject
    private PropertiesStorageBean propertiesStorage;

    public void setPropertiesStorage(PropertiesStorageBean ps) {
        propertiesStorage = ps;
    }

    @Override
    protected PropertyGroup getPropertyGroup() {
        PropertyGroup  pg = null;

        try {
            return propertiesStorage.getGroup("user-vpn", true);
        }
        catch (SQLException ex) {
            logger.severe(String.format("Cannot get property group user-vpn: %s",
                ex.getMessage()));
            if (ex.getNextException() != null)
            logger.severe(String.format("Cannot get property group user-vpn: %s",
                ex.getNextException().getMessage()));
        }

        return null;
    }

    @Override
    public String getDefaultAuthScriptUrl() {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String url = req.getRequestURL().toString();

        return url.substring(0, url.lastIndexOf("/"));
    }

    @Override
    public String getDefaultDnsServers() {
        List<String> dnsServers = new LinkedList<>();

        try {
            Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            DirContext ictx = new InitialDirContext(env);
            String dnsServersStr = (String) ictx.getEnvironment().get("java.naming.provider.url");

            for (String ent : dnsServersStr.split(" ")) {
                dnsServers.add(ent.split("/")[2]);
            }
        }
        catch (NamingException ex) {
            logger.warning(String.format("Cannot find DNS : %s", ex.getMessage()));
        }

        return String.join(",", dnsServers);
    }

    @Override
    public String getDefaultPushRoutes() {
        List<String> routes = new LinkedList<>();

        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();

                if (!nic.isLoopback()) {
                    for (InterfaceAddress ia : nic.getInterfaceAddresses()) {
                        if (ia.getAddress() instanceof Inet4Address) {
                            Inet4Address addr = (Inet4Address) ia.getAddress();
                            addr = NetUtils.maskInet4Address(addr, ia.getNetworkPrefixLength());

                            String route = String.format("%s/%s",
                                    addr.toString().substring(1),
                                    ia.getNetworkPrefixLength());

                            routes.add(route);
                        }
                    }
                }
            }
        }
        catch (SocketException ex) {
            logger.warning(String.format("Cannot find network address: %s", ex.getMessage()));
        }

        return String.join(",", routes);
    }

    @Override
    public String getDefaultHost() {
        String host = "unknown";

        try {
            host = Inet4Address.getLocalHost().getCanonicalHostName();
        }
        catch (UnknownHostException ex) {
            logger.warning(String.format("Cannot get local hostname: %s", ex.getMessage()));
        }

        return host;
    }
}

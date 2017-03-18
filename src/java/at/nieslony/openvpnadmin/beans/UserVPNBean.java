/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.ConfigBuilder;
import at.nieslony.openvpnadmin.views.AdminWelcome;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.context.RequestContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author claas
 */
@ManagedBean
@ViewScoped
public class UserVPNBean implements Serializable {
    public enum VpnAuthType implements Serializable {
        AUTH_USERNAME_PASSWORD("Username / password"),
        AUTH_CLIENT_CERT("Client certificate"),
        AUTH_BOTH("Username / pwd. + client cert.");

        final private String description;

        VpnAuthType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum UserPasswordMode implements Serializable {
        UPM_PAM("PAM"),
        UPM_HTTP("HTTP Url");

        final private String mode;

        private UserPasswordMode(String mode) {
            this.mode = mode;
        }

        public String getMode() {
            return mode;
        }
    }

    private String name = "New user VPN";
    private String networkManagerConnectionTemplate = "%n - %u@%h";
    private String host = "";
    private String protocol = "TCP";
    private String clientNetwork = "192.168.1.0";
    private int clientNetMask = 24;
    private String deviceType = "TUN";
    private int port = 1194;
    private String selDnsServer;
    private String editDnsServer;
    private ArrayList<String> dnsServers = new ArrayList<>();
    private String selPushRoute;
    private String editPushRouteNetwork;
    private String editPushRouteMask;
    private ArrayList<String> pushRoutes = new ArrayList<>();
    private int ping;
    private int pingRestart;
    private VpnAuthType authType;
    private String authPamService;
    private String authScriptUrl;
    private UserPasswordMode userPasswordMode;

    private String propsFile = null;
    private String serverConfigFile = null;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    public static final String PROP_NAME = "connection-name";
    public static final String PROP_NW_CON_TMPL = "networkmanager-connection-template";
    public static final String PROP_HOST = "host";
    public static final String PROP_PROTOCOL = "protocol";
    public static final String PROP_CLIENT_NETWORK = "client-nertwork";
    public static final String PROP_CLIENT_NET_MASK = "client-netmask";
    public static final String PROP_AUTH_TYPE = "auth-type";
    public static final String PROP_DEVICE_TYPE = "device-type";
    public static final String PROP_PORT = "port";
    public static final String PROP_DNS_SERVERS = "dns-servers";
    public static final String PROP_PUSH_ROUTES = "push-routes";
    public static final String PROP_PING = "ping";
    public static final String PROP_PING_RESTART = "ping-restart";
    public static final String PROP_AUTH_PAM_SERVICE = "auth-pam-service";
    public static final String PROP_AUTH_SCRIPT_URL = "auth-script-url";
    public static final String PROP_USER_PWD_MODE = "user-pwd-mode";

    Properties props = new Properties();

    @ManagedProperty(value = "#{folderFactory}")
    private FolderFactory folderFactory;

    @ManagedProperty(value = "#{adminWelcome}")
    private AdminWelcome adminWelcome;

    @ManagedProperty(value = "#{pki}")
    Pki pki;

    @ManagedProperty(value = "#{currentUser}")
    CurrentUser currentUser;

    @ManagedProperty(value = "#{configBuilder}")
    ConfigBuilder configBuilder;

    public String getNetworkManagerConnectionTemplate() {
        return networkManagerConnectionTemplate;
    }

    public void setNetworkManagerConnectionTemplate(String tmpl) {
        networkManagerConnectionTemplate = tmpl;
    }

    public UserPasswordMode getUserPasswordMode() {
        return userPasswordMode;
    }

    public ConfigBuilder getConfigBuilder() {
        return configBuilder;
    }

    public void setConfigBuilder(ConfigBuilder cb) {
        configBuilder = cb;
    }

    public void setUserPasswordMode(UserPasswordMode userPasswordMode) {
        this.userPasswordMode = userPasswordMode;
    }

    public UserPasswordMode[] getUserPasswordModes() {
        return UserPasswordMode.values();
    }

    public String getAuthScriptUrl() {
        return authScriptUrl;
    }

    public void setAuthScriptUrl(String authScriptUrl) {
        this.authScriptUrl = authScriptUrl;
    }

    public String getAuthPamService() {
        return authPamService;
    }

    public void setAuthPamService(String authPamService) {
        this.authPamService = authPamService;
    }

    public void setAuthType(VpnAuthType at) {
        authType = at;
    }

    public VpnAuthType getAuthType() {
        return authType;
    }

    public VpnAuthType[] getAuthTypes() {
        return VpnAuthType.values();
    }

    public void setPing(int secs) {
        this.ping = secs;
    }

    public int getPing() {
        return ping;
    }

    public void setPingRestart(int secs) {
        this.pingRestart = secs;
    }

    public int getPingRestart() {
        return pingRestart;
    }

    public void setCurrentUser(CurrentUser u) {
        currentUser = u;
    }

    public void setPki(Pki pki) {
        this.pki = pki;
    }

    public void setAdminWelcome(AdminWelcome ab) {
        this.adminWelcome = ab;
    }

    public void setFolderFactory(FolderFactory fc) {
        this.folderFactory = fc;
    }

    public List<String> getDnsServers() {
        return dnsServers;
    }

    public List<String> getPushRoutes() {
        return pushRoutes;
    }

    public int getPort() {
        return port;
    }

    public String getSelDnsServer() {
        return selDnsServer;
    }

    public void setSelDnsServer(String selDnsServer) {
        this.selDnsServer = selDnsServer;
    }

    public String getEditDnsServer() {
        return selDnsServer;
    }

    public void setEditDnsServer(String editDnsServer) {
        this.editDnsServer = editDnsServer;
    }

    public void setSelPushRoute(String s) {
        this.selPushRoute = s;
    }

    public String getSelPushRoute() {
        return selPushRoute;
    }

    public void setEditPushRouteNetwork(String s) {
        this.editPushRouteNetwork = s;
    }

    public String getEditPushRouteNetwork() {
        if (selPushRoute == null)
            return "";

        String[] sa = selPushRoute.split("/");

        if (sa.length == 2)
            return sa[0];
        else
            return "???";
    }

    public void setEditPushRouteMask(String s) {
        this.editPushRouteMask = s;
    }

    public String getEditPushRouteMask() {
        if (selPushRoute == null)
            return "";

        String[] sa = selPushRoute.split("/");

        if (sa.length == 2)
            return sa[1];
        else
            return "???";
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public int getClientNetMask() {
        return clientNetMask;
    }

    public void setClientNetMask(int clientNetMask) {
        this.clientNetMask = clientNetMask;
    }

    public String getClientNetwork() {
        return clientNetwork;
    }

    public void setClientNetwork(String clientNetwork) {
        this.clientNetwork = clientNetwork;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Creates a new instance of UserVPNBean
     */
    public UserVPNBean() {
    }

    private String serverConfigFor(String propertiesFile) {
        File f = new File(propertiesFile);
        String pfn = f.getName();
        int i = pfn.lastIndexOf(".");
        return folderFactory.getServerConfDir() + "/" + pfn.subSequence(0, i) + ".conf";
    }

    public void save() throws IOException {
        props.setProperty(PROP_NAME, name);
        props.setProperty(PROP_NW_CON_TMPL, networkManagerConnectionTemplate);
        props.setProperty(PROP_AUTH_TYPE, authType.name());
        props.setProperty(PROP_CLIENT_NETWORK, clientNetwork);
        props.setProperty(PROP_CLIENT_NET_MASK, String.valueOf(clientNetMask));
        props.setProperty(PROP_DEVICE_TYPE, deviceType);
        props.setProperty(PROP_DNS_SERVERS, String.join(" ", dnsServers));
        props.setProperty(PROP_HOST, host);
        props.setProperty(PROP_PORT, String.valueOf(port));
        props.setProperty(PROP_PROTOCOL, protocol);
        props.setProperty(PROP_DNS_SERVERS, String.join(",", dnsServers));
        props.setProperty(PROP_PUSH_ROUTES, String.join(",", pushRoutes));
        props.setProperty(PROP_PING, String.valueOf(ping));
        props.setProperty(PROP_PING_RESTART, String.valueOf(pingRestart));
        props.setProperty(PROP_AUTH_PAM_SERVICE, authPamService);
        props.setProperty(PROP_AUTH_SCRIPT_URL, authScriptUrl);
        props.setProperty(PROP_USER_PWD_MODE, userPasswordMode.name());

        if (propsFile == null || propsFile.isEmpty()) {
            propsFile = folderFactory.getNewVpnFilePath();
        }
        serverConfigFile = serverConfigFor(propsFile);

        logger.info(String.format("Writing VPN settings to %s", propsFile));
        FileOutputStream fos = new FileOutputStream(propsFile);
        props.store(fos, "");
        fos.close();

        logger.info("Writing server config to " + serverConfigFile);
        FileWriter fwr= new FileWriter(serverConfigFile);
        configBuilder.writeUserVpnServerConfig(props, pki, fwr);
        fwr.flush();
        fwr.close();

        adminWelcome.loadUserVpns();
        RequestContext.getCurrentInstance().update("menuForm:mainMenu");
    }

    private void setValues(Properties props) {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String url =req.getRequestURL().toString();

        name = props.getProperty(PROP_NAME, "New user VPN");
        networkManagerConnectionTemplate = props.getProperty(PROP_NW_CON_TMPL, "%n - %u@%h");
        authType = VpnAuthType.valueOf(props.getProperty(PROP_AUTH_TYPE, VpnAuthType.AUTH_BOTH.name()));
        clientNetwork = props.getProperty(PROP_CLIENT_NETWORK, "192.168.1.0");
        clientNetMask = Integer.valueOf(props.getProperty(PROP_CLIENT_NET_MASK, "24"));
        deviceType = props.getProperty(PROP_DEVICE_TYPE, "tun");
        host = props.getProperty(PROP_HOST, "");
        port = Integer.valueOf(props.getProperty(PROP_PORT, String.valueOf(1194)));
        protocol = props.getProperty(PROP_PROTOCOL, "udp");
        String[] l = props.getProperty(PROP_DNS_SERVERS, "").split(",");
        dnsServers.addAll(Arrays.asList(l));
        l = props.getProperty(PROP_PUSH_ROUTES, "").split(",");
        pushRoutes.addAll(Arrays.asList(l));
        ping = Integer.valueOf(props.getProperty(PROP_PING, "10"));
        pingRestart = Integer.valueOf(props.getProperty(PROP_PING_RESTART, "60"));
        authPamService = props.getProperty(PROP_AUTH_PAM_SERVICE, "openvpn");
        authScriptUrl = props.getProperty(
                PROP_AUTH_SCRIPT_URL,
                url.substring(0, url.lastIndexOf("/")));
        userPasswordMode = UserPasswordMode.valueOf(
                props.getProperty(
                        PROP_USER_PWD_MODE,
                        UserPasswordMode.UPM_HTTP.name()));
    }

    public void load() {
        try {
            FileInputStream fis = new FileInputStream(propsFile);
            props.load(fis);
        }
        catch (IOException ex) {
            logger.severe(String.format("Cannot find %s",  propsFile));
            propsFile = null;
        }

        setValues(props);
    }

    public void addRoute() {
        String routeStr = editPushRouteNetwork + "/" + editPushRouteMask;
        logger.info(String.format("Adding route %s", routeStr));
        pushRoutes.add(routeStr);
        selPushRoute = routeStr;
    }

    public void modifyRoute() {
        String routeStr = editPushRouteNetwork + "/" + editPushRouteMask;

        int index = pushRoutes.indexOf(selPushRoute);
        if (index != -1) {
            pushRoutes.set(index, routeStr);
        }
    }

    public void removeRoute() {

    }

    public void addDnsServer() {
        logger.info(String.format("Adding DNS server %s", editDnsServer));
        dnsServers.add(editDnsServer);
    }

    public void modifyDnsServer() {
        logger.info(String.format("Replacing DNS server %s by %s", selDnsServer, editDnsServer));
        int index = dnsServers.indexOf(selDnsServer);
        if (index != -1) {
            dnsServers.set(index, editDnsServer);
        }
        else {
            logger.warning(String.format("Cannot find index of %s", selDnsServer));
        }
    }

    public void removeDnsServer() {

    }

    public void remove() {
        if (propsFile != null && !propsFile.isEmpty()) {
            File f = new File(propsFile);
            f.delete();
        }
    }

    @PostConstruct
    public void init() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String vpnName = params.get("userVpn");
        if (vpnName != null && !vpnName.isEmpty()) {
            logger.info(String.format("Loading VPN %s", vpnName));
            propsFile  = folderFactory.getVpnConfigDir() + "/" + vpnName + ".properties";
            serverConfigFile = folderFactory.getServerConfDir() + "/" + vpnName + ".conf";

            load();
        }
        else {
            try {
                logger.info("No VPN given, creating default config");
                setValues(props);

                Hashtable env = new Hashtable();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
                DirContext ictx = new InitialDirContext(env);
                String dnsServersStr = (String) ictx.getEnvironment().get("java.naming.provider.url");

                for (String ent : dnsServersStr.split(" ")) {
                    dnsServers.add(ent.split("/")[2]);
                }
            }
            catch (Exception ex) {
                logger.warning(String.format("Cannot find LDAP server in DNS: %s", ex.getMessage()));
            }
        }
    }

    StreamedContent sc = new DefaultStreamedContent();

    public Boolean getRenderPamService() {
        return userPasswordMode == UserPasswordMode.UPM_PAM && authType != VpnAuthType.AUTH_CLIENT_CERT;
    }

    public Boolean getRenderAuthUrl() {
        return userPasswordMode == UserPasswordMode.UPM_HTTP && authType != VpnAuthType.AUTH_CLIENT_CERT;
    }

    public Boolean getRenderUserPasswordMode() {
        return authType != VpnAuthType.AUTH_CLIENT_CERT;
    }
}

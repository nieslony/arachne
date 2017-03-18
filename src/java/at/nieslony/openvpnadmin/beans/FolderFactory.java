/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

/**
 *
 * @author claas
 */
@ManagedBean
@ApplicationScoped
public class FolderFactory implements Serializable {
    private String appRoot;
    private String binDir;

    private static final String DIR_PKI = "/pki";
    private static final String DIR_REVOKED_CERTS = DIR_PKI + "/revoked";
    private static final String DIR_USER_CERTS = "/pki/user-certs";
    private static final String DIR_ROLES = "/roles";
    private static final String DIR_VPN_CONFIG = "/vpns";
    private static final String DIR_SERVER_CONF = "/server-conf";

    class UserVpnFilter implements FilenameFilter, Serializable {
        String match = getUserVpnName("[0-9*]");

        @Override
        public boolean accept(File dir, String name) {
            return name.matches(match);
        }
    }

    UserVpnFilter userVpnFilter = new UserVpnFilter();
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    /**
     * Creates a new instance of FolderFactory
     */
    public FolderFactory() {
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing folderFactory");
        ExternalContext extCtx = FacesContext.getCurrentInstance().getExternalContext();
        String dir = extCtx.getInitParameter("dynamic-data-dir");
        if (dir.startsWith("/")) {
            appRoot = dir;
        }
        else {
            appRoot = extCtx.getRealPath("/" + dir);
        }
        new File(getVpnConfigDir()).mkdirs();
        new File(getPkiDir()).mkdirs();
        new File(getRevokedCertsDir()).mkdirs();
        new File(getUserCertsDir()).mkdirs();
        new File(getServerConfDir()).mkdirs();
        new File(getRolesDir()).mkdirs();

        binDir = extCtx.getRealPath("/WEB-INF/bin");
    }

    public String getRevokedCertsDir() {
        return appRoot + DIR_REVOKED_CERTS;
    }

    public String getRolesDir() {
        return appRoot + DIR_ROLES;
    }

    public String getServerConfDir() {
        return appRoot + DIR_SERVER_CONF;
    }

    public String getUserCertsDir() {
        return appRoot + DIR_USER_CERTS;
    }

    public String getConfigDir() {
        return appRoot;
    }

    public String getVpnConfigDir() {
        return appRoot + DIR_VPN_CONFIG;
    }

    public String getPkiDir() {
        return appRoot + DIR_PKI;
    }

    public String getBinDir() {
        return binDir;
    }

    public String[] getUserVpns() {
        File dir = new File(getVpnConfigDir());
        logger.info(String.format("Searching for files in %s with filer %s",
                getVpnConfigDir(), userVpnFilter.match));
        String[] userVpns = dir.list(userVpnFilter);
        return userVpns;
    }

    public String getUserVpnName(String nr) {
        return "user-vpn-" + nr + ".properties";
    }

    public String getUserVpnPath(String nr) {
        return getVpnConfigDir() + "/" + getUserVpnName(nr);
    }

    public String getNewVpnFilePath() {
        String[] userVpns = getUserVpns();
        if (userVpns == null || userVpns.length == 0) {
            logger.info("No files found.");
            return getVpnConfigDir() + "/" + getUserVpnName("0");
        }
        else
            logger.info(String.format("Found %d files", userVpns.length));
        int freeNr = 0;
        do {
            boolean found = false;
            String name = "";
            for (int i = 0; i < userVpns.length; i++) {
                name = getUserVpnName(String.valueOf(i));
                if (userVpns[i].equals(name)) {
                    found = true;
                    break;
                }
            }
            if (!found)
                return getVpnConfigDir() + "/" + name;
            freeNr++;
        } while (freeNr < 100);

        logger.severe("No free file for user VPN found. Do you realy habe more than 100 user VPNs???");
        return "";
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

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
    private String dynamicDataDir = null;
    private String sqlDir = null;
    private String binDir = null;
    private String pluginDir = null;

    private final String FN_USER_VPN = "arachne_uservpn.conf";

    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    /**
     * Creates a new instance of FolderFactory
     */
    public FolderFactory() {
    }

    @PostConstruct
    public void init() {
        getBinDir();
        getSqlDir();
        getDynamicDataDir();
    }

    private String getDynamicDataDir() {
        if (dynamicDataDir == null) {
            ExternalContext extCtx = FacesContext.getCurrentInstance().getExternalContext();
            dynamicDataDir = extCtx.getInitParameter("dynamic-data-dir");
        }

        return dynamicDataDir;
    }

    public String getSqlDir() {
        if (sqlDir == null) {
            ExternalContext extCtx = FacesContext.getCurrentInstance().getExternalContext();
            sqlDir = extCtx.getInitParameter("sql-dir");
        }

        return sqlDir;
    }

    public String getPluginDir() {
        if (pluginDir == null) {
            ExternalContext extCtx = FacesContext.getCurrentInstance().getExternalContext();
            pluginDir = extCtx.getInitParameter("plugin-dir");
        }

        return pluginDir;
    }

    public String getServerConfDir() {
        return getDynamicDataDir() + "/vpnconfig";
    }

    public String getConfigDir() {
        return getDynamicDataDir() + "/appconfig";
    }

    public String getPkiDir() {
        return getDynamicDataDir() + "/vpnconfig";
    }

    public String getBinDir() {
        if (binDir == null) {
            ExternalContext extCtx = FacesContext.getCurrentInstance().getExternalContext();
            binDir = extCtx.getInitParameter("bin-dir");
        }

        return binDir;
    }

    public String getUserVpnFileName() {
        return String.format("%s/%s", getServerConfDir(), FN_USER_VPN);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 *
 * @author claas
 */
public class DynamicLoginConfiguration extends Configuration {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    Map<String, AppConfigurationEntry[]> entries = new HashMap<>();

    public void updateEntry(LdapHelperUser lhu) {
        String name = lhu.getClass().getName();
        String keytab = lhu.getKeytabFile();
        String principal = lhu.getKerberosPrincipal();
        logger.info(String.format("Create config for %s, keytab=%s, principal=%s",
                name, keytab, principal));

        Map<String, String> options = new HashMap<>();
        options.put("debug", "TRUE");
        options.put("doNotPrompt", "TRUE");
        options.put("refreshKrb5Config", "TRUE");
        options.put("useKeyTab", "TRUE");
        options.put("keyTab", keytab);
        options.put("principal", principal);

        AppConfigurationEntry[] aces = {
            new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        options)
        };

        entries.remove(name);
        entries.put(name, aces);
    }

    public AppConfigurationEntry[] getAppConfigurationEntry(String name, boolean createIfNotFound) {
        AppConfigurationEntry[] aces = getAppConfigurationEntry(name);

        return aces;
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        return entries.get(name);
    }
}

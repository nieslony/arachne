/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 *
 * @author claas
 */
public class DynamicLoginConfiguration extends Configuration {
    Map<String, AppConfigurationEntry[]> entries = new HashMap<>();

    private AppConfigurationEntry[] createNewEntry() {
        Map<String, String> options = new HashMap<>();
        options.put("debug", "FALSE");
        options.put("doNotPrompt", "TRUE");
        options.put("refreshKrb5Config", "TRUE");
        options.put("useKeyTab", "TRUE");
        options.put("keyTab", "");
        options.put("principal", "");

        AppConfigurationEntry[] aces = {
            new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        options)
        };

        return aces;
    }

    public void setKeytabFile(String configName, String filename) {
        AppConfigurationEntry[] config = getAppConfigurationEntry(configName);
        if (config == null) {
            entries.put(configName, createNewEntry());
        }

        Map<String, Object> options = (Map<String,Object>) config[0].getOptions();
        options.put("keyTab",  filename);
    }

    public void setPrincipal(String configName, String principal) {
        AppConfigurationEntry[] config = getAppConfigurationEntry(configName);
        if (config == null) {
            entries.put(configName, createNewEntry());
        }

        Map<String, Object> options = (Map<String,Object>) config[0].getOptions();
        options.put("keyTab",  principal);
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        return entries.get(name);
    }
}

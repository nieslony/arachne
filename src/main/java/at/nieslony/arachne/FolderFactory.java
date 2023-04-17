/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.utils.NetUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
public class FolderFactory {

    private static final Logger logger = LoggerFactory.getLogger(FolderFactory.class);

    @Value("${vpnConfigDir}")
    private String vpnConfigDir;

    @Value("${arachneConfigDir}")
    private String arachneConfigDir;

    static private FolderFactory instance;

    public FolderFactory() {
        instance = this;
    }

    static public FolderFactory getInstance() {
        return instance;
    }

    public String getVpnConfigDir() {
        try {
            Files.createDirectories(Path.of(vpnConfigDir));
            File f = new File(vpnConfigDir);
            return f.getCanonicalPath();
        } catch (IOException ex) {
            logger.error(
                    "Cannot create %s: %s"
                            .formatted(vpnConfigDir, ex.getMessage())
            );
            return vpnConfigDir;
        }
    }

    public String getVpnConfigDir(String filename) {
        return "%s/%s".formatted(getVpnConfigDir(), filename);
    }

    public String getArachneConfigDir() {
        return arachneConfigDir;
    }

    public String getKrb5ConfPath() {
        String krb5ConfPath = arachneConfigDir + "/krb5.conf";

        File krb5ConfFile = new File(krb5ConfPath);
        if (!krb5ConfFile.exists()) {
            logger.info("Creating " + krb5ConfPath);
            try {
                NetUtils.concatKrb5Conf("/etc/krb5.conf", krb5ConfPath);
            } catch (Exception ex) {
                logger.error("Cannot copy krb5.conf to %s: %s"
                        .formatted(krb5ConfPath, ex.getMessage())
                );
            }
        }
        try {
            return krb5ConfFile.getCanonicalPath();
        } catch (IOException ey) {
            return krb5ConfPath;
        }
    }

    public String getDefaultKeytabPath() {
        String filename = arachneConfigDir + "/krb5.keytab";
        File f = new File(filename);
        try {
            return f.getCanonicalPath();
        } catch (IOException ex) {
            return arachneConfigDir + "/krb5.keytab";
        }
    }
}

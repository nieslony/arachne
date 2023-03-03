/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
public class FolderFactory {

    @Value("${vpnConfigDir}")
    private String vpnConfigDir;

    public String getVpnConfigDir() {
        try {
            Files.createDirectories(Path.of(vpnConfigDir));
        } catch (IOException ex) {

        }

        return vpnConfigDir;
    }

    public String getVpnConfigDir(String filename) {
        return "%s/%s".formatted(getVpnConfigDir(), filename);
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpnmanagement;

import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.utils.FolderFactory;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
public class OpenVpnManagement {

    private static final Logger logger = LoggerFactory.getLogger(OpenVpnManagement.class);

    private String socketPath;
    private SocketChannel socketChannel;
    private BufferedReader managementReader;
    private PrintWriter managementWriter;
    private String managementPassword;

    @Autowired
    Settings settings;

    @Autowired
    FolderFactory folderFactory;

    private OpenVpnManagementSettings openVpnManagementSettings;

    public OpenVpnManagement() {
        logger.info("Creating OpenVPN management interface");
    }

    @PostConstruct
    public void init() {
        openVpnManagementSettings
                = settings.getSettings(OpenVpnManagementSettings.class);
        if (!openVpnManagementSettings.getManagementPassword().isEmpty()) {
            managementPassword = new String(
                    Base64.getDecoder()
                            .decode(openVpnManagementSettings.getManagementPassword()
                            )
            );
        } else {
            managementPassword = getNewPassword();
            String encodedPassword
                    = Base64
                            .getEncoder()
                            .encodeToString(managementPassword.getBytes());
            openVpnManagementSettings.setManagementPassword(encodedPassword);
        }
    }

    public void quit() {
        try {
            logger.info("Quitting management interface");
            invokeCommand("quit");
        } catch (OpenVpnManagementException ex) {
            logger.error("Cannot quit management interface: " + ex.getMessage());
        }
    }

    private String getSocketPath() {
        return folderFactory.getVpnConfigDir(openVpnManagementSettings.getSocketFilename());
    }

    public void openSocket() throws OpenVpnManagementException {
        if (socketChannel != null && socketChannel.isOpen() && socketChannel.isConnected()) {
            return;
        }

        socketPath = getSocketPath();
        logger.info("Connecting to socket " + socketPath);

        var address = UnixDomainSocketAddress.of(getSocketPath());
        try {
            socketChannel = SocketChannel.open(address);
            managementWriter = new PrintWriter(
                    new OutputStreamWriter(
                            Channels.newOutputStream(socketChannel)
                    ),
                    true
            );
            managementReader = new BufferedReader(
                    new InputStreamReader(
                            Channels.newInputStream(socketChannel))
            );

            managementWriter.println(managementPassword);

            String line;
            do {
                line = managementReader.readLine();
                logger.info("Opening management interface: " + line);
            } while (!line.startsWith(">"));
            logger.info("Management interface is open");
        } catch (IOException ex) {
            String msg = "Cannot connect to socket: " + ex.getMessage();
            logger.error(msg);
            managementReader = null;
            managementWriter = null;
            throw new OpenVpnManagementException(msg);
        }
    }

    synchronized List<String> invokeCommand(String command)
            throws OpenVpnManagementException {
        openSocket();

        List<String> lines = new LinkedList<>();
        logger.info("Invoking command: " + command);

        try {
            managementWriter.println(command);
            String line = managementReader.readLine();
            if (line == null) {
                String msg = "Nothing read from management interface";
                logger.warn(msg);
                throw new OpenVpnManagementException(msg);
            }
            lines.add(line);
            if (line.startsWith("SUCCESS:")) {
                return lines;
            } else if (line.startsWith("ERROR:")) {
                throw new OpenVpnManagementException("");
            }
            do {
                line = managementReader.readLine();
                if (line == null) {
                    break;
                }
                lines.add(line);
            } while (!line.equals("END"));
        } catch (IOException ex) {
            String msg = "Error invoking command %s: %s".formatted(command, ex.getMessage());
            logger.error(msg);
            throw new OpenVpnManagementException(msg);
        }
        logger.info(lines.toString());
        return lines;
    }

    public List<ConnectedClient> getConnectedUsers() throws OpenVpnManagementException {
        List<ConnectedClient> clients = new LinkedList<>();
        for (String s : invokeCommand("status 3")) {
            if (s.startsWith("CLIENT_LIST")) {
                ConnectedClient client = new ConnectedClient(s);
                clients.add(client);
            }
        }

        return clients;
    }

    public void restartServer() throws OpenVpnManagementException {
        List<String> result = invokeCommand("signal SIGHUP");
        logger.info("Command result: " + result.toString());
    }

    public void saveSettings(String socket, String password) {
        String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());

        OpenVpnManagementSettings openVpnManagementSettings
                = settings.getSettings(OpenVpnManagementSettings.class);
        openVpnManagementSettings.setSocketFilename(socket);
        openVpnManagementSettings.setManagementPassword(encodedPassword);
        try {
            openVpnManagementSettings.save(settings);
        } catch (SettingsException ex) {
            logger.error("Cannot save settings: " + ex.getMessage());
        }
    }

    private String getNewPassword() {
        SecureRandom random = new SecureRandom();

        String password = random.ints(32, 127)
                .filter(i -> Character.isLetterOrDigit(i))
                .limit(32)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return password;
    }

    public String getVpnConfigSetting() {
        return "management %s unix %s"
                .formatted(
                        getSocketPath(),
                        folderFactory.getVpnConfigDir(
                                openVpnManagementSettings.getPasswordFilename()
                        )
                );
    }
}

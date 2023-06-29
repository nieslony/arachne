/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpnmanagement;

import at.nieslony.arachne.settings.SettingsModel;
import at.nieslony.arachne.settings.SettingsRepository;
import at.nieslony.arachne.utils.FolderFactory;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileWriter;
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
import java.util.Optional;
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

    private static final String SK_MANAGEMENT_SOCKET = "openvpn.management.socket";
    private static final String SK_MANAGEMENT_PASSWORD = "openvpn.management.password";
    private static final String PASSWORD_FN = "management.pwd";
    private static final String SOCKET_FN = "arachne-management.socket";

    private String socketPath;
    private SocketChannel socketChannel;
    private BufferedReader managementReader;
    private PrintWriter managementWriter;

    private String managementPassword;

    @Autowired
    SettingsRepository settingsRepository;

    @Autowired
    FolderFactory folderFactory;

    public OpenVpnManagement() {
        logger.info("Creating OpenVPN management interface");
    }

    @PostConstruct
    public void init() {
        managementPassword = getPassword();
    }

    public void quit() {
        try {
            logger.info("Quitting management interface");
            invokeCommand("quit");
        } catch (OpenVpnManagementException ex) {
            logger.error("Cannot quit management interface: " + ex.getMessage());
        }
    }

    private String getPassword() {
        Optional<SettingsModel> setting = settingsRepository.findBySetting(SK_MANAGEMENT_PASSWORD);
        if (!setting.isPresent()) {
            String password = getNewPassword();
            savePassword(password);
            return password;
        } else {
            String password = new String(
                    Base64.getDecoder().decode(setting.get().getContent())
            );
            return password;
        }
    }

    private String getSocketPath() {
        return folderFactory.getVpnConfigDir(SOCKET_FN);
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

            managementWriter.println(getPassword());

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
                lines.add(line);
            } while (!line.equals("END"));
        } catch (IOException ex) {
            String msg = "Error invoking command %s: %s".formatted(command, ex.getMessage());
            logger.error(msg);
            throw new OpenVpnManagementException(msg);
        }
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
        Optional<SettingsModel> setting;

        String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());

        setting = settingsRepository.findBySetting(SK_MANAGEMENT_SOCKET);
        if (setting.isPresent()) {
            setting.get().setContent(socket);
            settingsRepository.save(setting.get());
        } else {
            settingsRepository.save(new SettingsModel(SK_MANAGEMENT_SOCKET, socket));
        }

        setting = settingsRepository.findBySetting(SK_MANAGEMENT_PASSWORD);
        if (setting.isPresent()) {
            setting.get().setContent(encodedPassword);
            settingsRepository.save(setting.get());
        } else {
            settingsRepository.save(new SettingsModel(SK_MANAGEMENT_PASSWORD, encodedPassword));
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

    private void savePassword(String password) {
        String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());

        Optional<SettingsModel> setting;
        setting = settingsRepository.findBySetting(SK_MANAGEMENT_PASSWORD);
        if (setting.isPresent()) {
            setting.get().setContent(encodedPassword);
            settingsRepository.save(setting.get());
        } else {
            settingsRepository.save(new SettingsModel(SK_MANAGEMENT_PASSWORD, encodedPassword));
        }

        try (FileWriter passwordFile = new FileWriter(folderFactory.getVpnConfigDir(PASSWORD_FN))) {
            passwordFile.write(password + "\n");
        } catch (IOException ex) {

        }
    }

    public void generateNewPassword() {
        savePassword(getNewPassword());
    }

    public String getVpnConfigSetting() {
        return "management %s unix %s"
                .formatted(
                        getSocketPath(),
                        folderFactory.getVpnConfigDir(PASSWORD_FN));
    }
}

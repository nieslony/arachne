/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpnmanagement;

import at.nieslony.arachne.FolderFactory;
import at.nieslony.arachne.settings.SettingsModel;
import at.nieslony.arachne.settings.SettingsRepository;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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

    private UnixDomainSocketAddress managementSocket;
    private String socketPath;
    private SocketChannel socketChannel;
    private BufferedReader managementReader;
    private BufferedWriter managementWriter;

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

    @PreDestroy
    public void destroy() {
        logger.info("Closing and removing socket");
        Path path = Path.of(socketPath);
        try {
            if (socketChannel.isOpen()) {
                socketChannel.close();
            }
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            logger.error(
                    "Cannopt delete socket %s: %s"
                            .formatted(socketPath, ex.getMessage())
            );
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
        socketPath = getSocketPath();
        logger.info("Connecting to socket " + socketPath);
        managementSocket = UnixDomainSocketAddress.of(socketPath);

        try {
            socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
            socketChannel.connect(managementSocket);

            managementReader = new BufferedReader(
                    new InputStreamReader(
                            socketChannel.socket().getInputStream()
                    )
            );
            managementWriter = new BufferedWriter(
                    new OutputStreamWriter(
                            socketChannel.socket().getOutputStream()
                    ));

            managementWriter.write(getPassword());
            managementWriter.newLine();
        } catch (IOException ex) {
            String msg = "Cannot connect to socket: " + ex.getMessage();
            logger.error(msg);
            managementReader = null;
            managementReader = null;
            throw new OpenVpnManagementException(msg);
        }
    }

    synchronized List<String> invokeCommand(String command)
            throws OpenVpnManagementException {
        List<String> lines = new LinkedList<>();
        logger.info("Invoking command: " + command);

        try {
            managementWriter.write(command);
            managementWriter.newLine();

            String line = managementReader.readLine();
            lines.add(line);
            if (line.startsWith("SUCCESS:")) {
                return lines;
            } else if (line.startsWith("ERROR:")) {
                throw new OpenVpnManagementException("");
            }
            do {
                line = managementReader.readLine();
                lines.add(line);
            } while (line.equals("END"));
        } catch (IOException ex) {

        }
        return lines;
    }

    public void restart() throws OpenVpnManagementException {
        List<String> result = invokeCommand("signal ");
        logger.info("Command result: " + result.get(0));
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
        return "management %s unix %s".formatted(getSocketPath(), PASSWORD_FN);
    }
}

package at.nieslony.arachne.openvpn.management;

import at.nieslony.arachne.openvpn.management.commands.Command;
import at.nieslony.arachne.openvpn.management.commands.DropUser;
import at.nieslony.arachne.openvpn.management.commands.Hold;
import at.nieslony.arachne.openvpn.management.commands.Pid;
import at.nieslony.arachne.openvpn.management.commands.RestartServer;
import at.nieslony.arachne.openvpn.management.commands.Status;
import at.nieslony.arachne.openvpn.management.commands.Version;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Slf4j
public class OpenVpnManagement {

    private final Path socketPath;
    private final String name;

    private final BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> managementMsgQueue = new LinkedBlockingQueue<>();
    private final Thread commandProcessor;
    private final Thread managementMsgReader;
    private final Thread managementMsgProcessor;
    private volatile Command currentCommand = null;

    private boolean isInHoldStatus;

    private volatile SocketChannel clientChannel = null;

    public OpenVpnManagement(String name, Path managementSocketPath) {
        this.name = name;
        this.socketPath = managementSocketPath;

        commandProcessor = new Thread(() -> {
            log.info("Starting command processor");
            try {
                for (;;) {
                    Command cmd = commandQueue.take();
                    log.info("Took command from queue: " + cmd.toString());
                    try {
                        if (clientChannel == null || !clientChannel.isConnected()) {
                            commandQueue.remove(cmd);
                            cmd.cancel(new ManagementException("Cannot connect to management interface"));
                        } else {
                            int len = cmd.writeCommand(clientChannel);
                            log.debug("%s (%d bytes) written".formatted(cmd.toString(), len));
                            currentCommand = cmd;
                            log.debug("Waiting for unlock");
                            cmd.waitForUnlock();
                        }
                    } catch (IOException ex) {
                        log.error("Cannot write Command: " + ex.getMessage());
                    }
                }
            } catch (InterruptedException | ExecutionException ex) {
                log.error("Interrupted");
            }
        }, "OvMgmtCmdProc-" + Character.toUpperCase(name.charAt(0)));

        managementMsgReader = new Thread(() -> {
            log.info("Starting management message reader");
            StringBuilder currentLine = new StringBuilder();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            for (;;) {
                try {
                    if (clientChannel == null || !clientChannel.isOpen()) {
                        waitForSocket();
                        connectToManagementInterface();
                    }
                    log.debug("Waiting for data");
                    buffer.clear();
                    int len = clientChannel.read(buffer);
                    log.debug("%d bytes read".formatted(len));
                    if (len < 0) {
                        clientChannel.close();
                    }
                } catch (IOException ex) {
                    log.error("Error reading from socket: " + ex.getMessage());
                }
                buffer.flip();
                while (buffer.hasRemaining()) {
                    char c = (char) buffer.get();
                    switch (c) {
                        case '\r' -> {
                        }
                        case '\n' -> {
                            try {
                                String l = currentLine.toString();
                                log.debug("Putting »%s« to queue".formatted(l));
                                managementMsgQueue.put(l);
                                currentLine = new StringBuilder();
                            } catch (InterruptedException ex) {
                                log.error("Interrupted: " + ex.getMessage());
                            }
                        }
                        default -> {
                            currentLine.append(c);
                        }
                    }
                }
            }
        }, "OvMgmtMsgRead-" + Character.toUpperCase(name.charAt(0)));

        managementMsgProcessor = new Thread(() -> {
            for (;;) {
                try {
                    String curLine = managementMsgQueue.take();
                    log.debug("Processing line: »%s«".formatted(curLine));
                    if (curLine.startsWith(">INFO:")) {
                        log.debug("Got log line: " + curLine);
                    } else if (curLine.startsWith(">HOLD:")) {
                        log.info("Management interface is in hold status");
                        isInHoldStatus = true;
                    } else if (currentCommand != null) {
                        try {
                            if (currentCommand.processResultLine(curLine)) {
                                log.debug("Last line of %s added".formatted(
                                        currentCommand.toString()
                                ));
                                currentCommand.processResult();
                                log.info("Result of %s processed, removing command".formatted(
                                        currentCommand.toString()
                                ));
                                currentCommand = null;
                            }
                        } catch (ManagementException ex) {
                            currentCommand.cancel(ex);
                            currentCommand = null;
                        }
                    } else {
                        log.warn("Unexpected line from management interface: " + curLine);
                    }
                } catch (InterruptedException ex) {
                    log.error("Interrupted");
                }
            }
        }, "OvMgmtMsgProc-" + Character.toUpperCase(name.charAt(0)));

        log.info("Starting %s threads".formatted(name));
        commandProcessor.start();
        managementMsgReader.start();
        managementMsgProcessor.start();
    }

    private void connectToManagementInterface() {
        try {
            log.info("Connecting to %s management Socket".formatted(name));
            clientChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
            UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
            clientChannel.connect(address);
            clientChannel.configureBlocking(true);
            isInHoldStatus = false;
        } catch (IOException ex) {
            log.error("IO Exception: " + ex);
        }
    }

    private void waitForSocket() {
        if (Files.exists(socketPath)) {
            log.info("Socket %s already exists".formatted(socketPath.toString()));
            return;
        }
        try {
            log.info("Waiting for socket to appear.");
            WatchService watchService = FileSystems.getDefault().newWatchService();

            Path socketDir = socketPath.getParent();
            socketDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    String filename = event.context().toString();
                    log.debug("File created: " + filename);
                    if (filename.equals(socketPath.getFileName().toString())) {
                        log.debug("Socket appeared");
                        return;
                    } else {
                        log.debug("Not mine. Expected: %s, got: %s".formatted(
                                filename,
                                socketPath.getFileName().toString()
                        ));
                    }
                }
                key.reset();
            }
        } catch (IOException | InterruptedException ex) {
            log.error("Error waiting for socket: " + ex.getMessage());
        }
    }

    public boolean getIsInHoldStatus() {
        return isInHoldStatus;
    }

    public int pid() throws ManagementException {
        Pid p = new Pid(commandQueue);
        return p.waitForResult();
    }

    public Version.VersionInfo version() throws ManagementException {
        Version v = new Version(commandQueue);
        return v.waitForResult();
    }

    public void restartServer() throws ManagementException {
        RestartServer rs = new RestartServer(commandQueue);
        rs.waitForResult();
    }

    public String dropUser(String username) throws ManagementException {
        DropUser du = new DropUser(commandQueue, username);
        return du.waitForResult();
    }

    public Status.StatusInfo status() throws ManagementException {
        Status st = new Status(commandQueue);
        return st.waitForResult();
    }

    public String hold(Hold.HoldParam holdParam) throws ManagementException {
        Hold h = new Hold(commandQueue, holdParam);
        return h.waitForResult();
    }
}

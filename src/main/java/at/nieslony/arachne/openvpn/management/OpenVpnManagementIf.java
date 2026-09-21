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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Slf4j
public class OpenVpnManagementIf {

    public enum ManagementConnectionStatus {
        Connected, Hold, Disconnected
    }

    protected final Runnable onConnect;

    private final BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> managementMsgQueue = new LinkedBlockingQueue<>();
    private Future<?> commandProcessor;
    private Future<?> managementMsgReader;
    private Future<?> managementMsgProcessor;
    private volatile Command currentCommand = null;
    private volatile SocketChannel clientChannel = null;
    private final Path socketPath;
    private final String logPrefix;

    private final ExecutorService executorService;

    @Getter
    private volatile ManagementConnectionStatus managementConnectionStatus;

    public OpenVpnManagementIf(
            Runnable onConnect,
            Path socketPath,
            String logPrefix
    ) {
        this.onConnect = onConnect;
        this.socketPath = socketPath;
        this.logPrefix = logPrefix;
        setManagementConnectionStatus(ManagementConnectionStatus.Disconnected);

        executorService = Executors.newFixedThreadPool(5);
    }

    private Runnable createCommandProcessor() {
        return () -> {

            log.info("Starting command processor");
            try {
                for (;;) {
                    Command cmd = commandQueue.take();
                    log.info("Took command from queue: " + cmd.toString());
                    try {
                        if (clientChannel == null || !clientChannel.isOpen()) {
                            commandQueue.remove(cmd);
                            String msg
                                    = "Cannot connect to management interface on"
                                    + socketPath;
                            log.warn(msg);
                            cmd.cancel(new ManagementException(msg));
                        } else {
                            log.debug("Write command");
                            int len = cmd.writeCommand(clientChannel);
                            log.debug("%s (%d bytes) written".formatted(cmd.toString(), len));
                            currentCommand = cmd;
                            log.debug("Waiting for command %s unlock".formatted(cmd.toString()));
                            try {
                                cmd.waitForUnlock();
                            } catch (TimeoutException tEx) {
                                cmd.cancel(new ManagementException("Timeout"));
                            }
                            log.debug("Command %s is unlocked".formatted(cmd.toString()));
                        }
                    } catch (IOException ex) {
                        log.error("Cannot write Command: " + ex.getMessage());
                    }
                }
            } catch (InterruptedException | ExecutionException ex) {
                log.error("CommandProcessor (%s) interrupted"
                        .formatted(logPrefix)
                );
            }
        };
    }

    private Runnable createManagementMsgReader() {
        return () -> {
            log.info("Starting management message reader");
            StringBuilder currentLine = new StringBuilder();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            for (;;) {
                int sleep = 1;
                while (clientChannel == null || !clientChannel.isOpen()) {
                    try {
                        commandQueue.clear();
                        waitForSocket();
                        connectToManagementInterface();
                    } catch (IOException ex) {
                        try {
                            log.error(
                                    "Error connecting to %s: %s. Try again in %d secs"
                                            .formatted(
                                                    socketPath,
                                                    ex.getMessage(),
                                                    sleep
                                            )
                            );
                            //log.debug("Sleeping %d secs".formatted(sleep));
                            Thread.sleep(sleep * 1000);
                            if (executorService.isShutdown()) {
                                log.info("Executor Service is shutdown. Exiting Reader.");
                                return;
                            }
                        } catch (InterruptedException iEx) {
                        }
                        if (sleep < 32) {
                            sleep *= 2;
                        }
                    }
                }
                try {
                    log.debug("Waiting for data");
                    buffer.clear();
                    int len = clientChannel.read(buffer);
                    log.debug("%d bytes read".formatted(len));
                    if (len < 0) {
                        clientChannel.close();
                        setManagementConnectionStatus(ManagementConnectionStatus.Disconnected);
                    }
                } catch (IOException ex) {
                    log.error("Error reading from socket: " + ex.getMessage());
                    if (clientChannel != null) {
                        try {
                            clientChannel.close();
                            clientChannel = null;
                        } catch (IOException ex1) {
                        }
                    }
                    continue;
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
        };
    }

    private Runnable createManagementMsgProcessor() {
        log.info("Starting management message processor");
        return () -> {
            try {
                for (;;) {
                    String curLine = managementMsgQueue.take();
                    //log.debug("Processing line: »%s«".formatted(curLine));
                    if (curLine.startsWith(">INFO:")) {
                        log.info("Connected to management interface, got INFO");
                        setManagementConnectionStatus(ManagementConnectionStatus.Connected);
                        onConnect.run();
                    } else if (curLine.startsWith(">HOLD:")) {
                        log.info("Management interface is in hold status");
                        setManagementConnectionStatus(ManagementConnectionStatus.Hold);
                    } else if (currentCommand != null) {
                        try {
                            if (currentCommand.processResultLine(curLine)) {
                                log.info("Last line of %s added".formatted(
                                        currentCommand.toString()
                                ));
                                try {
                                    currentCommand.processResult();
                                } catch (Exception ex) {
                                    log.error("Caught exception while processing result: " + ex.getMessage());
                                    throw new ManagementException(
                                            "Error processing result", ex
                                    );
                                }
                                log.info("Result of %s processed, removing command".formatted(
                                        currentCommand.toString()
                                ));
                                currentCommand = null;
                            }
                        } catch (ManagementException ex) {
                            log.warn("Got exception while processing line: " + ex.getMessage());
                            currentCommand.cancel(ex);
                            currentCommand = null;
                        }
                    } else {
                        log.warn("Unexpected line from management interface: " + curLine);
                    }
                }
            } catch (InterruptedException ex) {
                log.error("ManagementMsgProcesor (%s) interrupted"
                        .formatted(logPrefix)
                );
            }
        };
    }

    public void run() {
        log.info("Starting threads (%s)"
                .formatted(logPrefix)
        );
        managementMsgReader = executorService.submit(createManagementMsgReader());
        managementMsgProcessor = executorService.submit(createManagementMsgProcessor());
        commandProcessor = executorService.submit(createCommandProcessor());
    }

    public void stop() {
        log.info("Cancelling threads (%s)"
                .formatted(logPrefix)
        );
        commandProcessor.cancel(true);
        managementMsgProcessor.cancel(true);
        managementMsgReader.cancel(true);

        log.info("Shutting down executorService (%s)"
                .formatted(logPrefix)
        );
        executorService.shutdownNow();
    }

    private void connectToManagementInterface() throws IOException {
        //log.info("Connecting to management Socket");
        clientChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(
                socketPath
        );
        clientChannel.connect(address);
        clientChannel.configureBlocking(true);
        setManagementConnectionStatus(ManagementConnectionStatus.Connected);
    }

    private synchronized void setManagementConnectionStatus(ManagementConnectionStatus status) {
        log.debug("New Connection Status %s: %s"
                .formatted(logPrefix, status.toString())
        );
        managementConnectionStatus = status;
    }

    private void waitForSocket() {
        if (Files.exists(socketPath)) {
            log.info("Socket %s already exists".formatted(
                    socketPath.toString())
            );
            return;
        }
        try {
            //log.info("Waiting for socket to appear.");
            WatchService watchService = FileSystems.getDefault().newWatchService();

            Path socketDir = socketPath.getParent();
            socketDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    String filename = event.context().toString();
                    //log.debug("File created: " + filename);
                    if (filename.equals(socketPath.getFileName().toString())) {
                        // log.debug("Socket appeared");
                        return;
                    } else {
                        /*log.debug("Not mine. Expected: %s, got: %s".formatted(
                                getSocketPath().getFileName().toString(),
                                filename
                        ));*/
                    }
                }
                key.reset();
            }
        } catch (IOException | InterruptedException ex) {
            log.error("Error waiting for socket: " + ex.getMessage());
        }
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.exceptions.ManagementInterfaceException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

/**
 *
 * @author claas
 */
@ManagedBean
@ApplicationScoped
public class ManagementInterface {
    private BufferedReader miReader;
    private PrintWriter miWriter;
    private Socket socket;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());
    private static final transient DateFormat dateFormat =
            new SimpleDateFormat("E MMM d HH:mm:ss yyyy", Locale.US);

    /**
     * Creates a new instance of ManagementInterface
     */
    public ManagementInterface() {
    }

    @PostConstruct
    public void init() {
        try {
            ensureConnected();
        }
        catch (ManagementInterfaceException ex) {
            logger.severe(ex.getMessage());

        }
    }


    public class UserStatus {
        public UserStatus(String statusLine)
                throws NumberFormatException, ParseException, ArrayIndexOutOfBoundsException
        {
            logger.info(String.format("Creating user status: %s", statusLine));
            // claas,172.24.71.175:44800,8264,8341,Sun Aug 14 13:53:51 2016
            String[] fields = statusLine.split(",");
            user = fields[0];
            bytesReceived = Integer.parseInt(fields[2]);
            bytesSent = Integer.parseInt(fields[3]);
            connectedSince = dateFormat.parse(fields[4]);
        }

        private final String user;
        private final long bytesReceived;
        private final long bytesSent;
        private final Date connectedSince;

        public String getUser() {
            return user;
        }

        public long getBytesReceived() {
            return bytesReceived;
        }

        public long getBytesSent() {
            return bytesSent;
        }

        public Date getConnectedSince() {
            return connectedSince;
        }
    }

    public class RoutingTable {
        public RoutingTable(String line) {
            logger.info(String.format("Creating routing entry from %s", line));
        }

        private String remoteIp;
        private String localIp;
        private int localPort;
        private String user;
    }

    public String readLine() throws ManagementInterfaceException, IOException {
        String s = miReader.readLine();
        if (s == null) {
            logger.info("Read line is null, try to reconnect");
            try {
                connect("127.0.0.1", 9544);
            }
            catch (IOException ex) {
                throw new ManagementInterfaceException(
                        String.format("Cannot reconnect to 127.0.0.1:9544: %s",
                                ex.getMessage()));
            }
            s = miReader.readLine();
        }

        return s;
    }

    public void ensureConnected() throws ManagementInterfaceException {
        if (socket == null ||
                !socket.isConnected() || socket.isClosed() ||
                socket.isInputShutdown() || socket.isOutputShutdown())
        {
            try {
                connect("127.0.0.1", 9544);
            }
            catch (IOException ex) {
                throw new ManagementInterfaceException(
                        String.format("Cannot connect to 127.0.0.1:9544: %s",
                                ex.getMessage()));
            }
        }
        else {
            logger.info("Management interface socket is still open");
        }
    }

    public void connect(String addr, int port) throws IOException {
        socket = null;
        socket = new Socket(addr, port);
        socket.setSoTimeout(2000);
        miReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        miWriter  = new PrintWriter(socket.getOutputStream(),true);
        logger.info(String.format("Connected to management interface on %s:%d",
                addr, port));
    }

    private String sendCommand(String command)
           throws ManagementInterfaceException, IOException
    {
        ensureConnected();
        miWriter.println(command);
        String line = null;
        do {
            line = miReader.readLine();
            if (line == null)
                throw new ManagementInterfaceException(command, "No answer)");
        } while (processLogMessage(line));
        String[] split = line.split(": ");
        String status = split[0];
        String answer = split[1];

        if (status.equals("ERROR")) {
            throw new ManagementInterfaceException(command, answer);
        }

        return answer;
    }

    private Queue<String> sendMultiLineCommand(String command)
            throws IOException, ManagementInterfaceException
    {
        Queue<String> lines = new LinkedList<>();
        logger.info(String.format("Processing command \"%s\"", command));

        String line = null;

        ensureConnected();
        miWriter.println(command);
        do {
            do {
                logger.info("Reading next line");
                line = readLine();
                logger.info(String.format("Read line: %s", line));
            }
            while (processLogMessage(line) || processClientMessage(line));
            lines.add(line);

        } while (line != null && !line.equals("END"));

        logger.info(String.format("Read %d lines", lines.size()));
        return lines;
    }

    private boolean processLogMessage(String msg) {
        if (msg != null && msg.startsWith(">LOG:")) {
            logger.info(String.format("Found log message: %s", msg));
            return true;
        }
        else
            return false;
    }

    private boolean processClientMessage(String msg) {
        // >CLIENT:ENV
        if (msg != null && msg.startsWith(">CLIENT:")) {
            logger.info(String.format("Found client message: %s", msg));
            return true;
        }
        else
            return false;
    }

    public int getPid() throws ManagementInterfaceException, IOException {
        String ret = sendCommand("pid");
        if (ret != null)
            return Integer.getInteger(ret);
        else
            return -1;
    }

    public void getStatus(List<UserStatus> userStatus)
            throws IOException, ManagementInterfaceException
    {
        /*
status
OpenVPN CLIENT LIST
Updated,Sun Aug 14 14:12:38 2016
Common Name,Real Address,Bytes Received,Bytes Sent,Connected Since
claas,172.24.71.175:44800,8264,8341,Sun Aug 14 13:53:51 2016
ROUTING TABLE
Virtual Address,Common Name,Real Address,Last Ref
192.168.1.6,claas,172.24.71.175:44800,Sun Aug 14 13:53:51 2016
GLOBAL STATS
Max bcast/mcast queue length,0
END
        */
        Queue<String> lines = sendMultiLineCommand("status");
        userStatus.clear();

        for (String line: lines) {
            if (line != null) {
                try {
                    UserStatus us = new UserStatus(line);
                    userStatus.add(us);
                }
                catch (ArrayIndexOutOfBoundsException | NumberFormatException | ParseException ex) {
                    logger.info(String.format("Line %s does't contain user status", line));
                }
            }
        }
    }

    public void sendSigHup()
            throws ManagementInterfaceException, IOException
    {
        sendCommand("signal HUP");
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize(); //To change body of generated methods, choose Tools | Templates.

        try {
            logger.info("Closing socket");
            socket.close();
        }
        catch (IOException ex) {
            logger.warning(String.format("Canot close socket: %s", ex.getMessage()));
        }
    }
}

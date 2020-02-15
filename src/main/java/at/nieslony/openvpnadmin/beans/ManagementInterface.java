/*
 * Copyright (C) 2018 Claas Nieslony
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.exceptions.ManagementInterfaceException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 *
 * @author claas
 */
@ApplicationScoped
@Named
public class ManagementInterface
        implements Serializable
{
    private transient BufferedReader miReader;
    private transient PrintWriter miWriter;
    private transient Socket socket;
    private final transient Object socketLocker = new Object();
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    enum UserStatusField {
        CommonName,
        RemoteIpV4,
        RemoteIpV6,
        VpnIpV4,
        VpnIpV6,
        BytesReceived,
        BytesSent,
        ConnectedSinceTimeT
    };

    public enum Signal {
        // SIGHUP, SIGTERM, SIGUSR1, or SIGUSR2.

        Hup("SIGHUP"),
        Term("SIGTERM"),
        User1("SIGUSR1"),
        User2("SIGUSR2");

        private String signalName;

        private Signal(String s) {
            signalName = s;
        }

        public String getSignalName() {
            return signalName;
        }
    }

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

    public class UserStatus
            implements Serializable
    {
        public UserStatus(Map<UserStatusField, Integer> usfm, String statusLine)
                throws NumberFormatException, ParseException,
                ArrayIndexOutOfBoundsException, UnknownHostException
        {
            logger.info(String.format("Creating user status: %s", statusLine));
            // claas,172.24.71.175:44800,8264,8341,Sun Aug 14 13:53:51 2016
            //HEADER,CLIENT_LIST,Common Name,Real Address,Virtual Address,Bytes Received,Bytes Sent,Connected Since,Connected Since (time_t),Username
            //CLIENT_LIST,claas,172.24.71.185:40276,192.168.1.6,4074,3635,Sat Jun  3 11:39:41 2017,1496482781,claas
            //CLIENT_LIST,claas,172.24.71.185:39806,192.168.1.6,,4501,3946,Sat Jun  3 14:32:45 2017,1496493165,claas,0,0
            String[] fields = statusLine.split(",");
            user = fields[usfm.get(UserStatusField.CommonName)];
            remoteHost = InetAddress.getByName(fields[usfm.get(UserStatusField.RemoteIpV4)].split(":")[0]);
            vpnHost = InetAddress.getByName(fields[usfm.get(UserStatusField.VpnIpV4)].split(":")[0]);
            if (usfm.get(UserStatusField.BytesReceived) != null)
                bytesReceived = Integer.parseInt(fields[usfm.get(UserStatusField.BytesReceived)]);
            else
                bytesReceived = -1;
            if (usfm.get(UserStatusField.BytesSent) != null)
                bytesSent = Integer.parseInt(fields[usfm.get(UserStatusField.BytesSent)]);
            else
                bytesSent = -1;
            connectedSince = new Date(Long.parseLong(fields[usfm.get(UserStatusField.ConnectedSinceTimeT)]) * 1000);
        }

        private final String user;
        private InetAddress remoteHost;
        private InetAddress vpnHost;
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

        public String getRemoteHost() {
            return remoteHost.getHostAddress();
        }

        public String getVpnHost() {
            return vpnHost.getHostAddress();
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
            if (socket != null) {
                logger.info("Trying to close socket");
                try {
                    socket.close();
                }
                catch (IOException ex) {
                    logger.warning(String.format("Cannot close socket: %s", ex.getMessage()));
                }
            }

            try {
                logger.info("Connecting to management interface");
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
        logger.info(String.format("Connecting to %s:%d", addr, port));
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
        String line = null;

        ensureConnected();
        synchronized(socketLocker) {
            miWriter.println(command);
            boolean ignore_line;
            do {
                ignore_line = false;
                line = miReader.readLine();
                if (line == null)
                    throw new ManagementInterfaceException(command, "No answer)");
                if (line.startsWith(">INFO")) {
                    logger.info(String.format("Ignoring info: %s", line));
                    ignore_line = true;
                }
            } while (ignore_line || processLogMessage(line));
        }

        logger.info(String.format("Got answer: %s", line));
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

        synchronized(socketLocker) {
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
        }

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

status 2
TITLE,OpenVPN 2.3.8 x86_64-suse-linux-gnu [SSL (OpenSSL)] [LZO] [EPOLL] [MH] [IPv6] built on Aug  4 2015
TIME,Sat Jun  3 11:41:03 2017,1496482863
HEADER,CLIENT_LIST,Common Name,Real Address,Virtual Address,Bytes Received,Bytes Sent,Connected Since,Connected Since (time_t),Username
CLIENT_LIST,claas,172.24.71.185:40276,192.168.1.6,4074,3635,Sat Jun  3 11:39:41 2017,1496482781,claas
HEADER,ROUTING_TABLE,Virtual Address,Common Name,Real Address,Last Ref,Last Ref (time_t)
ROUTING_TABLE,192.168.1.6,claas,172.24.71.185:40276,Sat Jun  3 11:39:42 2017,1496482782
GLOBAL_STATS,Max bcast/mcast queue length,0
END


        */
        Queue<String> lines = sendMultiLineCommand("status 2");
        Map<UserStatusField, Integer> userStatusFieldMap = new HashMap<>();


        userStatus.clear();

        for (String line: lines) {
            if (line != null && line.startsWith("HEADER,CLIENT_LIST")) {
                String[] fieldNames = line.split(",");
                for (int i = 2; i < fieldNames.length; i++) {
                    if (fieldNames[i].equals("Common Name"))
                        userStatusFieldMap.put(UserStatusField.CommonName, i-1);
                    else if (fieldNames[i].equals("Real Address"))
                        userStatusFieldMap.put(UserStatusField.RemoteIpV4, i-1);
                    else if (fieldNames[i].equals("Virtual Address"))
                        userStatusFieldMap.put(UserStatusField.VpnIpV4, i-1);
                    else if (fieldNames[i].equals("Virtual IPv6 Address"))
                        userStatusFieldMap.put(UserStatusField.RemoteIpV6, i-1);
                    else if (fieldNames[i].equals("Bytes Received"))
                        userStatusFieldMap.put(UserStatusField.BytesReceived, i-1);
                    else if (fieldNames[i].equals("Bytes Sent"))
                        userStatusFieldMap.put(UserStatusField.BytesSent, i-1);
                    else if (fieldNames[i].equals("Connected Since (time_t)"))
                        userStatusFieldMap.put(UserStatusField.ConnectedSinceTimeT, i-1);
                    else
                        logger.info(String.format("Ignoring fieöd %s", fieldNames[i-1]));
                }
            }
            else if (line != null && line.startsWith("CLIENT_LIST,")) {
                try {
                    UserStatus us = new UserStatus(userStatusFieldMap, line);
                    userStatus.add(us);
                }
                catch (ArrayIndexOutOfBoundsException | NumberFormatException | ParseException ex) {
                    logger.info(String.format("Line %s does't contain user status (%s)",
                            line, ex.getMessage()));
                }
            }
        }
    }

    public void sendSignal(Signal s)
            throws ManagementInterfaceException, IOException
    {
        sendCommand(String.format("signal %s", s.getSignalName()));
    }

    public void reloadConfig()
            throws IOException, ManagementInterfaceException
    {
        sendSignal(Signal.Hup);
    }

    public void killUser(String username)
            throws IOException, ManagementInterfaceException
    {
        logger.info(String.format("Kill user %s", username));
        sendCommand(String.format("kill %s", username));
    }

    @PreDestroy
    public void destroy() {
        logger.info("Closing socket to management interface");

        try {
            sendCommand("quit");
        }
        catch (IOException | ManagementInterfaceException ex) {
            logger.warning(String.format("Cannot quit mamagement interface: %s", ex.getMessage()));
        }

        try {
            if (socket != null)
                socket.close();
        }
        catch (IOException ex) {
            logger.warning(String.format("Canot close socket: %s", ex.getMessage()));
        }
    }
}

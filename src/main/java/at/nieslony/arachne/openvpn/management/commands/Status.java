/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.management.commands;

import at.nieslony.arachne.openvpn.management.ManagementException;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Slf4j
public class Status extends MultiLineCommand<Status.StatusInfo> {

    private static final String MSG_ERROR = "Error parsing status.";
    private static final Pattern TIME_PATTERN = Pattern.compile("^TIME\t[0-9\\- :]+\t(\\d+)$");
    private static final Pattern CLIENT_LIST_HEADER = Pattern.compile("^HEADER\tCLIENT_LIST.*");
    private static final Pattern CONNECTED_CLIENT = Pattern.compile(
            "^CLIENT_LIST\t"
            + "(?<cn>[a-zA-Z0-9.@-]+)\t"
            + "(?<realAddr>[0-9.]+):[0-9]+\t"
            + "(?<virtAddr>[0-9.]+)\t"
            + "(?<virtAddrV6>[a-fA-F0-9:]*)\t"
            + "(?<bytesRcvt>[0-9]+)\t"
            + "(?<bytesSent>[0-9]+)\t"
            + "(?<connectedSince>[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2})\t"
            + "(?<connectedSinceInt>[0-9]+)\t"
            + "(?<username>[a-zA-Z0-9.@-]+)\t"
            + "(?<clientId>[0-9]+)\t"
            + "(?<peerId>[0-9]+)\t"
            + "(?<dataChannelCipher>[A-Z0-9\\-]+)"
            + ".*$"
    );
    Pattern ROUTING_TABLE_HEADER = Pattern.compile("^HEADER\tROUTING_TABLE.*");
    Pattern STATS = Pattern.compile(
            "^GLOBAL_STATS\t"
            + "(?<key>[^\t]+)\t"
            + "(?<value>.*)"
            + "$"
    );

    public record ConnectionStatus(
            String username,
            InetAddress realAddress,
            InetAddress virtualAddress,
            Long byteReceived,
            Long bytesSent,
            Date connectedSince,
            Integer clientId,
            Integer peerId,
            String dataChannelCipher
            ) {

        public static ConnectionStatus notConnected(String username) {
            return new ConnectionStatus(
                    username,
                    null, null,
                    null, null,
                    null,
                    null, null,
                    null
            );
        }
    }

    public record StatusInfo(
            Date time,
            List<ConnectionStatus> connectionStatus,
            Map<String, Integer> globalStats
            ) {

    }

    public Status(BlockingQueue<Command> queue) {
        super(queue, "status 3");
    }

    @Override
    public void processResult() throws ManagementException {
        log.info("Processing result: " + lines);
        String line;

        ListIterator<String> it = lines.listIterator();
        // Skip head line
        line = it.next();
        if (!line.startsWith("TITLE")) {
            log.error("Error parsing status. \"TITLE\" expected, but got " + line);
            throw new ManagementException(MSG_ERROR);
        }

        // date
        line = it.next();
        Matcher timeMatcher = TIME_PATTERN.matcher(line);
        if (!timeMatcher.matches()) {
            log.error("Error parsing status time: " + line);
            throw new ManagementException(MSG_ERROR);
        }
        long dateLong = Long.parseLong(timeMatcher.group(1));
        Date date = Date.from(Instant.ofEpochSecond(dateLong));
        log.info("Got date: " + date);

        // HEADER  CLIENT_LIST
        line = it.next();
        Matcher clientListHeaderMatcher = CLIENT_LIST_HEADER.matcher(line);
        if (!clientListHeaderMatcher.matches()) {
            log.error("HEADER CLIENT_LIST expected but found " + line);
            throw new ManagementException(MSG_ERROR);
        }

        // Client list
        List<ConnectionStatus> connectionStatus = new LinkedList<>();
        Matcher clientMatcher = CONNECTED_CLIENT.matcher(it.next());
        while (clientMatcher.matches()) {
            log.info("Parsing " + line);
            try {
                String username = clientMatcher.group("cn");
                InetAddress realAddress = InetAddress.getByName(clientMatcher.group("realAddr"));
                InetAddress virtualAddress = InetAddress.getByName(clientMatcher.group("virtAddr"));
                long bytesReceived = Long.parseLong(clientMatcher.group("bytesRcvt"));
                long bytesSent = Long.parseLong(clientMatcher.group("bytesSent"));
                Date connectedSince = Date.from(
                        Instant.ofEpochSecond(Long.parseLong(
                                clientMatcher.group("connectedSinceInt")
                        ))
                );
                int clientId = Integer.parseInt(clientMatcher.group("clientId"));
                int peerId = Integer.parseInt(clientMatcher.group("peerId"));
                String dataChannelCipher = clientMatcher.group("dataChannelCipher");
                ConnectionStatus conStatus = new ConnectionStatus(
                        username,
                        realAddress,
                        virtualAddress,
                        bytesReceived,
                        bytesSent,
                        connectedSince,
                        clientId,
                        peerId,
                        dataChannelCipher
                );
                log.info(conStatus.toString());
                connectionStatus.add(conStatus);

                clientMatcher = CONNECTED_CLIENT.matcher(it.next());
            } catch (NumberFormatException | UnknownHostException ex) {
                log.error(
                        "Error parsing client list entry %s: "
                                .formatted(line, ex.getMessage())
                );
                throw new ManagementException(MSG_ERROR);
            }
        }

        line = it.previous();
        Matcher routingTableHeaderMatcher = ROUTING_TABLE_HEADER.matcher(line);
        if (!routingTableHeaderMatcher.matches()) {
            log.error("HEADER ROUTING_TABLE expected but found " + line);
            throw new ManagementException(MSG_ERROR);
        }

        it.next();
        line = it.next();
        while (line.startsWith("ROUTING_TABLE")) {
            line = it.next();
        }
        it.previous();

        Map<String, Integer> globalStats = new HashMap<>();
        while (it.hasNext()) {
            line = it.next();
            log.info("Parsing stats " + line);
            Matcher statsMatcher = STATS.matcher(line);
            if (statsMatcher.matches()) {
                globalStats.put(statsMatcher.group("key"),
                        Integer.valueOf(statsMatcher.group("value"))
                );
            } else {
                log.error(
                        "Error parsing GLOBAL_STATS: tab-separated key-value-pait expected but got "
                        + line
                );
                throw new ManagementException(MSG_ERROR);
            }
        }
        log.info("Got global stats: " + globalStats);

        log.info("Value complete");

        value.complete(new StatusInfo(date, connectionStatus, globalStats));
    }
}
/*
[19:15:34] claas@len-s4dh7055:/home/claas/NetBeansProjects/arachne> socat UNIX-CONNECT:/tmp/openvpn-test/management.sock STDIO
>INFO:OpenVPN Management Interface Version 5 -- type 'help' for more info
status 3
TITLE   OpenVPN 2.6.22 x86_64-redhat-linux-gnu [SSL (OpenSSL)] [LZO] [LZ4] [EPOLL] [PKCS11] [MH/PKTINFO] [AEAD] [DCO]
TIME    2026-09-14 19:15:46     1789406146
HEADER       CLIENT_LIST  Common Name   Real Address    Virtual Address Virtual IPv6 Address    Bytes Received  Bytes Sent      Connected Since     Connected Since (time_t)        Username        Client ID       Peer ID Data Channel Cipher
CLIENT_LIST               test-client   127.0.0.1:42310	192.168.1.2                             18427           10433           2026-09-15 23:34:00 1789508040                      UNDEF           0               0       AES-256-GCM
HEADER  ROUTING_TABLE   Virtual Address Common Name     Real Address    Last Ref        Last Ref (time_t)
GLOBAL_STATS    Max bcast/mcast queue length    0
GLOBAL_STATS    dco_enabled     0
END
quit
[19:20:48] claas@len-s4dh7055:/home/claas/NetBeansProjects/arachne> socat UNIX-CONNECT:/tmp/openvpn-test/management.sock STDIO
>INFO:OpenVPN Management Interface Version 5 -- type 'help' for more info
status 3
OpenVPN STATISTICS
Updated,2026-09-14 19:21:24
TUN/TAP read bytes,0
TUN/TAP write bytes,0
TCP/UDP read bytes,0
TCP/UDP write bytes,0
Auth read bytes,0
END
^C[19:55:50] claas@len-s4dh7055:/home/claas/NetBeansProjects/arachne>

 */

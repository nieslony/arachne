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

/**
 *
 * @author claas
 */
public class Status extends MultiLineCommand<Status.StatusInfo> {

    public record ConnectionStatus(
            String username,
            InetAddress realAddress,
            InetAddress virtualAddress,
            long byteReceived,
            long bytesSent,
            Date connectedSince,
            int clientId,
            int peerId,
            String dataChannelCipher
            ) {

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
        int lineNr = 1;
        String dateStr = lines.get(lineNr++).split("\t")[2];
        long dateLong = Long.parseLong(dateStr);
        Date date = Date.from(Instant.ofEpochSecond(dateLong));

        lineNr++; // HEADER  CLIENT_LIST
        List<ConnectionStatus> connectionStatus = new LinkedList<>();
        while (lines.get(lineNr).startsWith("CLIENT_LIST")) {
            try {
                String[] fields = lines.get(lineNr).split("\t");
                int fieldNr = 1;

                String username = fields[fieldNr++];
                String realAddressStr = fields[fieldNr++].split(":")[1];
                InetAddress readAddress = InetAddress.getByName(realAddressStr);
                InetAddress virtualAddress = InetAddress.getByName(fields[fieldNr++]);
                fieldNr++; // Virtual IPv6 Address
                long bytesReceived = Long.parseLong(fields[fieldNr++]);
                long bytesSend = Long.parseLong(fields[fieldNr++]);
                fieldNr++; // Connected Since (string)
                Date connectedSince = Date.from(
                        Instant.ofEpochSecond(Long.parseLong(fields[fieldNr++]))
                );
                fieldNr++; // username
                int clientId = Integer.parseInt(fields[fieldNr++]);
                int peerId = Integer.parseInt(fields[fieldNr++]);
                String dataChannelCipher = fields[fieldNr++];

                connectionStatus.add(new ConnectionStatus(
                        username,
                        readAddress,
                        virtualAddress,
                        bytesReceived,
                        bytesSend,
                        connectedSince,
                        clientId,
                        peerId,
                        dataChannelCipher
                ));

                lineNr++;
            } catch (UnknownHostException ex) {
                throw new ManagementException("Cannot parse address", ex);
            }
        }
        lineNr++; // HEADER  ROUTING_TABLE
        while (lines.get(lineNr).startsWith("ROUTING_TABLE")) {
            lineNr++;
        }
        Map<String, Integer> globalStats = new HashMap<>();
        while (lineNr < lines.size() && lines.get(lineNr).startsWith("GLOBAL_STATS")) {
            String[] tokens = lines.get(lineNr).split("\t");
            globalStats.put(
                    tokens[1],
                    Integer.parseInt(tokens[2])
            );
            lineNr++;
        }

        value.complete(new StatusInfo(date, connectionStatus, globalStats));
    }
}

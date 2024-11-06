package at.nieslony.arachne.utils.net;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
/**
 *
 * @author claas
 */
public record DnsServiceName(String name, String description) {

    private static final Logger logger = LoggerFactory.getLogger(DnsServiceName.class);

    static private Map<String, DnsServiceName> knownServices = null;

    public static Map<String, DnsServiceName> getKnownServices() {
        if (knownServices == null) {
            knownServices = new HashMap<>();
            try {
                Path path = Paths
                        .get(ClassLoader
                                .getSystemResource("KnownDnsSrvTypes/known-dns-srv-types.csv").toURI()
                        );
                Files.lines(path).forEach((String line) -> {
                    String[] splitLine = line.split("\t");
                    knownServices.put(
                            splitLine[0],
                            new DnsServiceName(splitLine[0], splitLine[1])
                    );
                });
            } catch (IOException | URISyntaxException ex) {
                logger.error("Cannot read SRV names from resource: " + ex.getMessage());
            }
        }

        return knownServices;
    }

    public static DnsServiceName getService(String name) {
        return getKnownServices().get(name);
    }
}

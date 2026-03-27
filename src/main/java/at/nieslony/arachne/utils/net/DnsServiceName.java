package at.nieslony.arachne.utils.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
/**
 *
 * @author claas
 */
@Slf4j
public record DnsServiceName(String name, String description) {

    private static final String RN_KNOWN_SRV_TYPES = "KnownDnsSrvTypes/known-dns-srv-types.csv";

    static private Map<String, DnsServiceName> knownServices = null;

    public static Map<String, DnsServiceName> getKnownServices() {
        if (knownServices == null) {
            knownServices = new HashMap<>();
            try {
                var resource = new ClassPathResource(RN_KNOWN_SRV_TYPES);
                if (!resource.exists()) {
                    log.error("Cannot find resource " + RN_KNOWN_SRV_TYPES);
                } else {
                    var reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));

                    reader.lines().forEach((String line) -> {
                        String[] splitLine = line.split("\t");
                        if (splitLine == null || splitLine.length != 2) {
                            log.warn("Cannot split line: " + line);
                        } else {
                            knownServices.put(
                                    splitLine[0],
                                    new DnsServiceName(splitLine[0], splitLine[1])
                            );
                        }
                    });
                }
            } catch (IOException ex) {
                log.error("Cannot read SRV names from resource: " + ex.getMessage());
            }
        }

        return knownServices;
    }

    public static DnsServiceName getService(String name) {
        return getKnownServices().get(name);
    }
}

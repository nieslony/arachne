/*
 * Copyright (C) 2023 claas
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
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.utils.FolderFactory;
import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author claas
 */
@Getter
@Setter
public class FirewalldService {

    private static final Logger logger = LoggerFactory.getLogger(FirewalldService.class);

    public record Port(int port, String protocol) {

        @Override
        public String toString() {
            return "%d/%s".formatted(port, protocol);
        }
    }

    public record PortRange(int from, int to, String protocol) {

        @Override
        public String toString() {
            return "%d-%d/%s".formatted(from, to, protocol);
        }
    }

    private String name;
    private String shortDescription;
    private String longDescription;
    private List<Port> ports;
    private List<PortRange> portRanges;

    private FirewalldService(String name, Document doc) throws Exception {
        this.name = name;

        String rootName = doc.getDocumentElement().getNodeName();
        if (!rootName.equals("service")) {
            throw new Exception("Not a service");
        }

        Node shortNode = doc.getElementsByTagName("short").item(0);
        if (shortNode == null) {
            throw new Exception("Service has no tag short");
        } else {
            shortDescription = shortNode.getTextContent();
        }

        Node descriptionNode = doc.getElementsByTagName("short").item(0);
        if (shortNode == null) {
            throw new Exception("Service has no tag description");
        } else {
            longDescription = descriptionNode.getTextContent();
        }

    }

    static private Map<String, FirewalldService> allServices = null;
    FolderFactory folderFactory = FolderFactory.getInstance();

    static public Collection<FirewalldService> getAllServices() {
        if (allServices == null) {
            readAll();
        }

        return allServices.values();
    }

    static public FirewalldService getService(String name) {
        if (allServices == null) {
            readAll();
        }

        return allServices.get(name);
    }

    public static void readAll() {
        if (allServices == null) {
            allServices = new HashMap<>();
        } else {
            allServices.clear();
        }

        String firewalldServiceDir = FolderFactory.getInstance().getFirewalldServiceDir();
        DocumentBuilderFactory factory;
        DocumentBuilder docBuilder;
        try {
            factory = DocumentBuilderFactory.newInstance();
            docBuilder = factory.newDocumentBuilder();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                Paths.get(firewalldServiceDir)
        )) {
            for (Path path : stream) {
                if (Files.isRegularFile(path) && path.toString().endsWith(".xml")) {
                    File xmlFile = new File(path.toString());
                    String filename = path.getFileName().toString();
                    String serviceName = filename.substring(0, filename.lastIndexOf("."));
                    Document doc = docBuilder.parse(xmlFile);
                    doc.getDocumentElement().normalize();
                    try {
                        allServices.put(
                                serviceName,
                                new FirewalldService(serviceName, doc)
                        );
                    } catch (Exception ex) {
                        logger.error(
                                "Cannot parse %s: %s"
                                        .formatted(path.toString(), ex.getMessage())
                        );
                    }
                }
            }
        } catch (Exception ex) {
            logger.error(
                    "Error reading files from %s: %s"
                            .formatted(
                                    firewalldServiceDir,
                                    ex.getMessage())
            );
        }
    }
}

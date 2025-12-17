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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author claas
 */
@Getter
@Setter
@Slf4j
public class FirewalldService {

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

    public record ProtocolPort(String protocol, String port) {

        @Override
        public String toString() {
            return "%s/%s".formatted(port, protocol);
        }
    }

    private String name;
    private String shortDescription;
    private String longDescription;
    private List<Port> ports;
    private List<PortRange> portRanges;
    private List<String> includes;
    private List<ProtocolPort> protocolPorts;

    private FirewalldService(String name, Document doc) throws Exception {
        this.name = name;
        this.includes = new LinkedList<>();
        this.protocolPorts = new LinkedList<>();

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

        Node descriptionNode = doc.getElementsByTagName("description").item(0);
        if (descriptionNode == null) {
            throw new Exception("Service has no tag description");
        } else {
            longDescription = descriptionNode.getTextContent();
        }

        NodeList incs = doc.getElementsByTagName("include");
        if (incs != null) {
            for (int i = 0; i < incs.getLength(); i++) {
                includes.add(
                        incs
                                .item(i)
                                .getAttributes()
                                .getNamedItem("service")
                                .getNodeValue()
                );
            }
        }

        NodeList ports = doc.getElementsByTagName("port");
        if (ports != null) {
            for (int i = 0; i < ports.getLength(); i++) {
                var attrs = ports.item(i).getAttributes();
                String protocol = attrs.getNamedItem("protocol").getNodeValue();
                String port = attrs.getNamedItem("port").getNodeValue();
                protocolPorts.add(new ProtocolPort(protocol, port));
            }
        }
    }

    static private Map<String, FirewalldService> allServices = null;
    FolderFactory folderFactory = FolderFactory.getInstance();

    static public List<FirewalldService> getAllServices() {
        if (allServices == null) {
            readAll();
        }

        List<FirewalldService> services = new LinkedList<>(allServices.values());
        Collections.sort(
                services,
                (s1, s2) -> s1.getShortDescription().compareTo(s2.getShortDescription())
        );
        return services;
    }

    static public FirewalldService getService(String name) {
        if (allServices == null) {
            readAll();
        }

        return allServices.get(name);
    }

    public boolean matchesPortAndProtocol(int port, String protocol) {
        if (getPorts() != null
                && getPorts()
                        .stream()
                        .filter((p) -> p.port() == port && p.protocol().equals(protocol))
                        .findFirst()
                        .isPresent()) {
            return true;
        }
        if (getPortRanges() != null
                && getPortRanges()
                        .stream()
                        .filter(
                                (pr)
                                -> ((port >= pr.from && port <= pr.to)
                                && pr.protocol.equals(protocol))
                        )
                        .findFirst()
                        .isPresent()) {
            return true;
        }
        return false;
    }

    static public FirewalldService getByPortAndProtocol(int port, String protocol) {
        if (allServices == null) {
            readAll();
        }

        return allServices
                .values()
                .stream()
                .filter((s) -> s.matchesPortAndProtocol(port, protocol))
                .findFirst()
                .orElse(null);
    }

    static private Set<FirewalldService> getServiceRecursive(String name, int count) {
        Set<FirewalldService> services = new HashSet<>();
        FirewalldService srv = getService(name);

        if (srv == null) {
            return services;
        }

        if (srv.protocolPorts != null && !srv.protocolPorts.isEmpty()) {
            services.add(srv);
        }

        if (count > 0 && srv.includes != null) {
            for (String inc : srv.includes) {
                services.addAll(getServiceRecursive(inc, count - 1));
            }
        }

        return services;
    }

    static public Set<FirewalldService> getServiceRecursive(String name) {
        return getServiceRecursive(name, 10);
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
                        log.error(
                                "Cannot parse %s: %s"
                                        .formatted(path.toString(), ex.getMessage())
                        );
                    }
                }
            }
        } catch (Exception ex) {
            log.error(
                    "Error reading files from %s: %s"
                            .formatted(
                                    firewalldServiceDir,
                                    ex.getMessage())
            );
        }
    }

    public Component createInfoPopover(Component parent) {
        Popover popover = new Popover();
        popover.setTarget(parent);
        popover.setPosition(PopoverPosition.END);
        popover.addThemeVariants(PopoverVariant.AURA_ARROW);
        popover.setOpenOnClick(false);
        popover.setOpenOnHover(true);
        popover.setWidth("32em");

        Div poTitle = new Div(getShortDescription());
        poTitle.addClassNames(LumoUtility.FontWeight.BOLD);

        UnorderedList poPorts = new UnorderedList();
        if (getProtocolPorts() != null) {
            getProtocolPorts().forEach((port) -> {
                poPorts.add(new ListItem(port.toString()));
            });
        }
        getIncludes().forEach(include -> {
            FirewalldService incSrv = FirewalldService.getService(include);
            incSrv.getProtocolPorts().forEach(port -> {
                String label = "%s (%s)".formatted(
                        port.toString(),
                        incSrv.getShortDescription()
                );
                poPorts.add(new ListItem(label));
            });
        });

        Div poDescription = new Div(getLongDescription());
        poDescription.addClassNames(LumoUtility.FontSize.SMALL);
        poDescription.addClassNames(LumoUtility.TextAlignment.JUSTIFY);

        popover.add(
                poTitle,
                new Text("Ports:"), poPorts,
                poDescription
        );

        return popover;
    }
}

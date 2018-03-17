/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author claas
 */

@ApplicationScoped
@ManagedBean
public class FirewallDServices implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    public class Service {
        private String _name;
        private String _shortName;
        private String _description;
        private List<String> _ports;

        public Service() {}
        public Service(String name,
            String shortName,
            String description,
            List<String> ports)
        {
            init(name, shortName, description, ports);
        }

        public void init(String name,
            String shortName,
            String description,
            List<String> ports)
        {
            _name = name;
            _shortName = shortName;
            _description = description;
            _ports = ports;
        }

        public String getName() {
            return _name;
        }

        public String getShortDescription() {
            return _shortName;
        }

        public String getDescription() {
            return _description;
        }

        public List<String> getPorts() {
            return _ports;
        }

        public String getPortsStr() {
            return String.join(", ", _ports);
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(_shortName).append(" ( ");
            for (String p: _ports) {
                buf.append(p).append(" ");
            }
            buf.append(")");

            return buf.toString();
        }
    }

    public FirewallDServices() {
        loadServices("/usr/lib/firewalld/services");
    }


    private List<Service> services = new LinkedList<>();

    private Service loadFile(String filename) {
        try {
            logger.info(String.format("Loading services from %s...", filename));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(filename));

            doc.getDocumentElement().normalize();

            String name;
            String shortName = doc.getElementsByTagName("short").item(0).getTextContent();
            String description = doc.getElementsByTagName("description").item(0).getTextContent();
            List<String> ports = new LinkedList<>();
            NodeList portList = doc.getElementsByTagName("port");

            for (int i = 0; i < portList.getLength(); i++) {
                Element elem = (Element) portList.item(i);
                String protocol = elem.getAttribute("protocol");
                String port = elem.getAttribute("port");

                StringBuilder buf = new StringBuilder();
                buf.append(port).append("/").append(protocol);

                ports.add(buf.toString());
            }

            FirewallDServices.Service service = new FirewallDServices.Service("", shortName, description, ports);

            return service;
        }
        catch (Exception ex) {
            logger.warning(ex.getMessage());
        }

        return null;
    }

    public void loadServices(String dirName) {
        try (Stream<Path> paths = Files.walk(Paths.get(dirName))) {
            paths
                .filter(Files::isRegularFile)
                .forEach(p -> services.add(loadFile(p.toString())));
        }
        catch (IOException ex) {
            logger.warning(String.format("Error loading %s", ex.getMessage()));
        }

        Collections.sort(services,
                (Service t, Service t1) -> t._shortName.toLowerCase().compareTo(t1._shortName.toLowerCase()));
    }

    public List<Service> getServices() {
        return services;
    }
}

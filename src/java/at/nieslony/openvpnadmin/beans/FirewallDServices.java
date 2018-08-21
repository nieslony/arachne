/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.FirewallDService;
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
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author claas
 */

@ApplicationScoped
@ManagedBean
public class FirewallDServices implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());
    private static final transient String SERVICES_DIR = "/usr/lib/firewalld/services";


    public FirewallDServices() {
    }

    static private List<FirewallDService> _services = null;

    static private FirewallDService loadFile(String filename) {
        try {
            logger.info(String.format("Loading services from %s...", filename));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(filename));

            doc.getDocumentElement().normalize();

            NodeList elems;
            elems = doc.getElementsByTagName("short");
            String shortName = "";
            if (elems != null && elems.getLength() > 0)
                shortName = elems.item(0).getTextContent();

            String description = "";
            elems = doc.getElementsByTagName("description");
            if (elems != null && elems.getLength() > 0)
                description = elems.item(0).getTextContent();

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

            FirewallDService service = new FirewallDService(shortName, description, ports);

            return service;
        }
        catch (IOException | ParserConfigurationException | DOMException | SAXException ex) {
            logger.warning(ex.getMessage());
        }

        return null;
    }

    static private List<FirewallDService> loadServices(String dirName) {
        List<FirewallDService> services = new LinkedList<>();

        logger.info(String.format("Loading firewall services from %s", dirName));
        try (Stream<Path> paths = Files.walk(Paths.get(dirName))) {
            paths
                .filter(Files::isRegularFile)
                .forEach(p -> {
                    FirewallDService s = loadFile(p.toString());
                    if (s != null)
                        services.add(s);
                    });
        }
        catch (IOException ex) {
            logger.warning(String.format("Error loading %s", ex.getMessage()));
        }

        Collections.sort(services,
                (FirewallDService t, FirewallDService t1) ->
                        t.getShortDescription().toLowerCase().compareTo(t1.getShortDescription().toLowerCase()));

        return Collections.unmodifiableList(services);
    }

    public List<FirewallDService> getServices() {
        if (_services == null) {
            _services = loadServices(SERVICES_DIR);
        }
        return _services;
    }

    public FirewallDService getServiceByShort(String s) {
        List<FirewallDService> services = getServices();
        for (FirewallDService fs : services) {
            if (fs.getShortDescription().equals(s))
                return fs;
        }

        return null;
    }

    static public List<FirewallDService> getAllServices() {
        if (_services == null) {
            _services = loadServices(SERVICES_DIR);
        }
        return _services;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author claas
 */
public class FirewallDService {
    private String _shortName;
    private String _description;
    private List<String> _ports;

    public FirewallDService() {
        init("", "", new LinkedList<>());
    }

    public FirewallDService(String shortName,
        String description,
        List<String> ports)
    {
        init(shortName, description, ports);
    }

    public void init(String shortName,
        String description,
        List<String> ports)
    {
        _shortName = shortName;
        _description = description;
        _ports = ports;
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
}

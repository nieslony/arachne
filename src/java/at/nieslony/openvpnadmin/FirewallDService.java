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
	private String _id;
    private String _shortName;
    private String _description;
    private List<PortProtocol> _portsProtocols;

    public FirewallDService() {
        init("", "", "", new LinkedList<>());
    }

    public FirewallDService(String id,
            String shortName,
            String description,
            List<PortProtocol> ports)
    {
        init(id, shortName, description, ports);
    }

    private void init(String id,
            String shortName,
            String description,
            List<PortProtocol> portsProtocols)
    {
        _id = id;
        _shortName = shortName;
        _description = description;
        _portsProtocols = portsProtocols;
    }

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        _id = id;
    }

    public String getShortDescription() {
        return _shortName;
    }

    public String getDescription() {
        return _description;
    }

    public List<PortProtocol> getPortsProtocols() {
        return _portsProtocols;
    }

    public String getPortsProtocolsStr() {  
    	List<String> l = new LinkedList<>();
    	for (PortProtocol pp: _portsProtocols)
    		l.add(pp.getPortPorotol());
    	
        return String.join(", ", l);
    }
}

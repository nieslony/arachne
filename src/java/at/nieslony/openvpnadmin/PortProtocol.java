package at.nieslony.openvpnadmin;

public class PortProtocol {
	private String _port;
	private String _protocol;
	
	public PortProtocol(String port, String protocol) {
		this._port = port;
		this._protocol = protocol;
	}
	
	public String getPort() {
		return _port;
	}
	
	public String getProtocol() {
		return _protocol;
	}
	
	public String getPortPorotol() {
		StringBuffer buf = new StringBuffer();
		buf.append(_port).append("/").append(_protocol);
		
		return buf.toString();
	}
}
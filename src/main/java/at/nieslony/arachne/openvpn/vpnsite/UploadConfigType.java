/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package at.nieslony.arachne.openvpn.vpnsite;

/**
 *
 * @author claas
 */
public enum UploadConfigType {
    OvpnConfig(".ovpn file"), NMCL("NetworkManger");

    private UploadConfigType(String label) {
        this.label = label;
    }
    private final String label;

    @Override
    public String toString() {
        return label;
    }

}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package at.nieslony.arachne.roles;

/**
 *
 * @author claas
 */
public enum Role {
    //NO_ACCESS("No Access"),
    ADMIN("Administrator"),
    USER("VPN User");

    final private String roleStr;

    private Role(String roleStr) {
        this.roleStr = roleStr;
    }

    @Override
    public String toString() {
        return roleStr;
    }
}

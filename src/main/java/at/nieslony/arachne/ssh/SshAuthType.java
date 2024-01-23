/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package at.nieslony.arachne.ssh;

/**
 *
 * @author claas
 */
public enum SshAuthType {
    USERNAME_PASSWORD("Username/Password"), PRESHARED_KEY("Preshared Key");

    private final String value;

    private SshAuthType(String s) {
        value = s;
    }

    @Override
    public String toString() {
        return value;
    }

}

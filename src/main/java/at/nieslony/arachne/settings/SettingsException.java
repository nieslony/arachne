/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.settings;

/**
 *
 * @author claas
 */
public class SettingsException extends Exception {

    public SettingsException(String msg) {
        super(msg);
    }

    public SettingsException(String msg, Exception parent) {
        super(msg, parent);
    }
}

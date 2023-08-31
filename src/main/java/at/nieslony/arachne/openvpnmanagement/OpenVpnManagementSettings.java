/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpnmanagement;

import at.nieslony.arachne.settings.AbstractSettingsGroup;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author claas
 */
@Getter
@Setter
public class OpenVpnManagementSettings extends AbstractSettingsGroup {

    private String socketFilename = "arachne-management.socket";
    private String managementPassword = "management.pwd";
    private String passwordFilename = "arachne-management.pwd";
}

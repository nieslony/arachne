/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.auth;

import at.nieslony.arachne.settings.AbstractSettingsGroup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PreAuthSettings extends AbstractSettingsGroup {

    private boolean preAuthtEnabled = false;
    private String environmentVariable = "REMOTE_USER";
}

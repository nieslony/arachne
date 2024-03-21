/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.ldap;

import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author claas
 */
@RestController
@RequestMapping("/api/ldap")
public class LdapRestController {

    @Autowired
    Settings settings;

    @GetMapping("/settings")
    @RolesAllowed(value = {"ADMIN"})
    public LdapSettings getSettings() {
        LdapSettings ldapSettings = settings.getSettings(LdapSettings.class);
        return ldapSettings;
    }

    @PutMapping("/settings")
    @RolesAllowed(value = {"ADMIN"})
    public void putSettings(@RequestBody LdapSettings ldapSettings)
            throws SettingsException {
        ldapSettings.save(settings);
    }
}

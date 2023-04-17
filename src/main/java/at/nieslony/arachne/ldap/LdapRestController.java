/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.ldap;

import at.nieslony.arachne.settings.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Autowired
    LdapUserCacheRepository ldapUserCacheModel;

    @GetMapping("/settings")
    public LdapSettings getSettings() {
        LdapSettings ldapSettings = new LdapSettings(settings);
        return ldapSettings;
    }

    @PutMapping("/settings")
    public void putSettings(@RequestBody LdapSettings ldapSettings) {
        ldapSettings.save(settings);
    }

    @PostMapping("/clear_cache")
    public String clearCache() {
        long noEntries = ldapUserCacheModel.count();
        ldapUserCacheModel.deleteAll();
        return "%d entries deleted.".formatted(noEntries);
    }
}

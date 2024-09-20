/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.backup;

import at.nieslony.arachne.firewall.FirewallRuleRepository;
import at.nieslony.arachne.pki.CertificateRepository;
import at.nieslony.arachne.pki.KeyRepository;
import at.nieslony.arachne.roles.RoleRuleRepository;
import at.nieslony.arachne.settings.SettingsRepository;
import at.nieslony.arachne.tasks.RecurringTasksRepository;
import at.nieslony.arachne.tasks.TaskRepository;
import at.nieslony.arachne.users.UserRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import at.nieslony.arachne.apiindex.ApiDescription;

/**
 *
 * @author claas
 */
@RestController
public class BackupRestController {

    @Autowired
    FirewallRuleRepository firewallRuleRepository;
    @Autowired
    CertificateRepository certificateRepository;
    @Autowired
    KeyRepository keyRepository;
    @Autowired
    RoleRuleRepository roleRuleRepository;
    @Autowired
    SettingsRepository settingsRepository;
    @Autowired
    RecurringTasksRepository recurringTasksRepository;
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    UserRepository userRepository;

    @GetMapping("/api/backup")
    @RolesAllowed(value = {"ADMIN", "BACKUP"})
    @ApiDescription(
            """
            Creates backup of all settings. Can be restored with
            setup wizard after disaster.
            """
    )
    public Backup getBackup() {
        return new Backup(
                firewallRuleRepository,
                certificateRepository,
                keyRepository,
                roleRuleRepository,
                settingsRepository,
                recurringTasksRepository,
                taskRepository,
                userRepository
        );
    }
}

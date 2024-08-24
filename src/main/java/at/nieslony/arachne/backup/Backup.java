/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.backup;

import at.nieslony.arachne.firewall.FirewallRuleModel;
import at.nieslony.arachne.firewall.FirewallRuleRepository;
import at.nieslony.arachne.pki.CertificateModel;
import at.nieslony.arachne.pki.CertificateRepository;
import at.nieslony.arachne.pki.KeyModel;
import at.nieslony.arachne.pki.KeyRepository;
import at.nieslony.arachne.roles.RoleRuleModel;
import at.nieslony.arachne.roles.RoleRuleRepository;
import at.nieslony.arachne.settings.SettingsModel;
import at.nieslony.arachne.settings.SettingsRepository;
import at.nieslony.arachne.tasks.RecurringTaskModel;
import at.nieslony.arachne.tasks.RecurringTasksRepository;
import at.nieslony.arachne.tasks.TaskModel;
import at.nieslony.arachne.tasks.TaskRepository;
import at.nieslony.arachne.users.UserModel;
import at.nieslony.arachne.users.UserRepository;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 *
 * @author claas
 */
@JsonPropertyOrder({"version", "keys", "certificates"})
public class Backup {

    FirewallRuleRepository firewallRuleRepository;
    CertificateRepository certificateRepository;
    KeyRepository keyRepository;
    RoleRuleRepository roleRuleRepository;
    SettingsRepository settingsRepository;
    RecurringTasksRepository recurringTasksRepository;
    TaskRepository taskRepository;
    UserRepository userRepository;

    public Backup(
            FirewallRuleRepository firewallRuleRepository,
            CertificateRepository certificateRepository,
            KeyRepository keyRepository,
            RoleRuleRepository roleRuleRepository,
            SettingsRepository settingsRepository,
            RecurringTasksRepository recurringTasksRepository,
            TaskRepository taskRepository,
            UserRepository userRepository
    ) {
        this.settingsRepository = settingsRepository;
        this.firewallRuleRepository = firewallRuleRepository;
        this.certificateRepository = certificateRepository;
        this.keyRepository = keyRepository;
        this.roleRuleRepository = roleRuleRepository;
        this.recurringTasksRepository = recurringTasksRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public int getVersion() {
        return 1;
    }

    public List<SettingsModel> getSettings() {
        return settingsRepository.findAll();
    }

    public List<FirewallRuleModel> getFirewallRules() {
        return firewallRuleRepository.findAll();
    }

    public List<CertificateModel> getCertificates() {
        return certificateRepository.findAll();
    }

    public List<KeyModel> getKeys() {
        return keyRepository.findAll();
    }

    public List<RoleRuleModel> getRoleRules() {
        return roleRuleRepository.findAll();
    }

    public List<RecurringTaskModel> getRecurringTasks() {
        return recurringTasksRepository.findAll();
    }

    public List<TaskModel> getTasks() {
        return taskRepository.findAll();
    }

    public List<UserModel> getUsers() {
        return userRepository.findAll();
    }
}

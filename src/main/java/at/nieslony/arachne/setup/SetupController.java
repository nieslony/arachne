/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.setup;

import at.nieslony.arachne.Arachne;
import at.nieslony.arachne.apiindex.ApiMethodDescription;
import at.nieslony.arachne.firewall.FirewallRuleModel;
import at.nieslony.arachne.firewall.FirewallRuleRepository;
import at.nieslony.arachne.pki.CertSpecsValidationException;
import at.nieslony.arachne.pki.CertificateModel;
import at.nieslony.arachne.pki.CertificateRepository;
import at.nieslony.arachne.pki.KeyModel;
import at.nieslony.arachne.pki.KeyRepository;
import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.roles.Role;
import at.nieslony.arachne.roles.RoleRuleModel;
import at.nieslony.arachne.roles.RoleRuleRepository;
import at.nieslony.arachne.roles.RolesCollector;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.settings.SettingsModel;
import at.nieslony.arachne.settings.SettingsRepository;
import at.nieslony.arachne.ssh.SshKeyEntity;
import at.nieslony.arachne.ssh.SshKeyRepository;
import at.nieslony.arachne.tasks.RecurringTaskModel;
import at.nieslony.arachne.tasks.RecurringTasksRepository;
import at.nieslony.arachne.tasks.TaskModel;
import at.nieslony.arachne.tasks.TaskRepository;
import at.nieslony.arachne.tasks.TaskScheduler;
import at.nieslony.arachne.tasks.scheduled.UpdateDhParams;
import at.nieslony.arachne.usermatcher.UsernameMatcher;
import at.nieslony.arachne.users.UserModel;
import at.nieslony.arachne.users.UserRepository;
import at.nieslony.arachne.utils.FolderFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author claas
 */
@RestController
public class SetupController {

    private static final org.slf4j.Logger logger
            = LoggerFactory.getLogger(SetupController.class);

    public static final String SETUP_STATUS_KEY = "setup.status";

    public enum SetupStatus {
        FINISHED,
        RUNNING,
        FAILED
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRuleRepository roleRuleRepository;

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private RecurringTasksRepository recurringTasksRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private FirewallRuleRepository firewallRuleRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private KeyRepository keyRepository;

    @Autowired
    private SshKeyRepository SshKeyRepository;

    @Autowired
    private Settings settings;

    @Autowired
    private Pki pki;

    @Autowired
    private FolderFactory folderFactory;

    @Autowired
    private RolesCollector rolesCollector;

    @Autowired
    private TaskScheduler taskScheduler;

    public boolean setupAlreadyDone() {
        try {
            SetupStatus status = settings.get(SETUP_STATUS_KEY, SetupStatus.class);
            return status != null;
        } catch (SettingsException ex) {
            logger.error(
                    "Cannot get settings %s: %s"
                            .formatted(SETUP_STATUS_KEY, ex.getMessage())
            );
        }

        return true;
    }

    public String setupArachne(SetupData setupData) throws SettingsException {
        logger.info("Performing setup: " + setupData);
        logger.info("Work directory: " + folderFactory.getArachneConfigDir());

        settings.put(SETUP_STATUS_KEY, SetupStatus.RUNNING);

        RoleRuleModel adminIsAdmin
                = new RoleRuleModel(
                        UsernameMatcher.class,
                        setupData.getAdminUsername(),
                        Role.ADMIN
                );
        roleRuleRepository.save(adminIsAdmin);

        UserModel adminUser
                = new UserModel(
                        setupData.getAdminUsername(),
                        setupData.getAdminPassword(),
                        "Arachne Administrator",
                        setupData.getAdminEmail()
                );
        adminUser.setRoles(
                rolesCollector.findRolesForUser(adminUser)
        );
        userRepository.save(adminUser);

        try {
            pki.fromSetupData(setupData);
        } catch (CertSpecsValidationException ex) {
            logger.error("Setup failed: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        }

        taskScheduler.runTask(UpdateDhParams.class, null, null);

        settings.put(SETUP_STATUS_KEY, SetupStatus.FINISHED);

        String msg;
        msg = "Setup completed.";
        logger.info(msg);

        return msg;
    }

    @PostMapping("/setup")
    @AnonymousAllowed
    @ApiMethodDescription(
            """
            Automized setup. Only possible if setup not already performed.
            """
    )
    public String onStupArachne(@RequestBody SetupData setupData)
            throws SettingsException {
        return setupArachne(setupData);
    }

    public void restore(InputStream is) {
        logger.info("Starting restore");
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode node = objectMapper.readTree(is);
            node.fields().forEachRemaining((Map.Entry<String, JsonNode> n) -> {
                String key = n.getKey();
                logger.info("Restoring " + key);
                try {
                    switch (key) {
                        case "roleRules" -> {
                            var reader = objectMapper.readerForListOf(RoleRuleModel.class);
                            roleRuleRepository.saveAll(
                                    reader.readValue(n.getValue())
                            );
                        }
                        case "settings" -> {
                            var reader = objectMapper.readerForListOf(SettingsModel.class);
                            settingsRepository.saveAll(
                                    reader.readValue(n.getValue())
                            );
                        }
                        case "recurringTasks" -> {
                            var reader = objectMapper.readerForListOf(RecurringTaskModel.class);
                            recurringTasksRepository.saveAll(
                                    reader.readValue(n.getValue())
                            );
                        }
                        case "tasks" -> {
                            var reader = objectMapper.readerForListOf(TaskModel.class);
                            taskRepository.saveAll(
                                    reader.readValue(n.getValue())
                            );
                        }
                        case "firewallRules" -> {
                            var reader = objectMapper.readerForListOf(FirewallRuleModel.class);
                            firewallRuleRepository.saveAll(
                                    reader.readValue(n.getValue())
                            );
                        }
                        case "users" -> {
                            var reader = objectMapper.readerForListOf(UserModel.class);
                            userRepository.saveAll(
                                    reader.readValue(n.getValue())
                            );
                        }
                        case "keys" -> {
                            var reader = objectMapper.readerForListOf(KeyModel.class);
                            keyRepository.saveAll(
                                    reader.readValue(n.getValue())
                            );
                        }
                        case "certificates" -> {
                            var reader = objectMapper.readerForListOf(CertificateModel.class);
                            certificateRepository.saveAll(
                                    reader.readValue(n.getValue())
                            );
                        }
                        case "sshKeys" -> {
                            var reader = objectMapper.readerFor(SshKeyEntity.class);
                            SshKeyRepository.saveAll(
                                    reader.readValue(n.getValue())
                            );
                        }
                        case "version" -> {
                            int version = n.getValue().asInt();
                            logger.info("We restore back version " + String.valueOf(version));
                        }
                        default ->
                            logger.info("Unhandled key: " + key);
                    }
                } catch (IOException ex) {
                    logger.error("Cannot read value: " + ex.getMessage());
                }
            });
        } catch (IOException ex) {
            logger.error("Error reading json: " + ex.getMessage());
        }
        logger.info("Restarting server");
        Arachne.restart();
        logger.info("Restore completed");
    }
}

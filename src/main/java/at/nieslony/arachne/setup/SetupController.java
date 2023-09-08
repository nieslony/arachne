/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.setup;

import at.nieslony.arachne.pki.CertSpecsValidationException;
import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.roles.Role;
import at.nieslony.arachne.roles.RoleRuleModel;
import at.nieslony.arachne.roles.RoleRuleRepository;
import at.nieslony.arachne.roles.RolesCollector;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.usermatcher.UsernameMatcher;
import at.nieslony.arachne.users.ArachneUser;
import at.nieslony.arachne.users.UserRepository;
import at.nieslony.arachne.utils.FolderFactory;
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
    private Settings settings;

    @Autowired
    private Pki pki;

    @Autowired
    private FolderFactory folderFactory;

    @Autowired
    private RolesCollector rolesCollector;

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

        ArachneUser adminUser
                = new ArachneUser(
                        setupData.getAdminUsername(),
                        setupData.getAdminPassword(),
                        "Arachne Administrator",
                        setupData.getAdminEmail()
                );
        adminUser.setRoles(
                rolesCollector.findRolesForUser(setupData.getAdminUsername())
        );
        userRepository.save(adminUser);

        try {
            pki.fromSetupData(setupData);
        } catch (CertSpecsValidationException ex) {
            logger.error("Setup failed: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        }

        settings.put(SETUP_STATUS_KEY, SetupStatus.FINISHED);

        String msg;
        msg = "Setup completed.";
        logger.info(msg);

        return msg;
    }

    @PostMapping("/setup")
    public String onStupArachne(@RequestBody SetupData setupData)
            throws SettingsException {
        return setupArachne(setupData);
    }
}

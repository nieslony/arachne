/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.setup;

import at.nieslony.arachne.utils.FolderFactory;
import at.nieslony.arachne.pki.Pki;
import at.nieslony.arachne.pki.PkiSetupException;
import at.nieslony.arachne.roles.Role;
import at.nieslony.arachne.roles.RoleRuleModel;
import at.nieslony.arachne.roles.RoleRuleRepository;
import at.nieslony.arachne.settings.SettingsModel;
import at.nieslony.arachne.settings.SettingsRepository;
import at.nieslony.arachne.users.ArachneUser;
import at.nieslony.arachne.users.UserRepository;
import at.nieslony.arachne.users.UsernameMatcher;
import java.util.Optional;
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
    public static final String SETUP_STATUS_FINISHED = "finished";
    public static final String SETUP_STATUS_RUNNING = "running";
    public static final String SETUP_STATUS_FAILED = "failed";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRuleRepository roleRuleRepository;

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private Pki pki;

    @Autowired
    private FolderFactory folderFactory;

    public boolean setupAlreadyDone() {
        Optional<SettingsModel> settingsModel
                = settingsRepository.findBySetting(SetupController.SETUP_STATUS_KEY);

        return settingsModel.isPresent();
    }

    public String setupArachne(SetupData setupData) {
        logger.info("Performing setup: " + setupData);
        logger.info("Work directory: " + folderFactory.getArachneConfigDir());

        SettingsModel setupStatus = new SettingsModel(
                SETUP_STATUS_KEY,
                SETUP_STATUS_RUNNING
        );
        setupStatus = settingsRepository.save(setupStatus);

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
        userRepository.save(adminUser);

        try {
            pki.fromSetupData(setupData);
        } catch (PkiSetupException ex) {
            logger.error("Setup failed: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        }

        setupStatus.setContent(SETUP_STATUS_FINISHED);
        settingsRepository.save(setupStatus);

        String msg;
        msg = "Setup completed.";
        logger.info(msg);

        return msg;
    }

    @PostMapping("/setup")
    public String onStupArachne(@RequestBody SetupData setupData) {
        return setupArachne(setupData);
    }
}

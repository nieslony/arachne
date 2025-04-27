/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.tasks.scheduled;

import at.nieslony.arachne.ldap.LdapController;
import at.nieslony.arachne.ldap.LdapSettings;
import at.nieslony.arachne.roles.RolesCollector;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.tasks.RecurringTaskDescription;
import at.nieslony.arachne.tasks.Task;
import at.nieslony.arachne.tasks.TaskDescription;
import at.nieslony.arachne.users.UserRepository;
import at.nieslony.arachne.utils.ArachneTimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;

/**
 *
 * @author claas
 */
@TaskDescription(name = "Refresh LDAP Users")
@RecurringTaskDescription(
        defaulnterval = 1,
        timeUnit = ArachneTimeUnit.DAY,
        startAt = "03:00:00"
)
@Slf4j
public class RefreshLdapUsers extends Task {

    @Override
    public String run(BeanFactory beanFactory) throws Exception {
        try {
            Settings settings = beanFactory.getBean(Settings.class);
            LdapSettings ldapSettings = settings.getSettings(LdapSettings.class);

            RolesCollector rolesCollestor = beanFactory.getBean(RolesCollector.class);

            UserRepository userRepository = beanFactory.getBean(UserRepository.class);
            int noUsersUpdated = 0;
            int noUsersAdded = 0;
            int noUsersSkipped = 0;
            for (var ldapUser : LdapController
                    .getInstance()
                    .findUsers(ldapSettings, "*", 1000)) {
                var repoUser = userRepository.findByUsername(ldapUser.getUsername());
                var roles = rolesCollestor.findRolesForUser(ldapUser);
                if (repoUser != null) {
                    repoUser.update(ldapUser);
                    repoUser.setRoles(roles);
                    userRepository.save(repoUser);
                    noUsersUpdated++;
                } else {
                    if (!roles.isEmpty()) {
                        noUsersAdded++;
                        ldapUser.setRoles(roles);
                        ldapUser.createRandomPassword();
                        userRepository.save(ldapUser);
                    } else {
                        noUsersSkipped++;
                    }
                }
            }
            String msg = "%d users updated, %d user added, %d users skipped"
                    .formatted(noUsersUpdated, noUsersAdded, noUsersSkipped);
            log.info(msg);
            return msg;
        } catch (BeansException ex) {
            log.error(ex.getMessage());
            return ex.getMessage();
        }
    }
}

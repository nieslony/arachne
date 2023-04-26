/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.roles;

import at.nieslony.arachne.usermatcher.UserMatcher;
import at.nieslony.arachne.usermatcher.UserMatcherCollector;
import at.nieslony.arachne.users.UserMatcherDescription;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
public class RolesCollector {

    @Autowired
    RoleRuleRepository roleRuleRepository;

    @Autowired
    UserMatcherCollector userMatcherCollector;

    public Set<SimpleGrantedAuthority> findAuthoritiesForUser(String username, boolean isInternal) {
        Set<SimpleGrantedAuthority> auths = new HashSet<>();

        for (RoleRuleModel rrm : roleRuleRepository.findAll()) {
            UserMatcher userMatcher = userMatcherCollector.buildUserMatcher(
                    rrm.getUserMatcherClassName(),
                    rrm.getParameter()
            );
            if (isInternal && userMatcher.getClass().isAnnotationPresent(UserMatcherDescription.class)) {
                UserMatcherDescription descr = userMatcher.getClass().getAnnotation(UserMatcherDescription.class);
                if (descr.ignoreInternalUser()) {
                    continue;
                }
            }
            if (userMatcher.isUserMatching(username)) {
                auths.add(new SimpleGrantedAuthority(rrm.getRole().name()));
            }
        }
        return auths;
    }

    public Set<String> findRolesForUser(String username, boolean isInternal) {
        Set<String> roles = new HashSet<>();

        for (RoleRuleModel rrm : roleRuleRepository.findAll()) {
            UserMatcher userMatcher = userMatcherCollector.buildUserMatcher(
                    rrm.getUserMatcherClassName(),
                    rrm.getParameter()
            );
            if (isInternal && userMatcher.getClass().isAnnotationPresent(UserMatcherDescription.class)) {
                UserMatcherDescription descr = userMatcher.getClass().getAnnotation(UserMatcherDescription.class);
                if (descr.ignoreInternalUser()) {
                    continue;
                }
            }
            if (userMatcher.isUserMatching(username)) {
                roles.add(rrm.getRole().name());
            }
        }
        return roles;
    }

    public Set<String> findRoleDescriptionsForUser(String username) {
        Set<String> roles = new HashSet<>();

        for (RoleRuleModel rrm : roleRuleRepository.findAll()) {
            UserMatcher userMatcher = userMatcherCollector.buildUserMatcher(
                    rrm.getUserMatcherClassName(),
                    rrm.getParameter()
            );
            if (userMatcher.isUserMatching(username)) {
                roles.add(rrm.getRole().toString());
            }
        }
        return roles;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.roles;

import at.nieslony.arachne.ldap.LdapGroupUserMatcher;
import at.nieslony.arachne.users.EverybodyMatcher;
import at.nieslony.arachne.users.UserMatcher;
import at.nieslony.arachne.users.UserMatcherDescription;
import at.nieslony.arachne.users.UserMatcherInfo;
import at.nieslony.arachne.users.UsernameMatcher;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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

    List<Class<? extends UserMatcher>> userMatcherClasses = new LinkedList<>();

    @Autowired
    RoleRuleRepository roleRuleRepository;

    public RolesCollector() {
        userMatcherClasses.add(UsernameMatcher.class);
        userMatcherClasses.add(EverybodyMatcher.class);
        userMatcherClasses.add(LdapGroupUserMatcher.class);
    }

    public List<UserMatcherInfo> getAllUserMatcherInfo() {
        List<UserMatcherInfo> umi = new LinkedList<>();
        for (Class<? extends UserMatcher> um : userMatcherClasses) {
            umi.add(new UserMatcherInfo(um));
        }
        return umi;
    }

    private UserMatcher buildUserMatcher(RoleRuleModel rrm) {
        try {
            Class cl = Class.forName(rrm.getUserMatcherClassName());
            UserMatcher userMatcher
                    = (UserMatcher) cl
                            .getConstructor(String.class)
                            .newInstance(rrm.getParameter());

            return userMatcher;
        } catch (Exception ex) {
            return null;
        }
    }

    public Set<SimpleGrantedAuthority> findAuthoritiesForUser(String username, boolean isInternal) {
        Set<SimpleGrantedAuthority> auths = new HashSet<>();

        for (RoleRuleModel rrm : roleRuleRepository.findAll()) {
            UserMatcher userMatcher = buildUserMatcher(rrm);
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
            UserMatcher userMatcher = buildUserMatcher(rrm);
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
            UserMatcher userMatcher = buildUserMatcher(rrm);
            if (userMatcher.isUserMatching(username)) {
                roles.add(rrm.getRole().toString());
            }
        }
        return roles;
    }
}

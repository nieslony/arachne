/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.usermatcher;

import java.util.LinkedList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
public class UserMatcherCollector {

    List<Class<? extends UserMatcher>> userMatcherClasses = new LinkedList<>();

    public UserMatcherCollector() {
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

    public UserMatcher buildUserMatcher(String userMatchClassName, String parameter) {
        try {
            Class cl = Class.forName(userMatchClassName);
            UserMatcher userMatcher
                    = (UserMatcher) cl
                            .getConstructor(String.class)
                            .newInstance(parameter);

            return userMatcher;
        } catch (Exception ex) {
            return null;
        }
    }
}

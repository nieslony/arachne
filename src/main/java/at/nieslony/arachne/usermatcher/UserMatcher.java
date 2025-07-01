/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package at.nieslony.arachne.usermatcher;

import at.nieslony.arachne.users.UserModel;

/**
 *
 * @author claas
 */
public abstract class UserMatcher {

    protected String parameter;

    public UserMatcher(String parameter) {
        this.parameter = parameter;
    }

    public abstract boolean isUserMatching(UserModel user);

    public static String getMatcherDetails(String className, String parameter) {
        try {
            var c = Class.forName(className).asSubclass(UserMatcher.class);
            return getMatcherDetails(c, parameter);
        } catch (ClassNotFoundException ex) {
            return "unknown class: " + className;
        }
    }

    public static String getMatcherDetails(Class<? extends UserMatcher> matcherClass, String parameter) {
        UserMatcherDescription desc = matcherClass.getAnnotation(UserMatcherDescription.class);
        if (desc != null) {
            if (desc.parameterLabel() != null && parameter != null) {
                return "%s %s".formatted(desc.description(), parameter);
            } else {
                return desc.description();
            }
        }

        return matcherClass.getName();
    }

    @Override
    public String toString() {
        return getMatcherDetails(getClass(), parameter);
    }
}

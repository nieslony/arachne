/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package at.nieslony.arachne.usermatcher;

/**
 *
 * @author claas
 */
public abstract class UserMatcher {

    protected String parameter;

    public UserMatcher(String parameter) {
        this.parameter = parameter;
    }

    public abstract boolean isUserMatching(String username);

    public static String getMatcherDetails(String className, String parameter) {
        Class c;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            return "unknown class: " + className;
        }

        return getMatcherDetails(c, parameter);
    }

    public static String getMatcherDetails(Class matcherClass, String parameter) {
        UserMatcherDescription desc
                = (UserMatcherDescription) matcherClass.getAnnotation(
                        UserMatcherDescription.class
                );
        if (desc != null) {
            if (desc.description() != null) {
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

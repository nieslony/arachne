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
        UserMatcherInfo info
                = (UserMatcherInfo) matcherClass.getAnnotation(UserMatcherInfo.class);
        if (info != null) {
            if (info.getParameterLabel() != null) {
                return "%s %s".formatted(info.getDescription(), parameter);
            } else {
                return info.getDescription();
            }
        }

        return "???";
    }

    @Override
    public String toString() {
        return getMatcherDetails(getClass(), parameter);
    }
}

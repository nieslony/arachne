/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.usermatcher;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author claas
 */
@Getter
@Setter
@EqualsAndHashCode
public class UserMatcherInfo {

    private String className;
    private String description;
    private String parameterLabel;

    public UserMatcherInfo() {
        className = null;
        description = null;
        parameterLabel = null;
    }

    public UserMatcherInfo(Class< ? extends UserMatcher> userMatcherClass) {
        init(userMatcherClass);
    }

    public UserMatcherInfo(String userMatcherClassName) {
        if (userMatcherClassName != null) {
            try {
                Class userMatcherClass = Class.forName(userMatcherClassName);
                init(userMatcherClass);
            } catch (ClassNotFoundException ex) {

            }
        }
    }

    private void init(Class< ? extends UserMatcher> userMatcherClass) {
        if (userMatcherClass == null) {
            return;
        }

        this.className = userMatcherClass.getName();
        UserMatcherDescription umd
                = userMatcherClass.getAnnotation(
                        UserMatcherDescription.class);
        if (umd != null) {
            this.description = umd.description();
            this.parameterLabel = umd.parameterLabel();
        } else {
            this.description = userMatcherClass.getSimpleName();
            this.parameterLabel = "Parameter";
        }
    }

    @Override
    public String toString() {
        return description;
    }
}

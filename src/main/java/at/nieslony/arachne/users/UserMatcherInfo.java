/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

import lombok.Data;

/**
 *
 * @author claas
 */
@Data
public class UserMatcherInfo {

    private String className;
    private String description;
    private String parameterLabel;

    public UserMatcherInfo() {

    }

    public UserMatcherInfo(Class< ? extends UserMatcher> userMatcherClass) {
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

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
}

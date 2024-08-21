/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.usermatcher;

import at.nieslony.arachne.users.UserModel;

/**
 *
 * @author claas
 */
@UserMatcherDescription(description = "Username is", parameterLabel = "Username")
public class UsernameMatcher extends UserMatcher {

    public UsernameMatcher(String requiredUsername) {
        super(requiredUsername);
    }

    @Override
    public boolean isUserMatching(UserModel user) {
        return parameter.equals(user.getUsername());
    }
}

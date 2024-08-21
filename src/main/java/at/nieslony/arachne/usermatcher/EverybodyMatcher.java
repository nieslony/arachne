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
@UserMatcherDescription(description = "Everybody")
public class EverybodyMatcher extends UserMatcher {

    public EverybodyMatcher(String requiredUsername) {
        super(requiredUsername);
    }

    @Override
    public boolean isUserMatching(UserModel user) {
        return true;
    }
}

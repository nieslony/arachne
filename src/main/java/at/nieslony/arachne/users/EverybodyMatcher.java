/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

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
    public boolean isUserMatching(String username) {
        return true;
    }
}

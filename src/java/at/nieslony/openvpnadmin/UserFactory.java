/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import java.util.List;

/**
 *
 * @author claas
 */
abstract public class UserFactory {
    abstract public AbstractUser findUser(String username);
    abstract public AbstractUser addUser(String username);
    abstract public boolean removeUser(String username)
            throws Exception;
    abstract public List<AbstractUser> getAllUsers()
            throws Exception;
}

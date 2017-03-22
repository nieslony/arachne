/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

/**
 *
 * @author claas
 */
abstract public class UserFactory {
    abstract public User findUser(String username);
    abstract public User addUser(String username);
}

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
abstract public class User {
    private String username;
    private String givenName;
    private String surName;
    private String fullName;
    private String email;

    public String getUsername() {
        return username;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getSurName() {
        return surName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setUsername(String un) {
        username = un;
    }

    public void setGivenName(String gn) {
        givenName = gn;
    }

    public void setFullName(String fn) {
        fullName = fn;
    }

    public void setEmail(String em) {
        email = em;
    }

    public void setSurName(String sn) {
        surName = sn;
    }

    public void setPassword(String pwd) {
    }

    abstract public boolean isImmutable();
    abstract public boolean auth(String password);

    public void save()
            throws Exception
    {
    }

}

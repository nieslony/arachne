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
abstract public class AbstractUser {
    private String username;
    private String givenName;
    private String surName;
    private String fullName;
    private String email;

    public String getUsername() {
        return username;
    }

    public String getGivenName() {
        return givenName == null ? "" : givenName;
    }

    public String getSurName() {
        return surName == null ? "" : surName;
    }

    public String getFullName() {
        return fullName == null ? "" : fullName;
    }

    public String getEmail() {
        return email == null ? "" : email;
    }

    public void setUsername(String un) {
        username = un == null ? "" : un;
    }

    public void setGivenName(String gn) {
        givenName = gn == null ? "" : gn;
    }

    public void setFullName(String fn) {
        fullName = fn == null ? "" : fn;
    }

    public void setEmail(String em) {
        email = em == null ? "" : em;
    }

    public void setSurName(String sn) {
        surName = sn == null ? "" : sn;
    }

    public void setPassword(String pwd) {
    }

    abstract public boolean isImmutable();
    abstract public boolean auth(String password);
    abstract public String getUserTypeStr();

    public void save()
            throws Exception
    {
    }

}

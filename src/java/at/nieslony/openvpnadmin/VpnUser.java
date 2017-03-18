/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import java.io.Serializable;

/**
 *
 * @author claas
 */
public class VpnUser implements Serializable {
    public VpnUser(String username) {
        this.username = username;
    }

    public enum UserType {
        UT_UNASSIGNED,
        UT_LOCAL,
        UT_LDAP;
    }

    private String username;
    private String fullName;
    private String givenName;
    private String surname;
    private UserType userType = UserType.UT_UNASSIGNED;
    private String passwordHash;
    private String dn;
    private String email;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserType getUserType() {
        return userType;
    }

    public String getUserTypeStr() {
        if (userType == null)
            return "unknown";

        switch (userType) {
            case UT_LOCAL:
                return "Local";
            case UT_LDAP:
                return "LDAP";
            case UT_UNASSIGNED:
                return "unassigned";
        }
        return "unknown";
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLongName() {
        StringBuilder sb = new StringBuilder();

        sb.append(username);

        if (fullName != null && fullName.isEmpty())
            sb.append(" (").append(fullName).append(")");
        else if (givenName != null && givenName.isEmpty())
            sb.append(" (").append(givenName).append(")");

        return sb.toString();
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}

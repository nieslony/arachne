/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import java.util.logging.Logger;

/**
 *
 * @author claas
 */
public class LdapUser
        extends AbstractUser
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private LdapHelperUser ldapHelperUser;
    private String dn;

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getDn() {
        return dn;
    }

    public LdapUser(LdapHelperUser ls, String username) {
        ldapHelperUser = ls;
        setUsername(username);
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    @Override
    public boolean auth(String password) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getUserTypeStr() {
        return "LDAP";
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.LdapSettingsBase;
import at.nieslony.openvpnadmin.VpnUser;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapUser;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedProperty;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.primefaces.context.RequestContext;

/**
 *
 * @author claas
 */
@ManagedBean
@ApplicationScoped
public class LdapSettings
    extends LdapSettingsBase
    implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{folderFactory}")
    private FolderFactory folderFactory;

    public void setFolderFactory(FolderFactory ff) {
        folderFactory = ff;
    }

    /**
     * Creates a new instance of LdapSetup
     */
    public LdapSettings() {
    }

    @PostConstruct
    public void init()  {
        String fn = folderFactory.getConfigDir() + "/ldap.properties";
        logger.info(String.format("Loading settings from %s", fn));
        try {
            FileInputStream fis = new FileInputStream(fn);
            getProps().load(fis);
            fis.close();
        }
        catch(IOException ex) {
            logger.severe(String.format("Error reading ldap.properties: %s",
                    ex.getMessage()));
        }
    }

    public void save() {
        FileOutputStream fos = null;
        try {
            logger.info("Saving LDAP settings");
            fos = new FileOutputStream(folderFactory.getConfigDir() + "/ldap.properties");
            getProps().store(fos, "");
            fos.close();

            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Info",
                    "LDAP settings successfuly saved."
            );
            RequestContext.getCurrentInstance().showMessageInDialog(msg);
        } catch (IOException ex) {
            Logger.getLogger(LdapSettings.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException ex) {
                Logger.getLogger(LdapSettings.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public List<VpnUser> findVpnUsers(String pattern) {
        DirContext ctx;
        NamingEnumeration results;
        LinkedList<VpnUser> users = new LinkedList<>();

        try {
            ctx = getLdapContext();

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            results = ctx.search(getOuUsers(), getUserSearchString(pattern), sc);
            while (results.hasMore()) {
                SearchResult result = (SearchResult) results.next();
                Attributes attrs = result.getAttributes();
                Attribute attr;
                VpnUser vpnUser;

                attr = attrs.get(getAttrUsername());
                if (attr != null) {
                    vpnUser = new VpnUser((String) attr.get());
                }
                else {
                    logger.warning("Ignoring user with no username");
                    continue;
                }
                attr = attrs.get(getAttrFullName());
                if (attr != null)
                    vpnUser.setFullName((String) attr.get());

                attr = attrs.get(getAttrGivenName());
                if (attr != null)
                    vpnUser.setGivenName((String) attr.get());

                attr = attrs.get(getAttrSurname());
                if (attr != null)
                    vpnUser.setSurname((String) attr.get());

                vpnUser.setUserType(VpnUser.UserType.UT_LDAP);

                vpnUser.setDn(result.getName() + "," + getBaseDn());

                users.add(vpnUser);
            }
        }
        catch (NamingException ex) {
            logger.severe(String.format("Error finding VPN users in LDAP: %s",
                    ex.getMessage()));
        }

        return users;
    }

    public VpnUser findVpnUser(String username)
            throws NoSuchLdapUser, NamingException
    {
        logger.info(String.format("Trying to find user %s in LDAP", username));

        DirContext ctx;
        NamingEnumeration results;
        VpnUser vpnUser = null;

        ctx = getLdapContext();

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        results = ctx.search(getOuUsers(), getUserSearchString(username), sc);
        if (results.hasMore()) {
            vpnUser = new VpnUser(username);
            SearchResult result = (SearchResult) results.next();
            Attributes attrs = result.getAttributes();
            Attribute attr;
            attr = attrs.get(getAttrFullName());
            if (attr != null)
                vpnUser.setFullName((String) attr.get());

            attr = attrs.get(getAttrGivenName());
            if (attr != null)
                vpnUser.setGivenName((String) attr.get());

            attr = attrs.get(getAttrSurname());
            if (attr != null)
                vpnUser.setSurname((String) attr.get());

            vpnUser.setDn(result.getName() + "," + getBaseDn());

            vpnUser.setUserType(VpnUser.UserType.UT_LDAP);
        }
        else {
            throw new NoSuchLdapUser(String.format("LDAP user %s not found", username));
        }

        return vpnUser;
    }
}

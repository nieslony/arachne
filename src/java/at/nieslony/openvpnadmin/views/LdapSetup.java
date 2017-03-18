/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.LdapGroup;
import at.nieslony.openvpnadmin.LdapSettingsBase;
import at.nieslony.openvpnadmin.beans.LdapSettings;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapGroup;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Properties;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
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
@ViewScoped
public class LdapSetup
    extends LdapSettingsBase
    implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{ldapSettings}")
    LdapSettings ldapSettings;

    public void setLdapSettings(LdapSettings ls) {
        ldapSettings = ls;
    }

    /**
     * Creates a new instance of LdapSetup
     */
    public LdapSetup() {
    }

    @PostConstruct
    public void init() {
        onResetToSaved();
    }

    public void onSave() {
        ldapSettings.setProps((Properties) getProps().clone());
        ldapSettings.save();
    }

    public void onResetToSaved() {
        setProps((Properties) ldapSettings.getProps().clone());
    }

    public void onResetToDefaults() {
        Properties props = new Properties();
        setProps(props);
    }

    private String testUser;
    private String testFullName;
    private String testGivenName;
    private String testSurname;
    private String testGroupDesciption;
    private String testGroupMembers;
    private String testGroup;

    public String getTestGroup() {
        return testGroup;
    }

    public String getTestGroupMembers() {
        return testGroupMembers;
    }

    public void setTestGroupMembers(String gm) {
        testGroupMembers = gm;
    }

    public String getTestGroupDescription() {
        return testGroupDesciption;
    }

    public void setTestGroupDescription(String gd) {
        testGroupDesciption = gd;
    }

    public void setTestGroup(String tg) {
        testGroup = tg;
    }

    public String getTestSurname() {
        return testSurname;
    }

    public void setTestSurname(String testSurname) {
        this.testSurname = testSurname;
    }

    public String getTestGivenName() {
        return testGivenName;
    }

    public void setTestGivenName(String testGivenName) {
        this.testGivenName = testGivenName;
    }

    public String getTestFullName() {
        return testFullName;
    }

    public void setTestFullName(String testFullName) {
        this.testFullName = testFullName;
    }

    public String getTestUser() {
        return testUser;
    }

    public void setTestUser(String tu) {
        testUser = tu;
    }

    public void testConnectionGroup() {
        if (testGroup == null || testGroup.isEmpty()) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Please enter groupname to search for.");
            RequestContext.getCurrentInstance().showMessageInDialog(msg);
            return;
        }

        LdapGroup ldapGroup;
        try {
            ldapGroup = findLdapGroup(testGroup);
            RequestContext rctx = RequestContext.getCurrentInstance();

            if (!ldapGroup.getMemberDNs().isEmpty()) {
                testGroupMembers = String.join("<br/>", ldapGroup.getMemberDNs());
                logger.info(String.format("Found group members: %s", testGroupMembers));
            }
            else if (!ldapGroup.getMemberUids().isEmpty()) {
                testGroupMembers = String.join("<br/>", ldapGroup.getMemberUids());
                logger.info(String.format("Found group members: %s", testGroupMembers));
            }
            else {
                testGroupMembers = "no members found" ;
            }

            rctx.update("resultGroupDescription");
            rctx.update("resultGroupname");
            rctx.update("resultGroupMembers");
            rctx.execute("PF('testLdapGroup').show();");
        }
        catch (NoSuchLdapGroup nslg) {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Error",
                        "LDAP connection seems to work, but no result found.");
                RequestContext.getCurrentInstance().showMessageInDialog(msg);
        }
        catch (NamingException ne) {
            logger.warning(String.format("Error testing LDAP connection: %s",
                    ne.getMessage()));

            StringWriter wr = new StringWriter();
            wr.append("<h2>Error connecting to LDAP server</h2>");
            if (ne.getExplanation() != null) {
                wr.append("<p><strong>Explanation:</strong> ");
                wr.append(ne.getExplanation()).append("</p>");
            }
            if (ne.getCause() != null) {
                wr.append("<p><strong>Cause:</strong> ");
                wr.append(ne.getCause().toString());
                wr.append("</p>");
            }

            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", wr.toString());
            RequestContext.getCurrentInstance().showMessageInDialog(msg);
        }
    }

    public void testConnectionUser() {
        if (testUser == null || testUser.isEmpty()) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Please enter username to search for.");
            RequestContext.getCurrentInstance().showMessageInDialog(msg);
            return;
        }

        DirContext ctx;
        NamingEnumeration results;

        try {
            ctx = getLdapContext();
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            results = ctx.search(getOuUsers(), getUserSearchString(testUser), sc);

            if (results.hasMore()) {
                SearchResult result = (SearchResult) results.next();
                Attributes attrs = result.getAttributes();
                Attribute attr;
                attr = attrs.get(getAttrFullName());
                setTestFullName( attr != null ? (String) attr.get() : "");
                attr = attrs.get(getAttrGivenName());
                setTestGivenName(attr != null ? (String) attr.get() : "");
                attr = attrs.get(getAttrSurname());
                setTestSurname(attr != null ? (String) attr.get() : "");

                RequestContext rctx = RequestContext.getCurrentInstance();
                rctx.update("resultUserame");
                rctx.update("resultFullName");
                rctx.update("resultGivenName");
                rctx.update("resultSurame");
                rctx.execute("PF('testLdapUser').show();");

            }
            else {
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Error",
                        "LDAP connection seems to work, but no result found.");
                RequestContext.getCurrentInstance().showMessageInDialog(msg);
            }
        }
        catch (NamingException ex) {
            logger.warning(String.format("Error testing LDAP connection: %s",
                    ex.getMessage()));

            StringWriter wr = new StringWriter();
            wr.append("<h2>Error connecting to LDAP server</h2>");
            if (ex.getExplanation() != null) {
                wr.append("<p><strong>Explanation:</strong> ");
                wr.append(ex.getExplanation()).append("</p>");
            }
            if (ex.getCause() != null) {
                wr.append("<p><strong>Cause:</strong> ");
                wr.append(ex.getCause().toString());
                wr.append("</p>");
            }

            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", wr.toString());
            RequestContext.getCurrentInstance().showMessageInDialog(msg);
        }
    }

    public String getMemberAttrType_MEMBER_DN() {
        return MemberAttrType.MAT_MEMBER_DN.name();
    }

    public String getMemberAttrType_MEMBER_UID() {
        return MemberAttrType.MAT_MEMBER_UID.name();
    }

    public String getMemberAttrTypeStr() {
        return getMemberAttrType().name();
    }

    public void setMemberAttrTypeStr(String mat) {
        setMemberAttrType(MemberAttrType.valueOf(mat));
    }
}

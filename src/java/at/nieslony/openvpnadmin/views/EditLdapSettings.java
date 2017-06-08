
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.LdapGroup;
import at.nieslony.openvpnadmin.LdapHelper;
import at.nieslony.openvpnadmin.LdapHelperUser;
import at.nieslony.openvpnadmin.beans.LdapSettings;
import at.nieslony.openvpnadmin.beans.base.LdapSettingsBase;
import at.nieslony.openvpnadmin.exceptions.NoSuchLdapGroup;
import at.nieslony.openvpnadmin.views.base.EditLdapSettingsBase;
import at.nieslony.utils.NetUtils;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.primefaces.context.RequestContext;

@ManagedBean
@ViewScoped
public class EditLdapSettings
    extends EditLdapSettingsBase
    implements Serializable, LdapHelperUser
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private LdapHelper ldapHelper;

    public EditLdapSettings () {
        ldapHelper = new LdapHelper(this);
    }

    @ManagedProperty(value = "#{ldapSettings}")
    LdapSettings ldapSettings;

    @PostConstruct
    public void init() {
        setBackend(ldapSettings);
        load();
    }

    public void onSave() {
        save();
        FacesContext.getCurrentInstance().addMessage(
                null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO, "Info", "Settings saved."));
    }

    public void onReset() {
        load();
    }

    public void onResetToDefaults() {
        resetDefaults();
    }

    public void setLdapSettings(LdapSettings v) {
        ldapSettings = v;
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

    public void onTestConnectionGroup() {
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
            testGroupDesciption = ldapGroup.getDescription();

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

    public void onTestConnectionUser() {
        if (testUser == null || testUser.isEmpty()) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Please enter username to search for.");
            RequestContext.getCurrentInstance().showMessageInDialog(msg);
            return;
        }

        DirContext ctx;
        NamingEnumeration results;

        try {
            ctx = ldapHelper.getLdapContext();
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String searchString = ldapHelper.getUserSearchString(testUser);
            results = ctx.search(getOuUsers(), searchString, sc);
            logger.info(String.format("Search for user %s. Filter: %s",
                    testUser, searchString));

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

    public LdapGroup findLdapGroup(String gn)
            throws NamingException, NoSuchLdapGroup
    {
        return ldapHelper.findLdapGroup(gn);
    }

    public String getMemberAttrType_MEMBER_DN() {
        return LdapSettingsBase.MemberAttrType.MAT_MEMBER_DN.name();
    }

    public String getMemberAttrType_MEMBER_UID() {
        return LdapSettingsBase.MemberAttrType.MAT_MEMBER_UID.name();
    }

    public String getMemberAttrTypeStr() {
        return getMemberAttrType().name();
    }

    public void setMemberAttrTypeStr(String mat) {
        setMemberAttrType(LdapSettingsBase.MemberAttrType.MAT_MEMBER_DN.valueOf(mat));
    }

    @Override
    public void setSecurityCredentials(String sc) {
        if (sc != null && !sc.isEmpty())
            super.setSecurityCredentials(sc);
    }

    @Override
    public String getProviderUrl() {
        String pu = super.getProviderUrl();

        if (pu == null || pu.isEmpty())
            return getDefaultProviderUrl();
        else
            return pu;
    }

    public String getDefaultProviderUrl() {
        String myDomain = NetUtils.myDomain();
        String defaultHost = null;
        logger.info(String.format("DNS lookupup"
                + " for my domain %s", myDomain));
        try {
            logger.info("DNS lookup LDAP srv record");
            defaultHost = NetUtils.srvLookup("ldap");
            logger.info(String.format("Found: %s", defaultHost));
        }
        catch (NamingException ex) {
            logger.info(String.format("No entry founnd: %s", ex.getMessage()));
        }
        if (defaultHost == null) {
            defaultHost = "ldap." + myDomain;
            try {
                logger.info(String.format("DNS lookup %s", defaultHost));
                InetAddress addr = InetAddress.getByName(defaultHost);
                logger.info(String.format("Found: %s", addr.getHostAddress()));
            }
            catch (Exception e) {
                logger.info(String.format("No entry foind: %s", e.getMessage()));
                defaultHost = "ldap.example.com";
            }
        }

        String myDomSplit[] = myDomain.split("\\.");
        for (int i = 0; i < myDomSplit.length; i++) {
            myDomSplit[i] = "dc=" + myDomSplit[i];
        }
        String defaultDn = String.join(",", myDomSplit);

        return String.format("ldap://%s/%s", defaultHost, defaultDn);
    }

    public String getDefaultUserSearchFilter() {
        return ldapHelper.getDefaultUserSearchString("%u");
    }

    public String getDefaultGroupSearchFilter() {
        return ldapHelper.getDefaultGroupSearchString("%g");
    }

    public void onDefaultUserSearchFilter() {
        if (getCustomUserSearchFilter().isEmpty()) {
            setCustomUserSearchFilter(ldapHelper.getUserSearchString("%u"));
        }
    }

    public void onDefaultGroupSearchFilter() {
        if (getCustomGroupSearchFilter().isEmpty()) {
            setCustomGroupSearchFilter(ldapHelper.getGroupSearchString("%g"));
        }
    }

    public void onLoadDefaultsforActiveDirectory() {
        setObjectClassUser("person");
        setAttrUsername("samAccountName");
        setAttrFullName("cn");
        setAttrGivenName("givenName");
        setAttrSurname("sn");
        setOuUsers("cn=persons");

        setObjectClassGroup("group");
        setAttrGroupName("cn");
        setOuGroups("cn=groups");
        setAttrGroupDescription("description");
        setMemberAttrType(LdapSettingsBase.MemberAttrType.MAT_MEMBER_DN);
        setAttrGroupMemberDn("member");

        setUseCustomUserSearchFilter(Boolean.FALSE);
    }

    public void onLoadDefaultsForFreeIPA() {
        setObjectClassUser("person");
        setAttrUsername("uid");
        setAttrFullName("cn");
        setAttrGivenName("givenName");
        setAttrSurname("sn");
        setOuUsers("cn=users,cn=accounts");

        setObjectClassGroup("ipausergroup");
        setAttrGroupName("cn");
        setOuGroups("cn=groups,cn=accounts");
        setAttrGroupDescription("description");
        setMemberAttrType(LdapSettingsBase.MemberAttrType.MAT_MEMBER_DN);
        setAttrGroupMemberDn("member");

        setUseCustomUserSearchFilter(Boolean.FALSE);
    }

    public void onLoadDefaultsForRfc2307bis() {
        setObjectClassUser("posixAccount");
        setAttrUsername("uid");
        setAttrFullName("gecos");
        setAttrGivenName("");
        setAttrSurname("");
        setOuUsers("");

        setObjectClassGroup("posixGroup");
        setAttrGroupName("gid");
        setOuGroups("");
        setAttrGroupDescription("");
        setMemberAttrType(LdapSettingsBase.MemberAttrType.MAT_MEMBER_UID);
        setAttrGroupMemberDn("memberUid");

        setUseCustomUserSearchFilter(Boolean.FALSE);
    }
}

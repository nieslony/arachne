/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

/**
 *
 * @author claas
 */
@ManagedBean
@ApplicationScoped
public class ClientCertificateSettings implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{folderFactory}")
    private FolderFactory folderFactory;

    public void setFolderFactory(FolderFactory ff) {
        folderFactory = ff;
    }

    private static final String PROP_TITLE = "title";
    private static final String PROP_ORGANIZATION = "organization";
    private static final String PROP_ORGANIZATIONAL_UNIT = "organizationalUnit";
    private static final String PROP_CITY = "city";
    private static final String PROP_STATE = "state";
    private static final String PROP_COUNTRY = "country";
    private static final String PROP_VALID_TIME = "validTime";
    private static final String PROP_VALID_TIME_UNIT = "validTimeUnit";
    private static final String PROP_SIGN_ALGO = "signatureAlgorithm";
    private static final String PROP_KEY_SIZE = "keySize";

    private static final String PROPS_FILE = "clientcertsettings.properties";

    /**
     * Creates a new instance of ClientCertificateSettings
     */
    public ClientCertificateSettings() {
    }

    @PostConstruct
    public void init() {
        String fn = folderFactory.getConfigDir() + "/" + PROPS_FILE;
        logger.info(String.format("Loading settings from %s", fn));
        try {
            FileInputStream fis = new FileInputStream(fn);
            props.load(fis);
            fis.close();
        }
        catch(IOException ex) {
            logger.severe(String.format("Error reading clientcertsettings.properties: %s",
                    ex.getMessage()));
        }
    }

    Properties props = new Properties();

    public void save() {
        FileOutputStream fos = null;
        try {
            logger.info("Saving client cert settings");
            fos = new FileOutputStream(folderFactory.getConfigDir() + "/" + PROPS_FILE);
            props.store(fos, "");
            fos.close();
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

    public String getTitle() {
        return props.getProperty(PROP_TITLE, "UserCert_%u");
    }

    public void setTitle(String t) {
        props.setProperty(PROP_TITLE, t);
    }

    public String getOrganization() {
        return props.getProperty(PROP_ORGANIZATION, "");
    }

    public void setOrganization(String org) {
         props.setProperty(PROP_ORGANIZATION, org);
    }

    public String getOPrganizationalUnit() {
        return props.getProperty(PROP_ORGANIZATIONAL_UNIT, "");
    }

    public void setOrganizationalUnit(String ou) {
         props.setProperty(PROP_ORGANIZATIONAL_UNIT, ou);
    }

    public String getCity() {
        return props.getProperty(PROP_CITY, "");
    }

    public void setCity(String c) {
        props.setProperty(PROP_CITY, c);
    }

    public String getState() {
        return props.getProperty(PROP_STATE, "");
    }

    public void setState(String s) {
        props.setProperty(PROP_STATE, s);
    }

    public String getCountry() {
        return props.getProperty(PROP_COUNTRY, "");
    }

    public void setCountry(String c) {
        props.setProperty(PROP_COUNTRY, c);
    }

    public int getValidTime() {
        int i;

        try {
            i = Integer.parseInt(props.getProperty(PROP_VALID_TIME, "365"));
        }
        catch (NumberFormatException ex) {
            logger.log(Level.WARNING, "Cannot parse integer: {0}", props.getProperty(PROP_VALID_TIME));
            i = 365;
        }

        return i;
    }

    public void setValidTime(int t) {
        props.setProperty(PROP_VALID_TIME, String.valueOf(t));
    }

    public String getSignatureAlgorith() {
        return props.getProperty(PROP_SIGN_ALGO, "SHA512withRSA");
    }

    public void setSignatureAlgorith(String sa) {
        props.setProperty(PROP_SIGN_ALGO, sa);
    }

    public String getKeyAlgorithm() {
        String sa = getSignatureAlgorith();
        String[] saSplit = sa.split("with");
        if (saSplit.length != 2) {
            return "???";
        }
        else {
            return saSplit[1];
        }
    }

    public int getKeySize() {
        int ks;

        try {
            ks = Integer.parseInt(props.getProperty(PROP_KEY_SIZE, "2048"));
        }
        catch (NumberFormatException ex) {
            logger.log(Level.WARNING, "Cannot parse integer: {0}", props.getProperty(PROP_KEY_SIZE));
            ks = 2048;
        }

        return ks;
    }

    public void setKeySize(int ks) {
        props.setProperty(PROP_KEY_SIZE, String.valueOf(ks));
    }

    public String getValidTimeUnit() {
        return props.getProperty(PROP_VALID_TIME_UNIT, "days");
    }

    public void setValidTimeUnit(String vtu) {
        props.setProperty(PROP_VALID_TIME_UNIT, vtu);
    }
}

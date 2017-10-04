
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.TimeUnit;
import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.beans.ServerCertificateSettings;
import at.nieslony.openvpnadmin.views.base.EditServerCertificateSettingsBase;
import at.nieslony.utils.pki.CertificateAuthority;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

@ManagedBean
@ViewScoped
public class EditServerCertificateSettings
    extends EditServerCertificateSettingsBase
    implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());
    private List<SelectItem> signatureAlgorithms;

    @ManagedProperty(value = "#{pki}")
    private Pki pki;

    public void setPki(Pki pki) {
        this.pki = pki;
    }

    public EditServerCertificateSettings () {
    }

    @ManagedProperty(value = "#{serverCertificateSettings}")
    ServerCertificateSettings serverCertificateSettings;

    @PostConstruct
    public void init() {
        signatureAlgorithms = new ArrayList<>();
        for (CertificateAuthority.KeySignAlgo ksa : CertificateAuthority.getKeySignAlgos()) {
            String keyAlgo = ksa.keyAlgo;
            SelectItemGroup group = new SelectItemGroup(keyAlgo);
            SelectItem[] items = new SelectItem[ksa.signatureAlgos.length];
            for (int i = 0; i < ksa.signatureAlgos.length; i++) {
                String label = ksa.signatureAlgos[i] + " with " + keyAlgo;
                String value = ksa.signatureAlgos[i] + "with" + keyAlgo;
                items[i] = new SelectItem(value, label);
            }
            group.setSelectItems(items);
            signatureAlgorithms.add(group);
        }

        setBackend(serverCertificateSettings);
        load();

        if (!getValuesAlreadySet()) {
            //serverCertificateSettings.closeServerCertificateSettings(this);
        }
    }

    public void onSave() {
        setValuesAlreadySet(true);
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

    public List<SelectItem> getSignatureAlgorithms() {
        return signatureAlgorithms;
    }

    public void setServerCertificateSettings(ServerCertificateSettings v) {
        serverCertificateSettings = v;
    }

    public String getKeyAlgo() {
        return getSignatureAlgorithm().split("with")[1];
    }

    public int[] getKeySizes() {
        for (CertificateAuthority.KeySignAlgo ksa : CertificateAuthority.getKeySignAlgos()) {
            if (ksa.keyAlgo.equals(getKeyAlgo()))
                return ksa.keySizes;
        }

        return new int[0];
    }

    public TimeUnit[] getValidTimeUnits() {
        return TimeUnit.values();
    }
}

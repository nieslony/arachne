
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.TimeUnit;
import at.nieslony.openvpnadmin.beans.Pki;
import at.nieslony.openvpnadmin.beans.ServerCertificateSettings;
import at.nieslony.openvpnadmin.views.base.EditServerCertificateSettingsBase;
import at.nieslony.utils.pki.CertificateAuthority;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

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
        setBackend(serverCertificateSettings);
        load();
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

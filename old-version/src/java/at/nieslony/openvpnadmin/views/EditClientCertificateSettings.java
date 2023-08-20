
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.beans.ClientCertificateSettings;
import at.nieslony.openvpnadmin.views.base.EditClientCertificateSettingsBase;
import at.nieslony.utils.pki.CertificateAuthority;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
public class EditClientCertificateSettings
    extends EditClientCertificateSettingsBase
    implements Serializable
{
    private List<SelectItem> signatureAlgorithms;

    public EditClientCertificateSettings () {
    }

    @ManagedProperty(value = "#{clientCertificateSettings}")
    ClientCertificateSettings clientCertificateSettings;

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

        setBackend(clientCertificateSettings);
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

    public void setClientCertificateSettings(ClientCertificateSettings v) {
        clientCertificateSettings = v;
    }

    public List<SelectItem> getSignatureAlgorithms() {
        return signatureAlgorithms;
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

    public String[] getValidTimeUnits() {
        return new String[] {
            "days",
            "months",
            "years"
        };
    }
}

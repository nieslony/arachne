/*
 * Copyright (C) 2018 Claas Nieslony
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    /**
	 * 
	 */
	private static final long serialVersionUID = 7935300203892042222L;
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
        String[] split = getSignatureAlgorithm().split("WITH");

        if (split.length == 2)
            return split[1];
        else
            return "";
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

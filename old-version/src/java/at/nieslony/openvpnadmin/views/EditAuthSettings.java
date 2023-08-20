
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.views.base.EditAuthSettingsBase;
import at.nieslony.openvpnadmin.beans.AuthSettings;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.bean.ViewScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

@ManagedBean
@ViewScoped
public class EditAuthSettings 
    extends EditAuthSettingsBase
    implements Serializable
{
    public EditAuthSettings () {
    }

    @ManagedProperty(value = "#{authSettings}")
    AuthSettings authSettings;

    @PostConstruct
    public void init() {
        setBackend(authSettings);
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
    
    public void setAuthSettings(AuthSettings v) {
        authSettings = v;
    }
}

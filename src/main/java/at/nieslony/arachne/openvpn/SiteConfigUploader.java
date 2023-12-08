/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import static at.nieslony.arachne.openvpn.OpenVpnSiteView.SshAuthType.PRESHARED_KEY;
import static at.nieslony.arachne.openvpn.OpenVpnSiteView.SshAuthType.USERNAME_PASSWORD;
import com.jcraft.jsch.JSch;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author claas
 */
public class SiteConfigUploader {

    @Getter
    @Setter
    private class SiteUploadSettings {

        private String username;
        private String password;
        private boolean requireSudo;
        private boolean restartOpenVpn;
        private boolean enableOpenVpn;
    }

    private Dialog dlg;
    private Binder<SiteUploadSettings> binder;

    public SiteConfigUploader() {
        dlg = createUploadDialog();
    }

    public void openDialog(VpnSite site) {
        dlg.setHeaderTitle("Upload Configuration to " + site.getRemoteHost());

        dlg.open();
    }

    private Dialog createUploadDialog() {
        dlg = new Dialog();
        binder = new Binder<>(SiteUploadSettings.class);

        TextField destinationFolderField = new TextField("Destination folder");
        destinationFolderField.setWidthFull();
        destinationFolderField.setValue("/etc/openvpn/server");
        binder.forField(destinationFolderField)
                .asRequired()
                .bind(SiteUploadSettings::getUsername, SiteUploadSettings::setUsername);

        TextField username = new TextField("Username");
        username.setWidthFull();

        PasswordField passwordField = new PasswordField("Password");
        passwordField.setWidthFull();

        Checkbox requireSudoField = new Checkbox("Get sudo Access");
        requireSudoField.setWidthFull();

        VerticalLayout usernamePwdLayout = new VerticalLayout(
                username,
                passwordField,
                requireSudoField
        );
        usernamePwdLayout.setMargin(false);
        usernamePwdLayout.setPadding(false);
        usernamePwdLayout.setWidthFull();

        TextArea privateKeyField = new TextArea("Private Key");
        privateKeyField.setHeight(10, Unit.EM);
        privateKeyField.setWidthFull();

        Checkbox restartOpenVpnField = new Checkbox("Restart openVPN Service");
        Checkbox enableOpenVpnField = new Checkbox("Enable openVPN service");
        VerticalLayout actionsLayout = new VerticalLayout(
                restartOpenVpnField,
                enableOpenVpnField
        );
        actionsLayout.setMargin(false);
        actionsLayout.setPadding(false);

        Select<OpenVpnSiteView.SshAuthType> authTypeSelect = new Select<>(
                "AuthenticationType",
                (e) -> {
                    switch ((OpenVpnSiteView.SshAuthType) e.getValue()) {
                        case USERNAME_PASSWORD -> {
                            usernamePwdLayout.setVisible(true);
                            privateKeyField.setVisible(false);
                        }
                        case PRESHARED_KEY -> {
                            usernamePwdLayout.setVisible(false);
                            privateKeyField.setVisible(true);
                        }

                    }
                }
        );
        authTypeSelect.setItems(OpenVpnSiteView.SshAuthType.values());
        authTypeSelect.setWidthFull();
        authTypeSelect.setValue(OpenVpnSiteView.SshAuthType.USERNAME_PASSWORD);

        FormLayout layout = new FormLayout(
                destinationFolderField,
                authTypeSelect,
                usernamePwdLayout,
                privateKeyField,
                actionsLayout
        );
        layout.setColspan(destinationFolderField, 2);
        layout.setColspan(authTypeSelect, 2);
        dlg.add(layout);

        Button okButton = new Button("OK", (e) -> {
            dlg.close();
            onUploadConfig();
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", (e) -> {
            dlg.close();
        });

        dlg.getFooter().add(cancelButton, okButton);

        return dlg;
    }

    private void onUploadConfig() {
        JSch ssh = new JSch();
    }
}

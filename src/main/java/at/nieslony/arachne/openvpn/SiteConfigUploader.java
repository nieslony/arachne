/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.ssh.SshAuthType;
import static at.nieslony.arachne.ssh.SshAuthType.PRESHARED_KEY;
import static at.nieslony.arachne.ssh.SshAuthType.USERNAME_PASSWORD;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author claas
 */
@Service
public class SiteConfigUploader {

    private static final Logger logger = LoggerFactory.getLogger(SiteConfigUploader.class);

    @Getter
    @Setter
    private class SiteUploadSettings {

        private String username = "";
        private String password = "";
        private boolean sudoRequired = false;
        private boolean restartOpenVpn = false;
        private boolean enableOpenVpn = false;
        private String destinationFolder = "/etc/openvpn/server";
    }

    private Dialog dlg;
    private Binder<SiteUploadSettings> binder;
    private VpnSite vpnSite;
    private final SiteUploadSettings uploadSettings;

    @Autowired
    OpenVpnRestController openVPnRestController;

    public SiteConfigUploader() {
        uploadSettings = new SiteUploadSettings();
        dlg = createUploadDialog();
    }

    public void openDialog(VpnSite site) {
        dlg.setHeaderTitle("Upload Configuration to " + site.getRemoteHost());
        this.vpnSite = site;

        dlg.open();
    }

    private Dialog createUploadDialog() {
        dlg = new Dialog();
        binder = new Binder<>(SiteUploadSettings.class);
        binder.setBean(uploadSettings);

        TextField destinationFolderField = new TextField("Destination folder");
        destinationFolderField.setWidthFull();
        binder.forField(destinationFolderField)
                .asRequired()
                .bind(SiteUploadSettings::getDestinationFolder, SiteUploadSettings::setDestinationFolder);

        TextField usernameField = new TextField("Username");
        usernameField.setWidthFull();
        binder.forField(usernameField)
                .asRequired()
                .bind(SiteUploadSettings::getUsername, SiteUploadSettings::setUsername);

        PasswordField passwordField = new PasswordField("Password");
        passwordField.setWidthFull();
        binder.forField(passwordField)
                .bind(SiteUploadSettings::getPassword, SiteUploadSettings::setPassword);

        Checkbox requireSudoField = new Checkbox("Get sudo Access");
        requireSudoField.setWidthFull();
        binder.forField(requireSudoField)
                .bind(SiteUploadSettings::isSudoRequired, SiteUploadSettings::setSudoRequired);

        TextArea privateKeyField = new TextArea("Private Key");
        privateKeyField.setHeight(10, Unit.EM);
        privateKeyField.setWidthFull();

        Checkbox restartOpenVpnField = new Checkbox("Restart openVPN Service");
        Checkbox enableOpenVpnField = new Checkbox("Enable openVPN service");

        Select<SshAuthType> authTypeSelect = new Select<>(
                "AuthenticationType",
                (e) -> {
                    switch ((SshAuthType) e.getValue()) {
                        case USERNAME_PASSWORD -> {
                            passwordField.setVisible(true);
                            privateKeyField.setVisible(false);
                        }
                        case PRESHARED_KEY -> {
                            passwordField.setVisible(false);
                            privateKeyField.setVisible(true);
                        }

                    }
                }
        );
        authTypeSelect.setItems(SshAuthType.values());
        authTypeSelect.setWidthFull();
        authTypeSelect.setValue(SshAuthType.USERNAME_PASSWORD);

        FormLayout authLayout = new FormLayout(
                usernameField,
                authTypeSelect,
                passwordField,
                privateKeyField
        );

        FormLayout actionsLayout = new FormLayout(
                destinationFolderField,
                restartOpenVpnField,
                enableOpenVpnField
        );

        FormLayout layout = new FormLayout(
                authLayout,
                actionsLayout
        );
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
        SiteUploadSettings uploadSettings = binder.getBean();
        Session session = null;
        ChannelExec execChannel = null;
        ChannelSftp sftpChannel = null;

        try {
            session = ssh.getSession(uploadSettings.getUsername(), vpnSite.getRemoteHost());
            session.setPassword(uploadSettings.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            logger.info("Open Channel");
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            StringWriter writer = new StringWriter();
            openVPnRestController.writeOpenVpnSiteRemoteConfig(vpnSite.getId(), writer);
            logger.info("put ");
            sftpChannel.put(
                    new ByteArrayInputStream(writer.toString().getBytes()),
                    "%s-arachne-%s.conf".formatted(
                            uploadSettings.getDestinationFolder(),
                            vpnSite.getRemoteHost()
                    )
            );
            logger.info("Disconnect");
            sftpChannel.disconnect();
        } catch (JSchException ex) {
            logger.error(
                    "Cannot connect to %s: %s".formatted(
                            vpnSite.getRemoteHost(),
                            ex.getMessage()
                    )
            );
        } catch (SftpException ex) {
            logger.error("SSH error: " + ex.getMessage());
        } finally {
            if (session != null) {
                session.disconnect();
            }
            if (execChannel != null) {
                execChannel.disconnect();
            }
        }
    }
}

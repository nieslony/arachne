/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.ssh.SshAuthType;
import static at.nieslony.arachne.ssh.SshAuthType.PUBLIC_KEY;
import static at.nieslony.arachne.ssh.SshAuthType.USERNAME_PASSWORD;
import at.nieslony.arachne.ssh.SshKeyEntity;
import at.nieslony.arachne.ssh.SshKeyRepository;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.List;
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

    @Autowired
    SshKeyRepository sshKeyRepository;

    public SiteConfigUploader() {
        uploadSettings = new SiteUploadSettings();
    }

    @PostConstruct
    public void init() {
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

        Select<SshKeyEntity> sshKeys = new Select<>();
        sshKeys.setLabel("Public SSH Key");
        List<SshKeyEntity> sshKeyList = sshKeyRepository.findAll();
        sshKeys.setItems(sshKeyList);
        sshKeys.setWidthFull();
        sshKeys.setItemLabelGenerator((item) -> item.getLabel());

        Checkbox requireSudoField = new Checkbox("Sudo Access Required");
        requireSudoField.setWidthFull();
        binder.forField(requireSudoField)
                .bind(SiteUploadSettings::isSudoRequired, SiteUploadSettings::setSudoRequired);

        Checkbox restartOpenVpnField = new Checkbox("Restart openVPN Service");
        restartOpenVpnField.setWidthFull();

        Checkbox enableOpenVpnField = new Checkbox("Enable openVPN service");
        enableOpenVpnField.setWidthFull();

        Select<SshAuthType> authTypeSelect = new Select<>();
        authTypeSelect.setLabel("AuthenticationType");
        authTypeSelect.setItems(SshAuthType.values());
        authTypeSelect.setWidthFull();

        VerticalLayout authLayout = new VerticalLayout(
                usernameField,
                authTypeSelect,
                sshKeys,
                passwordField
        );
        authLayout.setPadding(false);
        authLayout.setSpacing(false);

        VerticalLayout actionsLayout = new VerticalLayout(
                requireSudoField,
                destinationFolderField,
                restartOpenVpnField,
                enableOpenVpnField
        );
        actionsLayout.setPadding(false);
        actionsLayout.setSpacing(false);

        HorizontalLayout layout = new HorizontalLayout(
                authLayout,
                actionsLayout
        );
        dlg.add(layout);

        Button okButton = new Button("OK", (e) -> {
            dlg.close();
            try {
                binder.writeBean(uploadSettings);
                onUploadConfig();
            } catch (ValidationException ex) {
                logger.error("Input validation Error: " + ex.getMessage());
            }
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", (e) -> {
            dlg.close();
        });

        dlg.getFooter().add(cancelButton, okButton);

        requireSudoField.addValueChangeListener((e) -> {
            passwordField.setEnabled(e.getValue() || authTypeSelect.getValue() == USERNAME_PASSWORD);
        });

        authTypeSelect.addValueChangeListener((e) -> {
            switch ((SshAuthType) e.getValue()) {
                case USERNAME_PASSWORD -> {
                    passwordField.setEnabled(true);
                    sshKeys.setEnabled(false);
                }
                case PUBLIC_KEY -> {
                    passwordField.setEnabled(requireSudoField.getValue());
                    sshKeys.setEnabled(true);
                }

            }
        });

        sshKeys.setValue(sshKeyList.get(0));
        authTypeSelect.setValue(SshAuthType.USERNAME_PASSWORD);

        return dlg;
    }

    private void onUploadConfig() {
        JSch ssh = new JSch();
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

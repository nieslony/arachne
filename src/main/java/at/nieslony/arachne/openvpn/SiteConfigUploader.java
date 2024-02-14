/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.ssh.SshAuthType;
import static at.nieslony.arachne.ssh.SshAuthType.PUBLIC_KEY;
import static at.nieslony.arachne.ssh.SshAuthType.USERNAME_PASSWORD;
import at.nieslony.arachne.ssh.SshKeyEntity;
import at.nieslony.arachne.ssh.SshKeyRepository;
import at.nieslony.arachne.utils.ShowNotification;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

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
        private SshAuthType sshAuthType;
        private SshKeyEntity sshKey;
    }

    private Dialog dlg;
    private Binder<SiteUploadSettings> binder;
    private VpnSite vpnSite;
    private final SiteUploadSettings uploadSettings;

    @Autowired
    OpenVpnRestController openVPnRestController;

    @Autowired
    SshKeyRepository sshKeyRepository;

    @Autowired
    private Settings settings;

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
        binder.forField(sshKeys)
                .bind(SiteUploadSettings::getSshKey, SiteUploadSettings::setSshKey);

        Checkbox requireSudoField = new Checkbox("Sudo Access Required");
        requireSudoField.setWidthFull();
        binder.forField(requireSudoField)
                .bind(SiteUploadSettings::isSudoRequired, SiteUploadSettings::setSudoRequired);

        Checkbox restartOpenVpnField = new Checkbox("Restart openVPN Service");
        restartOpenVpnField.setWidthFull();
        binder.forField(restartOpenVpnField)
                .bind(SiteUploadSettings::isRestartOpenVpn, SiteUploadSettings::setRestartOpenVpn);

        Checkbox enableOpenVpnField = new Checkbox("Enable openVPN service");
        enableOpenVpnField.setWidthFull();
        binder.forField(enableOpenVpnField)
                .bind(SiteUploadSettings::isEnableOpenVpn, SiteUploadSettings::setEnableOpenVpn);

        Select<SshAuthType> authTypeSelect = new Select<>();
        authTypeSelect.setLabel("AuthenticationType");
        authTypeSelect.setItems(SshAuthType.values());
        authTypeSelect.setWidthFull();
        binder.forField(authTypeSelect)
                .bind(SiteUploadSettings::getSshAuthType, SiteUploadSettings::setSshAuthType);

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

        authTypeSelect.setValue(SshAuthType.USERNAME_PASSWORD);

        dlg.addOpenedChangeListener((ocl) -> {
            if (ocl.isOpened()) {
                sshKeyList.clear();
                sshKeyList.addAll(sshKeyRepository.findAll());
                sshKeys.setItems(sshKeyList);
                if (!sshKeyList.isEmpty()) {
                    sshKeys.setValue(sshKeyList.get(0));
                }
            }
        });
        return dlg;
    }

    private String buildUploadCommand() {
        OpenVpnSiteSettings siteSettings = settings.getSettings(OpenVpnSiteSettings.class);
        String configName = openVPnRestController.getOpenVpnSiteRemoiteConfigName(siteSettings, vpnSite);
        String outputFile = "/tmp/%s.conf".formatted(configName);
        String sudo = uploadSettings.isSudoRequired() ? "sudo -S -p 'Sudo: '" : "";
        StringWriter configWriter = new StringWriter();
        openVPnRestController.writeOpenVpnSiteRemoteConfig(vpnSite.getId(), configWriter);

        return new StringBuilder()
                .append("%s mkdir -pv %s || exit 1\n".formatted(sudo, uploadSettings.getDestinationFolder()))
                .append("""
                           cat <<EOF > %s
                           %s
                           EOF
                           """.formatted(outputFile, configWriter.toString()))
                .append("%s mv -v %s %s || exit 1\n".formatted(
                        sudo,
                        outputFile,
                        uploadSettings.getDestinationFolder()
                ))
                .append(uploadSettings.isRestartOpenVpn()
                        ? "%s systemctl restart openvpn@%s || exit 1\n".formatted(sudo, configName)
                        : ""
                )
                .append(uploadSettings.isEnableOpenVpn()
                        ? "%s systemctl enable openvpn@%s || exit 1\n".formatted(sudo, configName)
                        : ""
                )
                .append("sleep 1\n")
                .toString();
    }

    private void onUploadConfig() {
        Dialog uploadingDlg = new Dialog("Uploading...");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);

        NativeLabel label = new NativeLabel("Connecting...");

        Button cancelButton = new Button("Cancel");

        UI ui = UI.getCurrent();
        Thread uploadThread = new Thread(
                () -> {
                    ui.access(() -> uploadingDlg.open());
                    Notification notification = _onUploadConfig(ui, label);
                    ui.access(() -> uploadingDlg.close());
                    if (notification != null) {
                        ui.access(() -> notification.open());
                        try {
                            wait();
                        } catch (InterruptedException | IllegalMonitorStateException ex) {
                        }
                    }
                    logger.info("Terminated.");
                },
                "Upload Site Configuration"
        );

        uploadingDlg.add(label, progressBar);
        uploadingDlg.getFooter().add(cancelButton);

        cancelButton.addClickListener((t) -> {
            uploadingDlg.close();
            uploadThread.interrupt();
        });

        uploadThread.start();
    }

    private Notification _onUploadConfig(UI ui, NativeLabel infoLabel) {
        JSch ssh = new JSch();
        Session session = null;
        ChannelExec execChannel = null;
        String command = buildUploadCommand();
        Notification notification = null;

        try {
            session = ssh.getSession(uploadSettings.getUsername(), vpnSite.getRemoteHost());
            switch (uploadSettings.getSshAuthType()) {
                case USERNAME_PASSWORD ->
                    session.setPassword(uploadSettings.getPassword());
                case PUBLIC_KEY -> {
                    SshKeyEntity sshKey = uploadSettings.getSshKey();
                    ssh.addIdentity(
                            sshKey.getComment(),
                            sshKey.getPrivateKey().getBytes(),
                            sshKey.getPublicKey().getBytes(),
                            "".getBytes());
                }
            }
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            execChannel = (ChannelExec) session.openChannel("exec");
            InputStream in = execChannel.getInputStream();
            InputStream err = execChannel.getErrStream();
            OutputStream out = execChannel.getOutputStream();
            execChannel.setCommand(command);
            execChannel.setPty(true);
            ui.access(() -> infoLabel.setText("Executing script..."));
            execChannel.connect();

            if (uploadSettings.isSudoRequired()) {
                logger.info("Sending passwortd for sudo");
                out.write((uploadSettings.getPassword() + "\n").getBytes());
                out.flush();
            }
            byte[] tmp = new byte[1024];
            StringBuilder msgs = new StringBuilder();
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    msgs.append(new String(tmp, 0, i));
                }
                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    msgs.append(new String(tmp, 0, i));
                }
                if (execChannel.isClosed()) {
                    var exitStatus = execChannel.getExitStatus();
                    if (exitStatus == 0) {
                        String msg = "Configuration successfully uploaded to " + vpnSite.getRemoteHost();
                        logger.info(msg);
                        notification = ShowNotification.createInfo(msg);
                    } else {
                        String header = "Configuration upload failed";
                        String msg = HtmlUtils.htmlEscape(msgs.toString())
                                .replaceAll("\n", "<br>");
                        logger.error(header + ": " + msg);
                        notification = ShowNotification.createError(header, msg);
                    }
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
        } catch (IOException | JSchException ex) {
            String header = "Error connecting to " + vpnSite.getRemoteHost();
            logger.error(header + ": " + ex.getMessage());
            notification = ShowNotification.createError(header, ex.getMessage());
        } finally {
            if (execChannel != null) {
                execChannel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }

        return notification;
    }
}

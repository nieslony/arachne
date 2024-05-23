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
import at.nieslony.arachne.utils.net.NetUtils;
import at.nieslony.arachne.utils.validators.HostnameValidator;
import at.nieslony.arachne.utils.validators.IgnoringInvisibleOrDisabledValidator;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.util.HtmlUtils;

/**
 *
 * @author claas
 */
@Service
@SessionScope
public class SiteConfigUploader implements BeanFactoryAware {

    private static final Logger logger = LoggerFactory.getLogger(SiteConfigUploader.class);

    enum UploadConfigType {
        OvpnConfig(".ovpn file"),
        NMCL("NetworkManger");

        private UploadConfigType(String label) {
            this.label = label;
        }

        private String label;

        @Override
        public String toString() {
            return label;
        }
    }

    @Getter
    @Setter
    public class SiteUploadSettings {

        private String remoteHostName;
        private String username = "";
        private String password = "";
        private boolean sudoRequired = false;
        private boolean restartOpenVpn = false;
        private boolean enableOpenVpn = false;

        private UploadConfigType uploadConfigType = UploadConfigType.NMCL;

        private String connectionName = "OpenVPN_" + NetUtils.myHostname();
        private String certitifaceFolder = "/etc/pki/arachne";
        private boolean enableConnection;
        private boolean autostartConnection;

        private String destinationFolder = "/etc/openvpn/client";
        private SshAuthType sshAuthType = USERNAME_PASSWORD;
        private SshKeyEntity sshKey;
    }

    private Dialog dlg;
    private Binder<SiteUploadSettings> binder;
    private VpnSite vpnSite;
    private final SiteUploadSettings uploadSettings;
    private BeanFactory beanFactory;

    @Autowired
    OpenVpnRestController openVPnRestController;

    @Autowired
    SshKeyRepository sshKeyRepository;

    @Autowired
    private Settings settings;

    @Autowired
    private VpnSiteRepository vpnSiteRepository;

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
        if (uploadSettings.remoteHostName == null || uploadSettings.getRemoteHostName().isEmpty()) {
            uploadSettings.setRemoteHostName(site.getRemoteHost());
        }
        binder.setBean(uploadSettings);
        binder.validate();
        dlg.open();
    }

    private Dialog createUploadDialog() {
        dlg = new Dialog();
        binder = new Binder<>(SiteUploadSettings.class);

        TextField destinationFolderField = new TextField("Destination folder");
        destinationFolderField.setClearButtonVisible(true);
        destinationFolderField.setWidthFull();
        destinationFolderField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(destinationFolderField)
                .asRequired(new IgnoringInvisibleOrDisabledValidator<>(
                        new StringLengthValidator("Value required", 1, 65535)
                ))
                .bind(SiteUploadSettings::getDestinationFolder, SiteUploadSettings::setDestinationFolder);

        TextField remoteHostNameField = new TextField("Remote Host Name/IP");
        remoteHostNameField.setClearButtonVisible(true);
        remoteHostNameField.setWidthFull();
        remoteHostNameField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(remoteHostNameField)
                .asRequired()
                .withValidator(
                        new HostnameValidator()
                                .withIpAllowed(true)
                                .withResolvableRequired(true)
                )
                .bind(SiteUploadSettings::getRemoteHostName, SiteUploadSettings::setRemoteHostName);

        TextField usernameField = new TextField("Username");
        usernameField.setClearButtonVisible(true);
        usernameField.setWidthFull();
        usernameField.setValueChangeMode(ValueChangeMode.EAGER);
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

        RadioButtonGroup<UploadConfigType> uploadConfigTypeField = new RadioButtonGroup<>(
                "Upload Type",
                UploadConfigType.values()
        );
        binder.forField(uploadConfigTypeField)
                .bind(SiteUploadSettings::getUploadConfigType, SiteUploadSettings::setUploadConfigType);

        TextField connectionNameField = new TextField("Connection Name");
        connectionNameField.setWidthFull();
        connectionNameField.setClearButtonVisible(true);
        connectionNameField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(connectionNameField)
                .asRequired(new IgnoringInvisibleOrDisabledValidator<>(
                        new StringLengthValidator("Value required", 1, 65535)
                ))
                .bind(SiteUploadSettings::getConnectionName, SiteUploadSettings::setConnectionName);

        TextField certificateFolderField = new TextField("Certificate Folder");
        certificateFolderField.setClearButtonVisible(true);
        certificateFolderField.setWidthFull();
        certificateFolderField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(certificateFolderField)
                .asRequired(new IgnoringInvisibleOrDisabledValidator<>(
                        new StringLengthValidator("Value required", 1, 65535)
                ))
                .bind(SiteUploadSettings::getCertitifaceFolder, SiteUploadSettings::setCertitifaceFolder);

        Checkbox enableConnectionField = new Checkbox("Enable Connection");
        binder.forField(enableConnectionField)
                .bind(SiteUploadSettings::isEnableConnection, SiteUploadSettings::setEnableConnection);

        Checkbox autostartConnectionField = new Checkbox("Autostart Connection on Boot");
        binder.forField(autostartConnectionField)
                .bind(SiteUploadSettings::isAutostartConnection, SiteUploadSettings::setAutostartConnection);

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
                remoteHostNameField,
                usernameField,
                authTypeSelect,
                sshKeys,
                passwordField
        );
        authLayout.setPadding(false);
        authLayout.setSpacing(false);

        VerticalLayout actionsLayout = new VerticalLayout(
                requireSudoField,
                uploadConfigTypeField,
                connectionNameField,
                certificateFolderField,
                enableConnectionField,
                autostartConnectionField,
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
        dlg.setMinWidth(50, Unit.EM);

        Button okButton = new Button("OK", (e) -> {
            dlg.close();
            try {
                binder.writeBean(uploadSettings);
                OpenVpnSiteSettings siteSettings = settings.getSettings(OpenVpnSiteSettings.class);
                Thread thread = new NMConfigUploadThread(
                        uploadSettings,
                        vpnSite,
                        beanFactory
                );
                thread.start();
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
            if (e.getValue() != null) {
                switch (e.getValue()) {
                    case USERNAME_PASSWORD -> {
                        passwordField.setEnabled(true);
                        sshKeys.setEnabled(false);
                    }
                    case PUBLIC_KEY -> {
                        passwordField.setEnabled(requireSudoField.getValue());
                        sshKeys.setEnabled(true);
                    }

                }
            }
        });

        uploadConfigTypeField.addValueChangeListener((e) -> {
            switch (e.getValue()) {
                case NMCL -> {
                    connectionNameField.setVisible(true);
                    certificateFolderField.setEnabled(true);
                    enableConnectionField.setVisible(true);
                    autostartConnectionField.setVisible(true);
                    destinationFolderField.setVisible(false);
                    restartOpenVpnField.setVisible(false);
                    enableOpenVpnField.setVisible(false);
                }
                case OvpnConfig -> {
                    connectionNameField.setVisible(false);
                    certificateFolderField.setEnabled(false);
                    enableConnectionField.setVisible(false);
                    autostartConnectionField.setVisible(false);
                    destinationFolderField.setVisible(true);
                    restartOpenVpnField.setVisible(true);
                    enableOpenVpnField.setVisible(true);
                }
            }
            binder.validate();
        });

        binder.addStatusChangeListener(
                (e) -> okButton.setEnabled(!e.hasValidationErrors())
        );

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
        String configName = openVPnRestController.getOpenVpnSiteRemoteConfigName(siteSettings, vpnSite);
        String outputFile = "/tmp/%s".formatted(configName);
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
                .append("""
                        chown 600 %s
                        """.formatted(outputFile))
                .append("""
                        %s chown root:root %s
                        """.formatted(sudo, outputFile))
                .append("%s mv -v %s %s || exit 1\n".formatted(
                        sudo,
                        outputFile,
                        uploadSettings.getDestinationFolder()
                ))
                .append(uploadSettings.isRestartOpenVpn()
                        ? "%s SYSTEMD_COLORS=false systemctl restart openvpn-client@%s || exit 1\n".formatted(sudo, configName)
                        : ""
                )
                .append(uploadSettings.isEnableOpenVpn()
                        ? "%s SYSTEMD_COLORS=false systemctl enable openvpn-client@%s || exit 1\n".formatted(sudo, configName)
                        : ""
                )
                .append("sleep 1\n")
                .toString();
    }

    private void onUploadConfig__() {
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
            session = ssh.getSession(uploadSettings.getUsername(), uploadSettings.getRemoteHostName());
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
                        notification = ShowNotification.createError(
                                header,
                                new Html("<text>" + msg + "</text>")
                        );
                    }
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ee) {
                    logger.info("Upload interrupted");
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

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn.sitevpnupload;

import at.nieslony.arachne.openvpn.VpnSite;
import at.nieslony.arachne.openvpn.VpnSiteRepository;
import at.nieslony.arachne.openvpn.vpnsite.UploadConfigType;
import at.nieslony.arachne.ssh.SshAuthType;
import static at.nieslony.arachne.ssh.SshAuthType.PUBLIC_KEY;
import static at.nieslony.arachne.ssh.SshAuthType.USERNAME_PASSWORD;
import at.nieslony.arachne.ssh.SshKeyEntity;
import at.nieslony.arachne.ssh.SshKeyRepository;
import at.nieslony.arachne.utils.validators.HostnameValidator;
import at.nieslony.arachne.utils.validators.IgnoringInvisibleOrDisabledValidator;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

/**
 *
 * @author claas
 */
@Service
@SessionScope
@Slf4j
public class SiteConfigUploader implements BeanFactoryAware {

    private Dialog dlg;
    private Binder<SiteUploadSettings> binder;
    private SiteUploadSettings uploadSettings;
    private BeanFactory beanFactory;

    @Autowired
    SshKeyRepository sshKeyRepository;

    @Autowired
    VpnSiteRepository vpnSiteRepository;

    @PostConstruct
    public void init() {
        dlg = createUploadDialog();
    }

    public void openDialog(VpnSite site) {
        dlg.setHeaderTitle("Upload Configuration to " + site.getSiteHostname());
        uploadSettings = new SiteUploadSettings(site);
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
                .bind(
                        (src) -> src.getVpnSite().getDestinationFolder(),
                        (dst, v) -> dst.getVpnSite().setDestinationFolder(v)
                );

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
                .bind(
                        (src) -> src.getVpnSite().getUploadToHost(),
                        (dst, v) -> dst.getVpnSite().setUploadToHost(v)
                );

        TextField usernameField = new TextField("Username");
        usernameField.setClearButtonVisible(true);
        usernameField.setWidthFull();
        usernameField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(usernameField)
                .asRequired()
                .bind(
                        (src) -> src.getVpnSite().getUsername(),
                        (dst, v) -> dst.getVpnSite().setUsername(v)
                );

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
                .bind(
                        (src) -> src.getVpnSite().getSshKey(),
                        (dst, v) -> dst.getVpnSite().setSshKey(v)
                );

        Checkbox requireSudoField = new Checkbox("Sudo Access Required");
        requireSudoField.setWidthFull();
        binder.forField(requireSudoField)
                .bind(
                        (src) -> src.getVpnSite().isSudoRequired(),
                        (dst, v) -> dst.getVpnSite().setSudoRequired(v)
                );

        RadioButtonGroup<UploadConfigType> uploadConfigTypeField = new RadioButtonGroup<>(
                "Upload Type",
                UploadConfigType.values()
        );
        binder.forField(uploadConfigTypeField)
                .bind(
                        (src) -> src.getVpnSite().getUploadConfigType(),
                        (dst, v) -> dst.getVpnSite().setUploadConfigType(v)
                );

        TextField connectionNameField = new TextField("Connection Name");
        connectionNameField.setWidthFull();
        connectionNameField.setClearButtonVisible(true);
        connectionNameField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(connectionNameField)
                .asRequired(new IgnoringInvisibleOrDisabledValidator<>(
                        new StringLengthValidator("Value required", 1, 65535)
                ))
                .bind(
                        (src) -> src.getVpnSite().getConnectionName(),
                        (dst, v) -> dst.getVpnSite().setConnectionName(v)
                );

        TextField certificateFolderField = new TextField("Certificate Folder");
        certificateFolderField.setClearButtonVisible(true);
        certificateFolderField.setWidthFull();
        certificateFolderField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(certificateFolderField)
                .asRequired(new IgnoringInvisibleOrDisabledValidator<>(
                        new StringLengthValidator("Value required", 1, 65535)
                ))
                .bind(
                        (src) -> src.getVpnSite().getCertitifaceFolder(),
                        (dst, v) -> dst.getVpnSite().setCertitifaceFolder(v)
                );

        Checkbox enableConnectionField = new Checkbox("Enable Connection");
        binder.forField(enableConnectionField)
                .bind(
                        (src) -> src.getVpnSite().isEnableConnection(),
                        (dst, v) -> dst.getVpnSite().setEnableConnection(v)
                );

        Checkbox autostartConnectionField = new Checkbox("Autostart Connection on Boot");
        binder.forField(autostartConnectionField)
                .bind(
                        (src) -> src.getVpnSite().isAutostartConnection(),
                        (dst, v) -> dst.getVpnSite().setAutostartConnection(v)
                );

        Checkbox restartOpenVpnField = new Checkbox("Restart openVPN Service");
        restartOpenVpnField.setWidthFull();
        binder.forField(restartOpenVpnField)
                .bind(
                        (src) -> src.getVpnSite().isRestartOpenVpn(),
                        (dst, v) -> dst.getVpnSite().setRestartOpenVpn(v)
                );

        Checkbox enableOpenVpnField = new Checkbox("Enable openVPN service");
        enableOpenVpnField.setWidthFull();
        binder.forField(enableOpenVpnField)
                .bind(
                        (src) -> src.getVpnSite().isEnableOpenVpn(),
                        (dst, v) -> dst.getVpnSite().setEnableOpenVpn(v)
                );

        Select<SshAuthType> authTypeSelect = new Select<>();
        authTypeSelect.setLabel("AuthenticationType");
        authTypeSelect.setItems(SshAuthType.values());
        authTypeSelect.setWidthFull();
        binder.forField(authTypeSelect)
                .bind(
                        (src) -> src.getVpnSite().getSshAuthType(),
                        (dst, v) -> dst.getVpnSite().setSshAuthType(v)
                );

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
                Thread thread = switch (uploadSettings.getVpnSite().getUploadConfigType()) {
                    case NMCL ->
                        new NMConfigUploadThread(uploadSettings, beanFactory);
                    case OvpnConfig ->
                        new OvpnConfigUploadThread(uploadSettings, beanFactory);
                };
                vpnSiteRepository.save(uploadSettings.getVpnSite());
                thread.start();
            } catch (ValidationException ex) {
                log.error("Input validation Error: " + ex.getMessage());
            }
        });
        okButton.addThemeVariants(ButtonVariant.AURA_PRIMARY);

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
                    certificateFolderField.setVisible(true);
                    enableConnectionField.setVisible(true);
                    autostartConnectionField.setVisible(true);
                    destinationFolderField.setVisible(false);
                    restartOpenVpnField.setVisible(false);
                    enableOpenVpnField.setVisible(false);
                }
                case OvpnConfig -> {
                    connectionNameField.setVisible(false);
                    certificateFolderField.setVisible(false);
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

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}

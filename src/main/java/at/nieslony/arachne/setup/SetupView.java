/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.setup;

import at.nieslony.arachne.AdminHome;
import at.nieslony.arachne.pki.CertSpecs;
import at.nieslony.arachne.settings.SettingsException;
import at.nieslony.arachne.utils.FolderFactory;
import at.nieslony.arachne.utils.ShowNotification;
import at.nieslony.arachne.utils.net.NetUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.bouncycastle.asn1.x500.X500Name;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author claas
 */
public class SetupView extends VerticalLayout {

    private static final org.slf4j.Logger logger
            = LoggerFactory.getLogger(SetupView.class);

    private TextField caCommonName;
    private TextField caOrganizationalUnit;
    private TextField caOrganization;
    private TextField caCountry;
    private TextField caState;
    private TextField caLocation;
    private TextField caSubject;
    private IntegerField caCertLifeTimeDays;
    private Select<String> caKeyAlgo;
    private Select<Integer> caRsaKeySize;
    private Select<String> caSignatureAlgo;

    private TextField serverCommonName;
    private TextField serverOrganizationalUnit;
    private TextField serverOrganization;
    private TextField serverCountry;
    private TextField serverState;
    private TextField serverLocation;
    private TextField serverSubject;
    private IntegerField serverCertLifeTimeDays;
    private Select<String> serverKeyAlgo;
    private Select<Integer> serverRsaKeySize;
    private Select<String> serverSignatureAlgo;
    private Select<Integer> serverDhParamsLength;

    private TextField adminUsername;
    private TextField adminDisplayName;
    private EmailField adminEmail;
    private PasswordField adminPassword;

    private Button next;
    private Button prev;
    private Button finish;
    private NativeLabel finishError;

    SetupController setupController;
    FolderFactory folderFactory;
    Binder<SetupData> binder;

    public SetupView(
            SetupController setupController,
            FolderFactory folderFactory
    ) {
        this.setupController = setupController;
        this.folderFactory = folderFactory;
        binder = new Binder<>();

        H1 header = new H1("Arachne Setup Wizard");

        TabSheet tabSheet = new TabSheet();

        tabSheet.add("Welcome", createRestoreTab());
        tabSheet.add("CA Certificate", createCaCertificateTab());
        tabSheet.add("Server Certificate", createServerCertificateTab());
        tabSheet.add("Admin User", createAdminUserTab());
        tabSheet.add("Summary", createSummaryTab());
        tabSheet.addSelectedChangeListener((t) -> {
            int noTabs = 5;
            int curTab = tabSheet.getSelectedIndex();
            next.setEnabled(curTab < noTabs - 1);
            prev.setEnabled(curTab > 0);
        });
        HorizontalLayout buttons = new HorizontalLayout();
        next = new Button("Next");
        next.addClickListener((t) -> {
            int noTabs = 5;
            int curTab = tabSheet.getSelectedIndex();
            if (curTab < noTabs - 1) {
                tabSheet.setSelectedIndex(curTab + 1);
            }
        });
        prev = new Button("Prev");
        prev.addClickListener((t) -> {
            int curTab = tabSheet.getSelectedIndex();
            if (curTab > 0) {
                tabSheet.setSelectedIndex(curTab - 1);
            }
        });
        prev.setEnabled(false);

        buttons.add(
                prev,
                next
        );

        binder.addStatusChangeListener((sce) -> {
            finish.setEnabled(!sce.hasValidationErrors());
            finishError.setVisible(sce.hasValidationErrors());
        });

        add(header, tabSheet, buttons);
        setJustifyContentMode(JustifyContentMode.CENTER);
        binder.validate();
    }

    private void updateCaSubject() {
        StringWriter subjectWriter = new StringWriter();
        subjectWriter.append("CN=" + caCommonName.getValue());
        if (!caOrganizationalUnit.getValue().isEmpty()) {
            subjectWriter.append(",OU=" + caOrganizationalUnit.getValue());
        }
        if (!caOrganization.getValue().isEmpty()) {
            subjectWriter.append(",o=" + caOrganization.getValue());
        }
        if (!caCountry.getValue().isEmpty()) {
            subjectWriter.append(",c=" + caCountry.getValue());
        }
        if (!caState.getValue().isEmpty()) {
            subjectWriter.append(",st=" + caState.getValue());
        }
        if (!caLocation.getValue().isEmpty()) {
            subjectWriter.append(",l=" + caLocation.getValue());
        }

        X500Name subject = new X500Name(subjectWriter.toString());
        caSubject.setValue(subject.toString());
    }

    private void updateServerSubject() {
        StringWriter subjectWriter = new StringWriter();
        subjectWriter.append("CN=" + serverCommonName.getValue());
        if (!serverOrganizationalUnit.getValue().isEmpty()) {
            subjectWriter.append(",OU=" + serverOrganizationalUnit.getValue());
        }
        if (!serverOrganization.getValue().isEmpty()) {
            subjectWriter.append(",o=" + serverOrganization.getValue());
        }
        if (!serverCountry.getValue().isEmpty()) {
            subjectWriter.append(",c=" + serverCountry.getValue());
        }
        if (!serverState.getValue().isEmpty()) {
            subjectWriter.append(",st=" + serverState.getValue());
        }
        if (!serverLocation.getValue().isEmpty()) {
            subjectWriter.append(",l=" + serverLocation.getValue());
        }

        X500Name subject = new X500Name(subjectWriter.toString());
        serverSubject.setValue(subject.toString());
    }

    private FormLayout createCaCertificateTab() {

        FormLayout subjectLayout = new FormLayout();
        caCommonName = new TextField("Common Name");
        caCommonName.addValueChangeListener((e) -> {
            updateCaSubject();
        });
        caCommonName.setRequiredIndicatorVisible(true);
        caCommonName.setErrorMessage("Common Name is required");
        caCommonName.setClearButtonVisible(true);

        caOrganizationalUnit = new TextField("Organizational Unit");
        caOrganizationalUnit.addValueChangeListener((e) -> {
            updateCaSubject();
        });
        caOrganizationalUnit.setClearButtonVisible(true);

        caOrganization = new TextField("Organization");
        caOrganization.addValueChangeListener((e) -> {
            updateCaSubject();
        });
        caOrganization.setClearButtonVisible(true);

        caCountry = new TextField("Country");
        caCountry.addValueChangeListener((e) -> {
            updateCaSubject();
        });
        caCountry.setClearButtonVisible(true);

        caState = new TextField("State");
        caState.addValueChangeListener((e) -> {
            updateCaSubject();
        });
        caState.setClearButtonVisible(true);

        caLocation = new TextField("Location");
        caLocation.addValueChangeListener((e) -> {
            updateCaSubject();
        });
        caLocation.setClearButtonVisible(true);

        caSubject = new TextField("Subject");
        caSubject.setEnabled(false);

        caCertLifeTimeDays = new IntegerField("Certificate Lifetime");
        caCertLifeTimeDays.setMin(1);
        caCertLifeTimeDays.setStepButtonsVisible(true);
        Div daysSuffix = new Div();
        daysSuffix.setText("days");
        caCertLifeTimeDays.setSuffixComponent(daysSuffix);

        caKeyAlgo = new Select<>();
        caKeyAlgo.setLabel("Key Algorithm");
        caKeyAlgo.setItems("RSA");

        caRsaKeySize = new Select<>();
        caRsaKeySize.setLabel("RSA Key Size");
        caRsaKeySize.setItems(2048, 4096);

        caSignatureAlgo = new Select<>();
        caSignatureAlgo.setLabel("Signature Algorithm");
        caSignatureAlgo.setItems("SHA256withRSA");

        caCommonName.setValue("Arachne CA");
        caCertLifeTimeDays.setValue(3650);
        caKeyAlgo.setValue("RSA");
        caRsaKeySize.setValue(2048);
        caSignatureAlgo.setValue("SHA256withRSA");

        subjectLayout.add(
                caCommonName,
                caOrganizationalUnit,
                caOrganization,
                caCountry,
                caState,
                caLocation,
                caSubject,
                caCertLifeTimeDays,
                caKeyAlgo,
                caRsaKeySize,
                caSignatureAlgo
        );
        subjectLayout.setColspan(caSubject, 3);

        return subjectLayout;
    }

    private FormLayout createServerCertificateTab() {
        FormLayout subjectLayout = new FormLayout();

        AtomicReference<String> cn = new AtomicReference<>();
        serverCommonName = new TextField("Common Name");
        serverCommonName.addValueChangeListener((e) -> {
            updateServerSubject();
        });
        serverCommonName.setPattern("[a-z][a-z0-9\\-]*(\\.[a-z][a-z0-9\\-]*)*");
        serverCommonName.setRequiredIndicatorVisible(true);
        serverCommonName.setErrorMessage("Common Name is required");
        serverCommonName.setClearButtonVisible(true);
        binder.forField(serverCommonName)
                .bind(
                        (src) -> cn.get(),
                        (dst, val) -> cn.set(val)
                );

        serverOrganizationalUnit = new TextField("Organizational Unit");
        serverOrganizationalUnit.addValueChangeListener((e) -> {
            updateServerSubject();
        });
        serverOrganizationalUnit.setClearButtonVisible(true);

        serverOrganization = new TextField("Organization");
        serverOrganization.addValueChangeListener((e) -> {
            updateServerSubject();
        });
        serverOrganization.setClearButtonVisible(true);

        serverCountry = new TextField("Country");
        serverCountry.addValueChangeListener((e) -> {
            updateServerSubject();
        });
        serverCountry.setClearButtonVisible(true);

        serverState = new TextField("State");
        serverState.addValueChangeListener((e) -> {
            updateServerSubject();
        });
        serverState.setClearButtonVisible(true);

        serverLocation = new TextField("Location");
        serverLocation.addValueChangeListener((e) -> {
            updateServerSubject();
        });
        serverLocation.setClearButtonVisible(true);

        serverSubject = new TextField("Subject");
        serverSubject.setEnabled(false);

        serverCertLifeTimeDays = new IntegerField("Certificate Lifetime");
        serverCertLifeTimeDays.setMin(1);
        serverCertLifeTimeDays.setStepButtonsVisible(true);
        Div daysSuffix = new Div();
        daysSuffix.setText("days");
        serverCertLifeTimeDays.setSuffixComponent(daysSuffix);

        serverKeyAlgo = new Select<>();
        serverKeyAlgo.setLabel("Key Algorithm");
        serverKeyAlgo.setItems("RSA");

        serverRsaKeySize = new Select<>();
        serverRsaKeySize.setLabel("RSA Key Size");
        serverRsaKeySize.setItems(2048, 4096);

        serverSignatureAlgo = new Select<>();
        serverSignatureAlgo.setLabel("Signature Algorithm");
        serverSignatureAlgo.setItems("SHA256withRSA");

        serverDhParamsLength = new Select<>();
        serverDhParamsLength.setLabel("DH Paramaters Length");
        serverDhParamsLength.setItems(1024, 2048, 4096);

        serverCommonName.setValue(NetUtils.myHostname());
        serverCertLifeTimeDays.setValue(365);
        serverKeyAlgo.setValue("RSA");
        serverRsaKeySize.setValue(2048);
        serverSignatureAlgo.setValue("SHA256withRSA");
        serverDhParamsLength.setValue(2048);

        subjectLayout.add(
                serverCommonName,
                serverOrganizationalUnit,
                serverOrganization,
                serverCountry,
                serverState,
                serverLocation,
                serverSubject,
                serverCertLifeTimeDays,
                serverKeyAlgo,
                serverRsaKeySize,
                serverSignatureAlgo,
                serverDhParamsLength
        );
        subjectLayout.setColspan(serverSubject, 3);

        return subjectLayout;
    }

    private FormLayout createAdminUserTab() {
        FormLayout layout = new FormLayout();

        adminUsername = new TextField("Username");
        adminUsername.setPattern("[a-z][a-z0-9\\-_.]*");
        adminUsername.setRequired(true);
        adminUsername.setRequiredIndicatorVisible(true);
        adminUsername.setValue("admin");
        adminUsername.setErrorMessage("Username is invalid");
        binder.forField(adminUsername)
                .bind(SetupData::getAdminUsername, SetupData::setAdminUsername);

        adminDisplayName = new TextField("Display Name");

        adminEmail = new EmailField("E-Mail Address");
        binder.forField(adminEmail)
                .bind(SetupData::getAdminEmail, SetupData::setAdminEmail);

        adminPassword = new PasswordField("Password");
        adminPassword.setValueChangeMode(ValueChangeMode.EAGER);
        adminPassword.setRequired(true);
        adminPassword.setRequiredIndicatorVisible(true);
        binder.forField(adminPassword)
                .asRequired("Password required")
                .bind(SetupData::getAdminPassword, SetupData::setAdminPassword);

        AtomicReference<String> retypePwd = new AtomicReference<>();
        PasswordField retypePassword = new PasswordField("Retype Password");
        retypePassword.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(retypePassword)
                .withValidator(
                        v -> adminPassword.getValue().equals(retypePassword.getValue()),
                        "Passwords don't match"
                )
                .bind(
                        (src) -> retypePwd.get(),
                        (dst, val) -> retypePwd.set(val)
                );

        adminDisplayName.setValue("Arachne Administrator");

        adminPassword.addValueChangeListener((e) -> {
            binder.validate();
        });

        layout.add(
                adminUsername,
                adminDisplayName,
                adminPassword,
                retypePassword,
                adminEmail
        );

        return layout;
    }

    private VerticalLayout createSummaryTab() {
        VerticalLayout layout = new VerticalLayout();

        Text text = new Text(
                """
                Now it's to time to setup Archne. Please be patient, it can
                take a couple of minutes,
                """);

        finishError = new NativeLabel("There are errors validating input. Please check");
        finishError.addClassName(LumoUtility.TextColor.ERROR);

        finish = new Button("Finish");
        finish.setDisableOnClick(true);
        finish.addClickListener((var t) -> {
            SetupData setupData = new SetupData();

            CertSpecs caCertSpecs = new CertSpecs(CertSpecs.CertSpecType.CA_SPEC);
            caCertSpecs.setSubject(caSubject.getValue());
            caCertSpecs.setKeyAlgo(caKeyAlgo.getValue());
            caCertSpecs.setKeySize(caRsaKeySize.getValue());
            caCertSpecs.setCertLifeTimeDays(caCertLifeTimeDays.getValue());
            caCertSpecs.setSignatureAlgo(caSignatureAlgo.getValue());

            CertSpecs serverCertSpecs = new CertSpecs(CertSpecs.CertSpecType.SERVER_SPEC);
            serverCertSpecs.setSubject(serverSubject.getValue());
            serverCertSpecs.setKeyAlgo(serverKeyAlgo.getValue());
            serverCertSpecs.setKeySize(serverRsaKeySize.getValue());
            serverCertSpecs.setCertLifeTimeDays(serverCertLifeTimeDays.getValue());
            serverCertSpecs.setSignatureAlgo(serverSignatureAlgo.getValue());
            setupData.setDhParamsBits(serverDhParamsLength.getValue());

            CertSpecs userCertSpecs = new CertSpecs(CertSpecs.CertSpecType.USER_SPEC);
            userCertSpecs.setSubject("cn={username}");
            userCertSpecs.setKeyAlgo("RSA");
            userCertSpecs.setKeySize(2048);
            userCertSpecs.setCertLifeTimeDays(365);
            userCertSpecs.setSignatureAlgo("SHA256withRSA");

            setupData.setCaCertSpecs(caCertSpecs);
            setupData.setServerCertSpecs(serverCertSpecs);
            setupData.setUserCertSpecs(userCertSpecs);

            setupData.setAdminUsername(adminUsername.getValue());
            setupData.setAdminPassword(adminPassword.getValue());
            setupData.setAdminEmail(adminEmail.getValue());

            try {
                setupController.setupArachne(setupData);
            } catch (SettingsException ex) {
                logger.error("Cannot during setup: " + ex.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            logger.info("Redirecting to /");
            getUI().get().navigate(AdminHome.class);
        });

        layout.add(text, finishError, finish);

        return layout;
    }

    private Component createRestoreTab() {
        VerticalLayout layout = new VerticalLayout();

        H2 welcome = new H2("Welcome to Arachne Setup Wizard!");

        Paragraph restoreFoundText;
        Component restoreButton;
        Path restorePath = Path.of(folderFactory.getRestorePath()).toAbsolutePath();
        if (Files.exists(restorePath)) {
            restoreFoundText = new Paragraph(
                    "Backup file %s found."
                            .formatted(restorePath.toString())
            );
            restoreButton = new Button("Restore settings...", (e) -> {
                try {
                    FileInputStream fis = new FileInputStream(restorePath.toString());
                    setupController.restore(fis);
                } catch (FileNotFoundException ex) {
                    logger.error(
                            "Cannot read %s: %s"
                                    .formatted(
                                            restorePath.toString(),
                                            ex.getMessage()
                                    )
                    );
                }
            });
        } else {
            restoreFoundText = new Paragraph(
                    """
                    No backup file %s found. Restore from uploaded file or
                    just continue with setup wizard by clicking "Next".
                    """.formatted(restorePath.toString()));
            MemoryBuffer memoryBuffer = new MemoryBuffer();
            Upload upload = new Upload(memoryBuffer);
            upload.setAcceptedFileTypes("application/json", ".json");
            upload.addSucceededListener((e) -> {
                InputStream is = memoryBuffer.getInputStream();
                setupController.restore(is);
            });
            upload.addFileRejectedListener(event -> {
                String errorMessage = event.getErrorMessage();
                logger.error(errorMessage);
                ShowNotification.error("Cannot upload backup", errorMessage);
            });

            restoreButton = upload;
        }

        layout.add(
                welcome,
                restoreFoundText,
                restoreButton
        );

        return layout;
    }
}

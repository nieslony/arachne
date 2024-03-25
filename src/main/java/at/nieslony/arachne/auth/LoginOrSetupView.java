/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.auth;

import at.nieslony.arachne.kerberos.KerberosSettings;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.setup.SetupController;
import at.nieslony.arachne.setup.SetupView;
import at.nieslony.arachne.utils.FolderFactory;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route("login")
@AnonymousAllowed
public class LoginOrSetupView
        extends VerticalLayout
        implements HasDynamicTitle, BeforeEnterObserver {

    private static final org.slf4j.Logger logger
            = LoggerFactory.getLogger(LoginOrSetupView.class);

    private final SetupController setupController;
    private final FolderFactory folderFactory;
    private final Settings settings;

    private String title;
    private LoginOverlay login = null;

    public LoginOrSetupView(
            SetupController setupController,
            FolderFactory folderFactory,
            Settings settings
    ) {
        this.setupController = setupController;
        this.folderFactory = folderFactory;
        this.settings = settings;
    }

    @Override
    public String getPageTitle() {
        return title;
    }

    private void createLogin() {
        logger.info("Create login page");
        login = new LoginOverlay();
        removeAll();
        KerberosSettings kerberosSettings = settings.getSettings(KerberosSettings.class);

        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        login.setAction("login");
        login.setTitle("Arachne");
        login.setDescription("Administer your openVPN");
        login.setForgotPasswordButtonVisible(false);
        if (kerberosSettings.isEnableKrbAuth()) {
            Button toSSoButton = new Button(
                    "Login with SSO",
                    e -> {
                        login.setOpened(false);
                        UI.getCurrent().getPage().setLocation("/arachne/sso");
                    }
            );
            toSSoButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            toSSoButton.setWidthFull();
            login.getFooter().add(toSSoButton);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (setupController.setupAlreadyDone()) {
            title = "Login | Arachne";
            if (login == null) {
                createLogin();
            }
            login.setOpened(true);

            if (beforeEnterEvent.getLocation()
                    .getQueryParameters()
                    .getParameters()
                    .containsKey("error")) {
                login.setError(true);
            }
            add(login);
        } else {
            logger.info("Show SetupView");
            title = "Setup | Arachne";
            add(new SetupView(setupController, folderFactory));
        }
    }
}

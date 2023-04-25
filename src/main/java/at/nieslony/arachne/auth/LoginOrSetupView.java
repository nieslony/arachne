/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.auth;

import at.nieslony.arachne.setup.SetupView;
import at.nieslony.arachne.setup.SetupController;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Route("login")
public class LoginOrSetupView
        extends VerticalLayout
        implements HasDynamicTitle, BeforeEnterObserver {

    private static final org.slf4j.Logger logger
            = LoggerFactory.getLogger(LoginOrSetupView.class);

    private SetupController setupController;

    private String title;
    boolean loginViewInit;
    private final LoginForm login = new LoginForm();

    ;

    public LoginOrSetupView(SetupController setupController) {
        loginViewInit = false;
        this.setupController = setupController;
    }

    @Override
    public String getPageTitle() {
        return title;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (setupController.setupAlreadyDone()) {
            logger.info("Create login page");
            title = "Login | Arachne";
            removeAll();

            addClassName("login-view");
            setSizeFull();
            setAlignItems(Alignment.CENTER);
            setJustifyContentMode(JustifyContentMode.CENTER);
            login.setAction("login");
            login.setForgotPasswordButtonVisible(false);
            add(new H1("Arachne"), login);

            if (beforeEnterEvent.getLocation()
                    .getQueryParameters()
                    .getParameters()
                    .containsKey("error")) {
                login.setError(true);
            }
        } else {
            logger.info("Show SetupView");
            title = "Setup | Arachne";
            add(new SetupView(setupController));
        }

    }
}

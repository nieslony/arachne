/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 *
 * @author claas
 */
@StyleSheet("/frontend/styles/styles.css")
public class ViewTemplate extends AppLayout {

    private final transient AuthenticationContext authContext;

    public ViewTemplate(AuthenticationContext authContext) {
        this.authContext = authContext;

        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Arachne");
        //logo.addClassNames("text-m", "m-m");
        logo.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");
        //logo.addClassName("logo");
        Button logout = new Button("Logout", click -> this.authContext.logout());

        HorizontalLayout header = new HorizontalLayout(
                new DrawerToggle(),
                logo,
                logout
        );
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidth("100%");
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);
    }

    private void createDrawer() {
        RouterLink mainLink = new RouterLink("Home", MainView.class);
        RouterLink usersLink = new RouterLink("Users", UsersView.class);
        RouterLink rolesView = new RouterLink("Roles", RolesView.class);

        addToDrawer(new VerticalLayout(
                mainLink,
                usersLink,
                rolesView)
        );
    }
}

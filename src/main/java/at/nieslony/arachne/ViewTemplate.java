/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.firewall.FirewallView;
import at.nieslony.arachne.kerberos.KerberosView;
import at.nieslony.arachne.ldap.LdapView;
import at.nieslony.arachne.users.ArachneUserDetails;
import at.nieslony.arachne.users.ChangePasswordDialog;
import at.nieslony.arachne.users.UserRepository;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 *
 * @author claas
 */
@StyleSheet("/frontend/styles/styles.css")
public class ViewTemplate extends AppLayout {

    private static final Logger logger = LoggerFactory.getLogger(ViewTemplate.class);

    private final transient AuthenticationContext authContext;
    private final UserRepository userRepository;

    public ViewTemplate(
            UserRepository userRepositoty,
            AuthenticationContext authContext) {
        this.authContext = authContext;
        this.userRepository = userRepositoty;

        createHeader();
        createDrawer();
    }

    private void createHeader() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();
        String userInfo;

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (userDetails instanceof ArachneUserDetails aud) {
            userInfo = "%s (%s)".formatted(aud.getDisplayName(), username);
        } else {
            userInfo = username;
        }

        H1 logo = new H1("Arachne");
        logo.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
        MenuItem item = menuBar.addItem(userInfo);
        SubMenu userMenu = item.getSubMenu();
        userMenu.addItem("Logout", click -> {
            VaadinSession.getCurrent().close();
            this.authContext.logout();
        });
        userMenu.addItem("Change Password...", click -> changePassword());

        HorizontalLayout header = new HorizontalLayout(
                new DrawerToggle(),
                logo,
                menuBar
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
        RouterLink rolesLink = new RouterLink("Roles", RolesView.class);
        RouterLink openVpnUsersLink = new RouterLink("OpenVPN User", OpenVpnUserView.class);
        RouterLink ldapSettingsLink = new RouterLink("LDAP Settings", LdapView.class);
        RouterLink kerberosSettingsLink = new RouterLink("Kerberos Settings", KerberosView.class);
        RouterLink tomcatSettingsLink = new RouterLink("Tomcat", TomcatView.class);
        RouterLink firewllLink = new RouterLink("Firewall", FirewallView.class);

        addToDrawer(new VerticalLayout(
                mainLink,
                usersLink,
                rolesLink,
                openVpnUsersLink,
                ldapSettingsLink,
                kerberosSettingsLink,
                tomcatSettingsLink,
                firewllLink
        ));
    }

    void changePassword() {
        ChangePasswordDialog dlg = new ChangePasswordDialog(userRepository);
        dlg.open();
    }
}

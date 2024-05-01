/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.apiindex.ApiIndexView;
import at.nieslony.arachne.auth.ExternalAuthView;
import at.nieslony.arachne.firewall.FirewallView;
import at.nieslony.arachne.ldap.LdapView;
import at.nieslony.arachne.mail.MailSettingsView;
import at.nieslony.arachne.openvpn.OpenVpnSiteView;
import at.nieslony.arachne.openvpn.OpenVpnUserView;
import at.nieslony.arachne.pki.CertSpecsView;
import at.nieslony.arachne.pki.CertificatesView;
import at.nieslony.arachne.pki.PkiSettingsView;
import at.nieslony.arachne.roles.RolesView;
import at.nieslony.arachne.tasks.RecurringTasksView;
import at.nieslony.arachne.tasks.TaskView;
import at.nieslony.arachne.tomcat.TomcatView;
import at.nieslony.arachne.users.ArachneUserDetails;
import at.nieslony.arachne.users.ChangePasswordDialog;
import at.nieslony.arachne.users.UserRepository;
import at.nieslony.arachne.users.UsersView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouteConfiguration;
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
public class ViewTemplate extends AppLayout implements HasDynamicTitle {

    private static final Logger logger = LoggerFactory.getLogger(ViewTemplate.class);

    private final transient AuthenticationContext authContext;
    private final UserRepository userRepository;
    private String pageTitleStr = null;

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

        H1 pageTitle = new H1("Arachne");
        pageTitle.getStyle()
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
        if (userRepository.findByUsername(username).getExternalProvider() == null) {
            userMenu.addItem("Change Password...", click -> changePassword());
        }
        HorizontalLayout header = new HorizontalLayout(
                new DrawerToggle(),
                pageTitle,
                menuBar
        );
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(pageTitle);
        header.setWidth("100%");
        header.setSpacing(false);
        header.setPadding(false);
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav homeNav = new SideNav();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (userDetails
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"))) {
            homeNav.addItem(
                    new SideNavItem("Admin Home", AdminHome.class,
                            VaadinIcon.HOME.create())
            );
            homeNav.addItem(
                    new SideNavItem("User Home", UserHome.class,
                            VaadinIcon.HOME.create())
            );
        } else {
            homeNav.addItem(
                    new SideNavItem("Home", AdminHome.class,
                            VaadinIcon.HOME.create())
            );
        }

        homeNav.setWidthFull();

        SideNav usersNav = new SideNav();
        usersNav.setLabel("Users & Authentication");
        usersNav.addItem(new SideNavItem("Users", UsersView.class,
                VaadinIcon.USERS.create()),
                new SideNavItem("LDAP User Source", LdapView.class,
                        VaadinIcon.FOLDER.create()),
                new SideNavItem("Roles", RolesView.class,
                        VaadinIcon.GROUP.create()),
                new SideNavItem("External Auth", ExternalAuthView.class,
                        VaadinIcon.AUTOMATION.create())
        );
        usersNav.setWidthFull();

        SideNav networkNav = new SideNav();
        networkNav.setLabel("VPN");
        networkNav.addItem(
                new SideNavItem("OpenVPN User", OpenVpnUserView.class,
                        VaadinIcon.CONTROLLER.create()),
                new SideNavItem("OpenVPN Site 2 Site", OpenVpnSiteView.class,
                        VaadinIcon.SERVER.create()),
                new SideNavItem("Firewall", FirewallView.class,
                        VaadinIcon.FIRE.create())
        );
        networkNav.setWidthFull();

        SideNav certificatesNav = new SideNav("Certificates");
        certificatesNav.addItem(
                new SideNavItem("All Certificates", CertificatesView.class,
                        VaadinIcon.DIPLOMA.create()
                ),
                new SideNavItem(
                        "User Cert Specs",
                        RouteConfiguration
                                .forSessionScope()
                                .getUrl(CertSpecsView.class, "user-spec"),
                        VaadinIcon.USER.create()
                ),
                new SideNavItem(
                        "Server Cert Specs",
                        RouteConfiguration
                                .forSessionScope()
                                .getUrl(CertSpecsView.class, "server-spec"),
                        VaadinIcon.CLOUD.create()
                ),
                new SideNavItem(
                        "Pki Settings",
                        PkiSettingsView.class,
                        VaadinIcon.KEY.create()
                )
        );

        SideNav servicesNav = new SideNav("Services");
        servicesNav.addItem(
                new SideNavItem("Mail Settings", MailSettingsView.class,
                        VaadinIcon.MAILBOX.create()),
                new SideNavItem("Integrated Tomcat", TomcatView.class,
                        VaadinIcon.CONNECT.create()),
                new SideNavItem("Tasks", TaskView.class,
                        VaadinIcon.AUTOMATION.create()),
                new SideNavItem("Recurring Tasks", RecurringTasksView.class,
                        VaadinIcon.CLOCK.create()),
                new SideNavItem("API Index", ApiIndexView.class,
                        VaadinIcon.LIST.create())
        );

        servicesNav.setWidthFull();

        VerticalLayout layout = new VerticalLayout(
                homeNav,
                usersNav,
                networkNav,
                certificatesNav,
                servicesNav
        );
        layout.setSpacing(true);
        layout.setSizeUndefined();

        addToDrawer(layout);
    }

    void changePassword() {
        ChangePasswordDialog dlg = new ChangePasswordDialog(userRepository);
        dlg.open();
    }

    @Override
    public void setContent(Component content) {
        PageTitle title = content.getClass().getAnnotation(PageTitle.class);
        if (title != null) {
            pageTitleStr = title.value();
        } else if (content instanceof HasDynamicTitle hdt) {
            pageTitleStr = hdt.getPageTitle();
        }

        if (pageTitleStr != null) {
            VerticalLayout layout = new VerticalLayout(
                    new H2(pageTitleStr),
                    content
            );
            super.setContent(layout);
        } else {
            super.setContent(content);
        }
    }

    @Override
    public String getPageTitle() {
        if (pageTitleStr != null) {
            return pageTitleStr + " | Arachne";
        } else {
            return "Arachne";
        }
    }
}

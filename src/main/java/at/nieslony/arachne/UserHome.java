/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.openvpn.OpenVpnController;
import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.pki.PkiException;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.settings.SettingsException;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import jakarta.annotation.security.RolesAllowed;
import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.olli.ClipboardHelper;

/**
 *
 * @author claas
 */
@Route(value = "user-home")
@PageTitle("Arachne | User Home")
@RolesAllowed("USER")
@Slf4j
public class UserHome extends VerticalLayout implements RouterLayout {

    private final OpenVpnController openVpnRestController;
    private final OpenVpnUserSettings openVpnUserSettings;
    private final int ICON_SIZE_SMALL = 32;
    private final int ICON_SIZE_LARGE = 96;

    public UserHome(
            OpenVpnController openVpnRestController,
            Settings settings
    ) {
        this.openVpnRestController = openVpnRestController;
        this.openVpnUserSettings = settings.getSettings(OpenVpnUserSettings.class);

        VerticalLayout content = new VerticalLayout();
        Tabs tabs = new Tabs();
        Tab ovpnTab = new Tab(createOvpnTitle());
        Tab networkManagerTab = new Tab(createNetworkManagerTitle());
        var ovpnPage = new Div(createOvpnPage());
        var networkManagerPage = new Div(createNetworkManagerPage());
        tabs.add(ovpnTab, networkManagerTab);
        tabs.addSelectedChangeListener((event) -> {
            content.removeAll();
            Tab tab = event.getSelectedTab();
            if (tab.equals(ovpnTab)) {
                content.add(ovpnPage);
            } else if (tab.equals(networkManagerTab)) {
                content.add(networkManagerPage);
            }
        });

        if (VaadinSession.getCurrent().getBrowser().isLinux()) {
            tabs.setSelectedTab(networkManagerTab);
        } else {
            tabs.setSelectedTab(ovpnTab);
        }

        VerticalLayout tabsLayout = new VerticalLayout(tabs, content);
        tabsLayout.setMargin(false);
        tabsLayout.setPadding(false);
        add(
                new H1("Arachne - User Home"),
                createNavigateToAdminHome(),
                new H2("Download your Client Configuration"),
                tabsLayout
        );
    }

    private Component createNavigateToAdminHome() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (userDetails
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return new Div(
                    new Text("Go to "),
                    new RouterLink("Admin Home", AdminHome.class)
            );
        } else {
            return new Text("");
        }
    }

    private HorizontalLayout createOvpnTitle() {
        HorizontalLayout os = new HorizontalLayout(
                loadIcon("Windows.png", ICON_SIZE_SMALL),
                new Text("Windows, "),
                loadIcon("Linux.png", ICON_SIZE_SMALL),
                new Text("Linux")
        );
        os.setSpacing(false);
        os.setAlignItems(Alignment.CENTER);
        HorizontalLayout layout = new HorizontalLayout(
                loadIcon("OpenVPN.png", ICON_SIZE_LARGE),
                new Div(
                        new H3("OpenVPN Configuration"),
                        new Paragraph(os)
                )
        );
        layout.setAlignItems(Alignment.CENTER);

        return layout;
    }

    private Component createOvpnPage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        var dlh = DownloadHandler.fromInputStream((de) -> {
            try {
                String config = openVpnRestController
                        .openVpnUserConfig(username);
                var is = new ByteArrayInputStream(config.getBytes());
                return new DownloadResponse(
                        is,
                        openVpnUserSettings.getClientConfigName(),
                        "application/x-openvpn-profile",
                        config.getBytes().length
                );
            } catch (PkiException | SettingsException e) {
                return DownloadResponse.error(500);
            }
        });
        var anchor = new Anchor(dlh, "openVPN configuration");

        return new HorizontalLayout(
                new Text("Download"), anchor
        );
    }

    private HorizontalLayout createNetworkManagerTitle() {
        HorizontalLayout os = new HorizontalLayout(
                loadIcon("Linux.png", ICON_SIZE_SMALL),
                new Text("Linux only")
        );
        os.setSpacing(false);
        os.setAlignItems(Alignment.CENTER);
        HorizontalLayout layout = new HorizontalLayout();
        layout.add(
                loadIcon("NetworkManager.png", ICON_SIZE_LARGE),
                new Div(
                        new H3("NetworkManager Configuration"),
                        new Paragraph(os)
                )
        );
        layout.setAlignItems(Alignment.CENTER);

        return layout;
    }

    private Component createNetworkManagerPage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        AtomicReference<String> nmInstructions = new AtomicReference<>();
        try {
            nmInstructions.set(openVpnRestController.openVpnUserConfigShell(username));
        } catch (PkiException | SettingsException ex) {
            return new Text("");
        }

        OrderedList steps = new OrderedList();
        steps.setType(OrderedList.NumberingType.NUMBER);
        steps.add(
                new ListItem(
                        """
                To add a new NetworkManager Connection open a terminal (xterm,
                konsole, …)
                """),
                new ListItem(new Details("execute the following commands",
                        new ClipboardHelper(
                                nmInstructions.get(),
                                new Button(
                                        "Copy to clipboard",
                                        VaadinIcon.COPY.create())
                        ),
                        new Pre(nmInstructions.get())
                )),
                new ListItem(
                        "you will find a new connection \"%s\""
                                .formatted(
                                        openVpnUserSettings.getFormattedClientConfigName(username)
                                )
                )
        );

        return steps;
    }

    private Image loadIcon(String iconName, int size) {
        String iconPath = "/icons/" + iconName;

        Image image = new Image(
                DownloadHandler.forClassResource(getClass(), iconPath),
                iconName
        );
        image.setWidth(size, Unit.PIXELS);
        image.setHeight(size, Unit.PIXELS);

        return image;
    }
}

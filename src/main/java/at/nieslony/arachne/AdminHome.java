/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import at.nieslony.arachne.openvpn.OpenVpnSiteSettings;
import at.nieslony.arachne.openvpn.OpenVpnUserSettings;
import at.nieslony.arachne.openvpn.VpnSiteRepository;
import at.nieslony.arachne.openvpn.management.ManagementException;
import at.nieslony.arachne.openvpn.management.OpenVpnManagement;
import at.nieslony.arachne.openvpn.management.OpenVpnManagementService;
import at.nieslony.arachne.openvpn.management.commands.Status;
import at.nieslony.arachne.settings.Settings;
import at.nieslony.arachne.utils.components.YesNoIcon;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author claas
 */
@Route(value = "admin-home", layout = ViewTemplate.class)
@PageTitle("Admin Dashboard")
@RolesAllowed("ADMIN")
@Slf4j
public class AdminHome
        extends VerticalLayout
        implements BeforeEnterObserver, BeforeLeaveObserver {

    @Autowired
    private OpenVpnManagementService openVpnManagementService;

    @Autowired
    VpnSiteRepository vpnSiteRepository;

    @Autowired
    Settings settings;

    private OpenVpnUserSettings openVpnUserSettings;
    private OpenVpnSiteSettings openVpnSiteSettings;

    private Select<Integer> userUpdateIntervalField;
    private Select<Integer> siteUpdateIntervalField;
    private Grid<Status.ConnectionStatus> connectedSitesGrid;
    private Grid<Status.ConnectionStatus> connectedUsersGrid;
    private Span msgConnectedUsers;
    private Span msgConnectedSites;

    private ScheduledExecutorService scheduledExecutor;
    private AtomicReference<ScheduledFuture<?>> updateUsers;
    private AtomicReference<ScheduledFuture<?>> updateSites;

    Text serverVersion;

    @PostConstruct
    public void init() {
        openVpnUserSettings = settings.getSettings(OpenVpnUserSettings.class);
        openVpnSiteSettings = settings.getSettings(OpenVpnSiteSettings.class);

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        updateUsers = new AtomicReference<>();
        updateSites = new AtomicReference<>();

        serverVersion = new Text("");

        Accordion content = new Accordion();
        content.add("Connected Users", createConnectedUsersView());
        content.add("Connected Sites", createConnectedSitesView());
        content.setWidthFull();
        add(
                serverVersion,
                content
        );
        content.setSizeFull();

        setPadding(false);

        onUpdateServerVersion();
    }

    private void onUpdateServerVersion() {
        String versionStr;
        try {
            versionStr = openVpnManagementService
                    .getUserManagement()
                    .version()
                    .longVersion();
        } catch (ManagementException ex) {
            log.error("Cannot connect to management interface: " + ex.getMessage());
            versionStr = "unknown version";
        }

        serverVersion.setText("Running " + versionStr);
    }

    private void onRefreshConnectedUsers() {
        List<Status.ConnectionStatus> items = new LinkedList<>();
        StringBuilder msg = new StringBuilder();

        if (openVpnUserSettings.isAlreadyConfigured()) {
            OpenVpnManagement mgmt = openVpnManagementService.getSiteManagement();
            switch (mgmt.getManagementConnectionStatus()) {
                case Disconnected -> {
                    msg.append("No connection to Management Interface");
                }
                case Hold -> {
                    msg.append("OpenVpn is in Hold Status");
                }
                case Connected -> {
                    try {
                        var status = openVpnManagementService.getUserManagement().status();
                        log.debug("User onnection status: " + status.connectionStatus().toString());
                        items.addAll(status.connectionStatus());
                        msg.append("%d Users connected".formatted(
                                status.connectionStatus().size()
                        ));
                    } catch (ManagementException ex) {
                        log.error("Error getting connected users: " + ex.getMessage());
                        msg
                                .append("Cannot get connected users: ")
                                .append(ex.getMessage());
                    }
                }
            }
        } else {
            msgConnectedUsers.setText("User VPN not yet configured");
        }
        log.debug("Updating UI");
        connectedUsersGrid.getUI().ifPresent(ui -> ui.access(() -> {
            connectedUsersGrid.setItems(items);
        }));
        msgConnectedUsers.getUI().ifPresent(ui -> ui.access(() -> {
            msgConnectedUsers.setText(msg.toString());
        }));
        log.debug("List of Users refreshed.");
    }

    private void onRefreshConnectedSites() {
        List<Status.ConnectionStatus> items = new LinkedList<>();
        StringBuilder msg = new StringBuilder();

        if (openVpnSiteSettings.isAlreadyConfigured()) {
            OpenVpnManagement mgmt = openVpnManagementService.getSiteManagement();
            switch (mgmt.getManagementConnectionStatus()) {
                case Disconnected -> {
                    msg.append("No connection to Management Interface");
                }
                case Hold -> {
                    msg.append("OpenVpn is in Hold Status");
                }
                case Connected -> {
                    try {
                        log.info("Gettings all configured sites");
                        var status = mgmt.status();
                        msg.append("%d of %d sites connected".formatted(
                                status.connectionStatus().size(),
                                vpnSiteRepository.count() - 1
                        ));
                        Set<String> siteNames = new HashSet<>();
                        status.connectionStatus().forEach(
                                site -> siteNames.add(site.username())
                        );
                        vpnSiteRepository.findAll()
                                .forEach(site -> {
                                    String siteName = site.getSiteHostname();
                                    if (!siteNames.contains(siteName)
                                            && !site.isDefaultSite()) {
                                        log.debug("Site %s is not connected".formatted(siteName));
                                        status
                                                .connectionStatus()
                                                .add(
                                                        Status.ConnectionStatus
                                                                .notConnected(siteName)
                                                );
                                    }
                                });
                        log.debug("Connextion status: " + status.connectionStatus().toString());
                        items.addAll(
                                status
                                        .connectionStatus()
                                        .stream()
                                        .sorted(
                                                (t1, t2) -> t1.username()
                                                        .compareTo(t2.username())
                                        )
                                        .toList()
                        );
                    } catch (ManagementException ex) {
                        log.error("Error getting connected sites: " + ex.getMessage());
                        msg.append("Error: ").append(ex.getMessage());
                    }
                }
            }
        } else {
            msgConnectedSites.setText("Site VPN not yet configured");
        }
        connectedSitesGrid.getUI().ifPresent(ui -> ui.access(() -> {
            connectedSitesGrid.setItems(items);
        }));
        msgConnectedSites.getUI().ifPresent(ui -> ui.access(() -> {
            msgConnectedSites.setText(msg.toString());
        }));
        log.info("Gettings all configured sites (Done)");
    }

    private void scheduleTimer(
            Select<Integer> select,
            Runnable func,
            AtomicReference<ScheduledFuture<?>> scheduledFuture) {
        Integer seconds = select.getValue();
        if (scheduledFuture.get() != null && !scheduledFuture.get().isCancelled()) {
            scheduledFuture.get().cancel(false);
            scheduledFuture.set(null);
        }
        if (seconds > 0) {
            scheduledFuture.set(
                    scheduledExecutor.scheduleWithFixedDelay(
                            () -> {
                                log.debug("Starting task");
                                func.run();
                                log.debug("Task done,");
                            },
                            seconds,
                            seconds,
                            TimeUnit.SECONDS
                    )
            );
        }
    }

    private Select<Integer> createUpdateIntervalSelect(
            Runnable func,
            AtomicReference<ScheduledFuture<?>> scheduledFuture) {
        Select<Integer> select = new Select<>("Auto Refresh Interval");
        select.setItems(
                -1,
                15, 30,
                60, 2 * 60, 5 * 60, 10 * 10
        );
        select.setItemLabelGenerator(interval -> {
            if (interval < 0) {
                return "Never";
            }
            if (interval <= 60) {
                return "%d seconds".formatted(interval);
            }
            return "%d minutes".formatted(interval / 60);
        });
        select.setValue(15);
        select.addValueChangeListener(v -> {
            scheduleTimer(select, func, scheduledFuture);
        });

        return select;
    }

    private Component createConnectedUsersView() {
        Button refreshButton = new Button("Refresh", (e) -> onRefreshConnectedUsers());
        msgConnectedUsers = new Span("");
        userUpdateIntervalField = createUpdateIntervalSelect(
                this::onRefreshConnectedUsers,
                updateUsers
        );

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.addToStart(
                refreshButton,
                msgConnectedUsers
        );
        headerLayout.addToEnd(userUpdateIntervalField);
        headerLayout.setPadding(false);
        headerLayout.setMargin(false);
        headerLayout.setAlignItems(Alignment.BASELINE);
        headerLayout.setWidthFull();

        connectedUsersGrid = new Grid<>();
        connectedUsersGrid.addColumn(Status.ConnectionStatus::username)
                .setSortable(true)
                .setHeader("Common Name");
        connectedUsersGrid.addColumn(
                source -> DecimalFormat
                        .getInstance()
                        .format(source.byteReceived()))
                .setSortable(true)
                .setTextAlign(ColumnTextAlign.END)
                .setHeader("Bytes Received");
        connectedUsersGrid.addColumn(
                source -> DecimalFormat
                        .getInstance()
                        .format(source.bytesSent()))
                .setSortable(true)
                .setTextAlign(ColumnTextAlign.END)
                .setHeader("Bytes Sent");
        connectedUsersGrid.addColumn(
                source -> DateFormat
                        .getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                        .format(source.connectedSince()))
                .setSortable(true)
                .setHeader("Connected since");
        connectedUsersGrid.addColumn(source -> source.realAddress().getHostAddress())
                .setSortable(true)
                .setHeader("Real Address");
        connectedUsersGrid.addColumn(source -> source.virtualAddress().getHostAddress())
                .setSortable(true)
                .setHeader("Virtual Address");

        VerticalLayout layout = new VerticalLayout();
        layout.add(
                headerLayout,
                connectedUsersGrid
        );
        layout.setPadding(false);
        layout.setSizeFull();

        return layout;
    }

    private Component createConnectedSitesView() {
        Button refreshButton = new Button("Refresh", (e) -> onRefreshConnectedSites());
        msgConnectedSites = new Span("");
        siteUpdateIntervalField = createUpdateIntervalSelect(
                this::onRefreshConnectedSites,
                updateSites
        );

        HorizontalLayout headerLayout = new HorizontalLayout(
                refreshButton,
                msgConnectedSites
        );
        headerLayout.addToEnd(siteUpdateIntervalField);

        headerLayout.setPadding(false);
        headerLayout.setMargin(false);
        headerLayout.setAlignItems(Alignment.BASELINE);
        headerLayout.setWidthFull();

        connectedSitesGrid = new Grid<>();
        connectedSitesGrid.addColumn(Status.ConnectionStatus::username)
                .setHeader("Site Name");
        connectedSitesGrid.addColumn(new ComponentRenderer<>(
                (var site) -> {
                    YesNoIcon icon = new YesNoIcon();
                    icon.setValue(site.connectedSince() != null);
                    return icon;
                }))
                .setHeader("Connected")
                .setAutoWidth(true)
                .setFlexGrow(0);
        connectedSitesGrid.addColumn(
                site -> site.connectedSince() != null
                ? DecimalFormat
                        .getInstance()
                        .format(site.byteReceived())
                : "")
                .setHeader("Bytes Received");
        connectedSitesGrid.addColumn(
                site -> site.connectedSince() != null
                ? DecimalFormat
                        .getInstance()
                        .format(site.bytesSent())
                : "")
                .setHeader("Bytes Sent");
        connectedSitesGrid.addColumn(
                site -> site.connectedSince() != null
                ? DateFormat
                        .getDateTimeInstance(
                                DateFormat.SHORT,
                                DateFormat.MEDIUM
                        )
                        .format(site.connectedSince()
                        )
                : "")
                .setHeader("Connected since");
        connectedSitesGrid.addColumn(
                site -> site.connectedSince() != null
                ? site.realAddress().getHostAddress()
                : "")
                .setHeader("Real Address");
        connectedSitesGrid.addColumn(
                site -> site.connectedSince() != null
                ? site.virtualAddress().getHostAddress()
                : "")
                .setHeader("Virtual Address");

        VerticalLayout layout = new VerticalLayout();
        layout.add(
                headerLayout,
                connectedSitesGrid
        );
        layout.setPadding(false);
        layout.setSizeFull();

        return layout;
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        log.info("About to leave, removing status change listener");
        if (updateUsers.get() != null) {
            updateUsers.get().cancel(false);
        }
        if (updateSites.get() != null) {
            updateSites.get().cancel(false);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent bee) {
        onRefreshConnectedUsers();
        onRefreshConnectedSites();

        log.info("Scheduling timers");
        scheduleTimer(userUpdateIntervalField, this::onRefreshConnectedUsers, updateUsers);
        scheduleTimer(siteUpdateIntervalField, this::onRefreshConnectedSites, updateSites);
    }
}

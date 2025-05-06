/*
 * Copyright (C) 2023 claas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.nieslony.arachne.tasks;

import at.nieslony.arachne.ViewTemplate;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.CastUtils;

/**
 *
 * @author claas
 */
@Route(value = "tasks", layout = ViewTemplate.class)
@PageTitle("All Tasks")
@RolesAllowed("ADMIN")
@Log4j2
public class TaskView
        extends VerticalLayout
        implements BeforeEnterObserver, BeforeLeaveObserver {

    Grid<TaskModel> tasksGrid;
    ComboBox<Integer> refreshInterval;
    Timer timer;

    public TaskView(TaskRepository taskRepository, TaskScheduler taskScheduler) {
        timer = new Timer();
        tasksGrid = new Grid<>();

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();
        buttonBar.setMargin(false);
        buttonBar.setPadding(false);

        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_DROPDOWN_INDICATORS);
        MenuItem taskTypeItem = menuBar.addItem("Create Task");
        SubMenu taskTypeMenu = taskTypeItem.getSubMenu();
        taskScheduler.getTaskTypes().forEach((task) -> {
            taskTypeMenu.addItem(getTaskName(task), (e) -> {
                taskScheduler.runTask(task,
                        () -> tasksGrid.getDataProvider().refreshAll(),
                        () -> tasksGrid.getDataProvider().refreshAll()
                );
                tasksGrid.getDataProvider().refreshAll();
                scheduleRefresh();
            });
        });
        buttonBar.addToStart(menuBar);

        Button autoRefresh = new Button("Autorefresh");
        autoRefresh.addClassNames(
                LumoUtility.Background.PRIMARY,
                LumoUtility.TextColor.PRIMARY_CONTRAST
        );
        buttonBar.addToEnd(autoRefresh);

        refreshInterval = new ComboBox<>();
        refreshInterval.setItems(1, 2, 5, 10, 20);
        refreshInterval.setItemLabelGenerator((l) -> l.toString() + " min");
        refreshInterval.addValueChangeListener((e) -> scheduleRefresh());
        refreshInterval.setValue(1);
        buttonBar.addToEnd(refreshInterval);

        tasksGrid
                .addColumn((source) -> {
                    try {
                        Class c = Class.forName(source.getTaskClassName());
                        return getTaskName(CastUtils.cast(c));
                    } catch (ClassNotFoundException ex) {
                        return "Unknown Class: " + source.getTaskClassName();
                    }
                })
                .setHeader("Name");
        tasksGrid
                .addColumn((source) -> {
                    if (source.getScheduled() == null) {
                        return "";
                    } else {
                        return DateFormat
                                .getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                                .format(source.getScheduled());
                    }
                })
                .setHeader("Scheduled");
        tasksGrid
                .addColumn((source) -> {
                    if (source.getStarted() == null) {
                        return "";
                    } else {
                        return DateFormat
                                .getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                                .format(source.getStarted());
                    }
                })
                .setHeader("Started");
        tasksGrid
                .addColumn((source) -> {
                    if (source.getStopped() == null) {
                        return "";
                    } else {
                        return DateFormat
                                .getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                                .format(source.getStopped());
                    }
                })
                .setHeader("Stopped");
        tasksGrid
                .addColumn((source) -> source.getStatus())
                .setTooltipGenerator((source) -> {
                    if (source.getStatusMsg() == null) {
                        return null;
                    }
                    if (source.getStatusMsg().isEmpty()) {
                        return null;
                    }
                    return source.getStatusMsg();
                })
                .setHeader("Status");
        tasksGrid.addComponentColumn((source) -> {
            if (source.getStatus() == TaskModel.Status.SCHEDULED) {
                return new Button("Reschedule...", (t) -> {
                    Dialog dlg = createRescheduleDialog(source, () -> {
                        taskRepository.save(source);
                        tasksGrid.getDataProvider().refreshItem(source);
                        taskScheduler.scheduleTask(source);
                    });
                    dlg.open();
                });
            } else {
                return new Text("");
            }
        });
        DataProvider<TaskModel, Void> dataProvider = DataProvider.fromCallbacks(
                (query) -> {
                    Pageable pageable = PageRequest.of(
                            query.getOffset(),
                            query.getLimit()
                    );
                    var page = taskRepository.findAll(pageable);
                    return page
                            .stream()
                            .sorted((t1, t2) -> -TaskModel.compare(t1, t2));
                },
                (query) -> (int) taskRepository.count()
        );
        tasksGrid.setDataProvider(dataProvider);
        tasksGrid.setHeightFull();

        add(
                buttonBar,
                tasksGrid
        );
        setPadding(false);

        autoRefresh.addClickListener(e -> {
            if (refreshInterval.isEnabled()) {
                refreshInterval.setEnabled(false);
                autoRefresh.removeClassNames(
                        LumoUtility.Background.PRIMARY,
                        LumoUtility.TextColor.PRIMARY_CONTRAST
                );
            } else {
                refreshInterval.setEnabled(true);
                autoRefresh.addClassNames(
                        LumoUtility.Background.PRIMARY,
                        LumoUtility.TextColor.PRIMARY_CONTRAST
                );
            }
            scheduleRefresh();
        });
    }

    private String getTaskName(Class<? extends Task> c) {
        if (c.isAnnotationPresent(TaskDescription.class)) {
            TaskDescription descr = c.getAnnotation(TaskDescription.class);
            return descr.name();
        } else {
            return c.getSimpleName();
        }
    }

    private Dialog createRescheduleDialog(TaskModel taskModel, Runnable onOk) {
        Dialog dlg = new Dialog();
        try {
            Class<? extends Task> taskClass = taskModel.getTaskClass();
            dlg.setHeaderTitle(
                    "Reschedule Task \"%s\""
                            .formatted(getTaskName(taskClass))
            );
        } catch (ClassNotFoundException ex) {
            dlg.setHeaderTitle("Reschedule invalid task class");
        }

        DatePicker datePicker = new DatePicker("Date");
        LocalDate scheduledDate = taskModel
                .getScheduled()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalTime scheduledTime = taskModel
                .getScheduled()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalTime();
        datePicker.setValue(scheduledDate);

        TimePicker timePicker = new TimePicker("Time");
        timePicker.setValue(scheduledTime);

        Button nowButton = new Button("Now", (e) -> {
            datePicker.setValue(LocalDate.now());
            timePicker.setValue(LocalTime.now());
        });

        HorizontalLayout layout = new HorizontalLayout(
                datePicker,
                timePicker,
                nowButton
        );
        layout.setAlignItems(Alignment.BASELINE);
        dlg.add(layout);

        Button okButton = new Button("OK", (t) -> {
            dlg.close();
            LocalDate newDate = datePicker.getValue();
            LocalTime newTime = timePicker.getValue();
            LocalDateTime ldt = LocalDateTime.of(newDate, newTime);
            Date date = Date.from(
                    ldt
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
            );
            taskModel.setScheduled(date);
            onOk.run();
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", (e) -> {
            dlg.close();
        });

        dlg.getFooter().add(
                cancelButton,
                okButton
        );
        return dlg;
    }

    private void scheduleRefresh() {
        try {
            timer.cancel();
        } catch (IllegalStateException ex) {
        }
        if (refreshInterval.isEnabled()) {
            timer = new Timer();
            int delay = 1000;
            int interval = refreshInterval.getValue() * 1000 * 60;
            UI ui = UI.getCurrent();

            timer.scheduleAtFixedRate(
                    new TimerTask() {
                @Override
                public void run() {
                    ui.access(() -> {
                        tasksGrid.getDataProvider().refreshAll();
                    });
                }
            },
                    delay,
                    interval
            );
        }
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent ble) {
        try {
            if (timer != null) {
                timer.cancel();
            }
        } catch (IllegalStateException ex) {
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent bee) {
        if (timer != null) {
            scheduleRefresh();
        }
    }
}

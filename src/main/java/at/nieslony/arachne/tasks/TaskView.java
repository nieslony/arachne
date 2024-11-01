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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
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
public class TaskView extends VerticalLayout {

    public TaskView(TaskRepository taskRepository, TaskScheduler taskScheduler) {
        Grid<TaskModel> grid = new Grid<>();
        MenuBar createTaskMenu = new MenuBar();
        createTaskMenu.addThemeVariants(MenuBarVariant.LUMO_DROPDOWN_INDICATORS);
        MenuItem taskTypeItem = createTaskMenu.addItem("Create Task");
        SubMenu taskTypeMenu = taskTypeItem.getSubMenu();
        for (var task : taskScheduler.getTaskTypes()) {
            taskTypeMenu.addItem(getTaskName(task), (e) -> {
                taskScheduler.runTask(
                        task,
                        () -> grid.getDataProvider().refreshAll(),
                        () -> grid.getDataProvider().refreshAll()
                );
                grid.getDataProvider().refreshAll();
            });
        }

        grid
                .addColumn((source) -> {
                    try {
                        Class c = Class.forName(source.getTaskClassName());
                        return getTaskName(CastUtils.cast(c));
                    } catch (ClassNotFoundException ex) {
                        return "Unknown Class: " + source.getTaskClassName();
                    }
                })
                .setHeader("Name");
        grid
                .addColumn((source) -> {
                    if (source.getScheduled() == null) {
                        return "";
                    } else {
                        return source.getScheduled();
                    }
                })
                .setHeader("Scheduled");
        grid
                .addColumn((source) -> {
                    if (source.getStarted() == null) {
                        return "";
                    } else {
                        return source.getStarted();
                    }
                })
                .setHeader("Started");
        grid
                .addColumn((source) -> {
                    if (source.getStopped() == null) {
                        return "";
                    } else {
                        return source.getStopped();
                    }
                })
                .setHeader("Stopped");
        grid
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
        grid.addComponentColumn((source) -> {
            if (source.getStatus() == TaskModel.Status.SCHEDULED) {
                return new Button("Reschedule...", (t) -> {
                    Dialog dlg = createRescheduleDialog(source, () -> {
                        taskRepository.save(source);
                        grid.getDataProvider().refreshItem(source);
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
        grid.setDataProvider(dataProvider);

        add(
                createTaskMenu,
                grid
        );
        setPadding(false);
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
}

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
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Date;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 *
 * @author claas
 */
@Route(value = "tasks", layout = ViewTemplate.class)
@PageTitle("Tasks | Arachne")
@RolesAllowed("ADMIN")
public class TaskView extends VerticalLayout {

    public TaskView(TaskRepository taskRepository, TaskScheduler taskScheduler) {
        Grid<TaskModel> grid = new Grid<>();
        MenuBar createTaskMenu = new MenuBar();
        MenuItem taskTypeItem
                = createTaskMenu.addItem("Create Task");
        taskTypeItem.add(new Icon(VaadinIcon.CHEVRON_DOWN));
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
                        return getTaskName(c);
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
        DataProvider<TaskModel, Void> dataProvider = DataProvider.fromCallbacks(
                (query) -> {
                    Pageable pageable = PageRequest.of(
                            query.getOffset(),
                            query.getLimit()
                    );
                    var page = taskRepository.findAll(pageable);
                    return page
                            .stream()
                            .sorted((t1, t2) -> {
                                int ret = compareDate(t2.getStarted(), t1.getStarted());
                                if (ret != 0) {
                                    return ret;
                                }
                                return compareDate(t2.getScheduled(), t1.getScheduled());
                            });
                },
                (query) -> (int) taskRepository.count()
        );
        grid.setDataProvider(dataProvider);

        add(
                createTaskMenu,
                grid
        );
    }

    static private int compareDate(Date d1, Date d2) {
        if (d1 != null && d2 != null) {
            return d1.compareTo(d2);
        }
        if (d1 != null && d2 == null) {
            return 1;
        }
        if (d1 == null && d2 != null) {
            return -1;
        }
        return 0;
    }

    private String getTaskName(Class<? extends Task> c) {
        if (c.isAnnotationPresent(TaskDescription.class)) {
            TaskDescription descr = c.getAnnotation(TaskDescription.class);
            return descr.name();
        } else {
            return c.getSimpleName();
        }
    }
}

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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 *
 * @author claas
 */
@Route(value = "recurring_tasks", layout = ViewTemplate.class)
@PageTitle("Recurring Tasks | Arachne")
@RolesAllowed("ADMIN")
public class RecurringTasksView extends VerticalLayout {

    public RecurringTasksView(RecurringTasksRepository recurringTaskRepository) {
        Grid<RecurringTaskModel> grid = new Grid<>();
        grid
                .addColumn((source) -> {
                    try {
                        Class c = Class.forName(source.getClassName());
                        return getTaskName(c);
                    } catch (ClassNotFoundException ex) {
                        return "Unknown Class: " + source.getClassName();
                    }
                })
                .setHeader("Name");
        grid
                .addColumn((source) -> {
                    Integer interval = source.getRecurringInterval();
                    if (interval != null && interval != 0) {
                        return "%d %s"
                                .formatted(
                                        source.getRecurringInterval(),
                                        source.getTimeUnit().toString()
                                );
                    } else {
                        return "";
                    }
                })
                .setHeader("Recurring Interval");
        grid
                .addColumn((source) -> {
                    String startAt = source.getStartAt();
                    if (startAt != null && !startAt.isEmpty()) {
                        return startAt;
                    } else {
                        return "";
                    }
                })
                .setHeader("Start at");

        grid.setItems(recurringTaskRepository.findAll());

        add(grid);
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

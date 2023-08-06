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
@Route(value = "tasks", layout = ViewTemplate.class)
@PageTitle("Tasks | Arachne")
@RolesAllowed("ADMIN")
public class TaskView extends VerticalLayout {

    public TaskView(TaskRepository taskRepository) {
        Grid<TaskModel> grid = new Grid<>();

        grid
                .addColumn((task) -> {
                    try {
                        String className = task.getTaskClassName();
                        Class cls = getClass().getClassLoader().loadClass(
                                className
                        );
                        if (cls.isAnnotationPresent(TaskDescription.class)) {
                            TaskDescription desc
                                    = (TaskDescription) cls.getAnnotation(TaskDescription.class);
                            return desc.name();
                        } else {
                            return className;
                        }
                    } catch (ClassNotFoundException ex) {

                    }
                    return "";
                })
                .setHeader("Name");

        grid
                .addColumn((task) -> {
                    return String.valueOf(task.getDelay() + task.getDelayUnit().toString());
                })
                .setHeader("Startup Delay");

        grid
                .addColumn((task) -> {
                    return String.valueOf(task.getInterval() + task.getIntervalUnit().toString());
                })
                .setHeader("Interval");

        grid
                .addColumn(TaskModel::getEnabled)
                .setHeader("Is Enabled");

        add(grid);
    }
}

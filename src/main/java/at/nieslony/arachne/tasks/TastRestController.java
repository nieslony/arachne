/*
 * Copyright (C) 2024 claas
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

import at.nieslony.arachne.apiindex.ShowApiDetails;
import jakarta.annotation.security.RolesAllowed;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author claas
 */
@RestController
@RequestMapping("/api/task")
@Slf4j
public class TastRestController {

    @Autowired
    private TaskScheduler taskScheduler;

    @ShowApiDetails
    public class TaskDetails {

        @Getter
        private final String className;

        @Getter
        private final String name;

        @Getter
        private final String description;

        TaskDetails(Class<? extends Task> taskClass) {
            className = taskClass.getSimpleName();
            TaskDescription descr = taskClass.getAnnotation(TaskDescription.class);
            if (descr != null) {
                name = descr.name();
                description = descr.description();
            } else {
                name = null;
                description = null;
            }
        }
    }

    @GetMapping("/list")
    @RolesAllowed(value = {"ADMIN"})
    public List<String> getTasks() {
        List<String> tasks = new LinkedList<>();
        taskScheduler.getTaskTypes().forEach(t -> {
            tasks.add(t.getSimpleName());
        });

        return tasks;
    }

    @GetMapping("/details/{taskName}")
    @RolesAllowed(value = {"ADMIN"})
    public TaskDetails getDetails(@PathVariable String taskName) {
        return taskScheduler.getTaskTypes()
                .stream()
                .filter(t -> t.getSimpleName().equals(taskName))
                .map((cl) -> new TaskDetails(cl))
                .findFirst()
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Task Name not found")
                );
    }

    @PostMapping("/start_now/{taskName}")
    @ResponseStatus(HttpStatus.CREATED)
    @RolesAllowed(value = {"ADMIN"})
    public String startTaskNow(@PathVariable String taskName) {
        var taskClass = taskScheduler.getTaskTypes()
                .stream()
                .filter(t -> t.getSimpleName().equals(taskName))
                .findFirst()
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Task Name not found")
                );
        taskScheduler.runTask(
                taskClass,
                () -> {
                },
                () -> {
                }
        );

        return "Task %s started".formatted(taskClass.getSimpleName());
    }
}

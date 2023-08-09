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

import at.nieslony.arachne.tasks.scheduled.UpdateCrl;
import at.nieslony.arachne.tasks.scheduled.UpdateDhParams;
import at.nieslony.arachne.tasks.scheduled.UpdateServerCert;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
public class TaskScheduler implements BeanFactoryAware {

    private static final Logger logger = LoggerFactory.getLogger(TaskScheduler.class);

    private ThreadGroup threadGroup;
    private BeanFactory beanFactory;
    private int taskNr = 0;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RecurringTasksRepository recurringTaskrepository;

    @Getter
    private List<Class<? extends Task>> taskTypes;

    public TaskScheduler() {
        taskTypes = new LinkedList<>();
        threadGroup = new ThreadGroup("arachne-tasks");
    }

    @PostConstruct
    public void init() {
        taskTypes.add(UpdateServerCert.class);
        taskTypes.add(UpdateDhParams.class);
        taskTypes.add(UpdateCrl.class);

        for (TaskModel task : taskRepository.findByStatus(TaskModel.Status.RUNNING)) {
            task.setStatus(TaskModel.Status.ERROR);
            task.setStatusMsg("Killed during Server Termination");
            taskRepository.save(task);
        }

        for (var task : taskTypes) {
            String className = task.getName();
            RecurringTaskModel model
                    = recurringTaskrepository.findByClassName(className);
            if (model == null) {
                model = new RecurringTaskModel();
                model.setClassName(className);
                RecurringTaskDescription descr
                        = task.getAnnotation(RecurringTaskDescription.class);
                if (descr != null) {
                    model.setRecurringInterval(descr.defaulnterval());
                    model.setTimeUnit(descr.timeUnit());
                    model.setStartAt(descr.startAt());
                }
                recurringTaskrepository.save(model);
            }
        }
    }

    public void runTask(Class<? extends Task> taskClass, Runnable onStart, Runnable onStop) {
        TaskModel taskModel = new TaskModel();
        taskModel.setTaskClassName(taskClass.getName());
        taskModel.setStatus(TaskModel.Status.WAITING);
        logger.info(taskModel.toString());
        runTask(taskClass, taskRepository.save(taskModel), onStart, onStop);
    }

    private void runTask(
            Class<? extends Task> taskClass,
            TaskModel taskModel,
            Runnable onStart,
            Runnable onStop
    ) {
        Thread thread = new Thread(
                threadGroup,
                () -> {
                    Task task;
                    try {
                        logger.info("Task %s started".formatted(taskModel.getTaskClassName()));
                        taskModel.setStatus(TaskModel.Status.RUNNING);
                        taskModel.setStarted(new Date());
                        taskRepository.save(taskModel);
                        if (onStart != null) {
                            onStart.run();
                        }
                        task = taskClass.getDeclaredConstructor().newInstance();
                        String msg = task.run(beanFactory);
                        taskModel.setStatus(TaskModel.Status.SUCCESS);
                        if (msg != null && !msg.isEmpty()) {
                            taskModel.setStatusMsg(msg);
                        }
                    } catch (Exception ex) {
                        String msg = ex.getMessage();
                        logger.error(msg);
                        taskModel.setStatus(TaskModel.Status.ERROR);
                        taskModel.setStatusMsg(msg);
                    }
                    taskModel.setStopped(new Date());
                    logger.info("Task %s stopped".formatted(taskModel.getTaskClassName()));
                    taskRepository.save(taskModel);
                    if (onStop != null) {
                        onStop.run();
                    }
                },
                "arachne-%d".formatted(taskNr++));
        thread.start();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}

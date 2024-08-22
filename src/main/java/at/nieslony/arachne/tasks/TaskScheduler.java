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

import at.nieslony.arachne.tasks.scheduled.RefreshLdapUsers;
import at.nieslony.arachne.tasks.scheduled.UpdateCrl;
import at.nieslony.arachne.tasks.scheduled.UpdateDhParams;
import at.nieslony.arachne.tasks.scheduled.UpdateVpnServerCert;
import at.nieslony.arachne.tasks.scheduled.UpdateWebServerCertificate;
import at.nieslony.arachne.utils.ArachneTimeUnit;
import jakarta.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
public class TaskScheduler implements BeanFactoryAware {

    private static final Logger logger = LoggerFactory.getLogger(TaskScheduler.class);

    private final ThreadGroup threadGroup;
    private BeanFactory beanFactory;
    private int taskNr = 0;
    private Map<String, ScheduledFuture<?>> scheduledTasks;

    @Getter
    ScheduledExecutorService scheduler;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RecurringTasksRepository recurringTaskRepository;

    @Getter
    private final List<Class<? extends Task>> taskTypes;

    public TaskScheduler() {
        taskTypes = new LinkedList<>();
        threadGroup = new ThreadGroup("arachne-tasks");
        scheduler = Executors.newScheduledThreadPool(5);
        scheduledTasks = new HashMap<>();
    }

    private void killTerminatedTasks() {
        logger.info("Kill terminated tasks");
        for (TaskModel task : taskRepository.findAllByStatus(TaskModel.Status.RUNNING)) {
            task.setStatus(TaskModel.Status.ERROR);
            task.setStatusMsg("Killed during Server Termination");
            logger.info("Killing " + task.getTaskClassName());
            taskRepository.save(task);
        }
    }

    private void registerTaskTypes() {
        logger.info("Registering task types");
        for (var task : taskTypes) {
            String className = task.getName();
            RecurringTaskModel model
                    = recurringTaskRepository.findByClassName(className);
            if (model == null) {
                model = new RecurringTaskModel();
                model.setClassName(className);
                RecurringTaskDescription descr
                        = task.getAnnotation(RecurringTaskDescription.class);
                if (descr != null) {
                    model.setRecurringInterval(descr.defaulnterval());
                    model.setTimeUnit(descr.timeUnit());
                    model.setRepeatTask(
                            model.getRecurringInterval() != 0
                            && model.getRecurringInterval() != 0
                    );
                    model.setStartAt(descr.startAt());
                    model.setStartAtFixTime(
                            !model.getStartAt().isEmpty()
                    );
                }
                logger.info("Registering task " + model.getClassName());
                recurringTaskRepository.save(model);
            }
        }
    }

    private Date getNextSchedulingDate(RecurringTaskModel model) {
        if (model.getRepeatTask() != null && !model.getRepeatTask()) {
            return null;
        }
        if (model.getTimeUnit() == null
                || model.getRecurringInterval() == null
                || model.getRecurringInterval() < 1) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        switch (model.getTimeUnit()) {
            case MIN ->
                cal.add(Calendar.MINUTE, model.getRecurringInterval());
            case HOUR ->
                cal.add(Calendar.HOUR, model.getRecurringInterval());
            case DAY ->
                cal.add(Calendar.DATE, model.getRecurringInterval());
        }
        if (model.getTimeUnit() == ArachneTimeUnit.DAY
                && model.getStartAtFixTime() != null
                && model.getStartAtFixTime()) {
            var time = model.getStartAtAsTime();
            cal.set(Calendar.HOUR, time.hour());
            cal.set(Calendar.MINUTE, time.min());
        }
        if (cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DATE, 1);
        }
        return cal.getTime();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleTasks() {
        logger.info("Scheduling tasks");
        var alreadyScheduledTasks = taskRepository.findAllByStatus(
                TaskModel.Status.SCHEDULED
        );
        for (var recurringTask : recurringTaskRepository.findAll()) {
            String taskClassName = recurringTask.getClassName();
            TaskModel alreadyScheduledTask = null;
            for (TaskModel t : alreadyScheduledTasks) {
                if (t.getTaskClassName().equals(taskClassName)) {
                    alreadyScheduledTask = t;
                }
            }
            if (alreadyScheduledTask == null) {
                Date next = getNextSchedulingDate(recurringTask);
                if (next != null) {
                    TaskModel model = new TaskModel();
                    model.setScheduled(next);
                    model.setTaskClassName(taskClassName);
                    model.setStatus(TaskModel.Status.SCHEDULED);
                    logger.info("Scheduling task %s for %s".formatted(
                            model.getTaskClassName(), next.toString()
                    ));
                    taskRepository.save(model);
                    scheduleTask(model);
                } else {
                    logger.info("Task %s is not repeated".formatted(taskClassName));
                }
            } else {
                logger.info(
                        "Task %s is already scheduled at %s"
                                .formatted(
                                        taskClassName,
                                        alreadyScheduledTask.getScheduled().toString()
                                )
                );
                scheduleTask(alreadyScheduledTask);
            }
        }
    }

    public void scheduleTask(TaskModel model) {
        RecurringTaskModel recurringTaskModel
                = recurringTaskRepository.findByClassName(
                        model.getTaskClassName()
                );
        if (scheduledTasks.containsKey(model.getTaskClassName())) {
            logger.info("Cancelling job to reschedule it");
            var futureTask = scheduledTasks.remove(model.getTaskClassName());
            futureTask.cancel(false);
        }

        Date startAt = model.getScheduled();
        Calendar now = Calendar.getInstance();
        if (now.after(startAt)) {
            if (recurringTaskModel.getStartAtFixTime()) {
                Calendar cal = Calendar.getInstance();
                var time = recurringTaskModel.getStartAtAsTime();
                cal.set(Calendar.HOUR, time.hour());
                cal.set(Calendar.MINUTE, time.min());
                if (cal.before(now)) {
                    cal.add(Calendar.DATE, 1);
                }
                startAt = cal.getTime();
            } else {
                startAt = now.getTime();
            }
            logger.info(
                    "Task scheduled in past, rescheduled to "
                    + startAt.toString()
            );
            model.setScheduled(startAt);
            taskRepository.save(model);
        }

        AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();
        ArachneTimerTask arachneTimerTask;
        try {
            arachneTimerTask = new ArachneTimerTask(beanFactory, model, future);
        } catch (Exception ex) {
            String msg = "Cannot create task %s: %s"
                    .formatted(
                            model.getTaskClassName(),
                            ex.getMessage());
            logger.error(msg);
            model.setStatusMsg(msg);
            model.setStatus(TaskModel.Status.ERROR);
            taskRepository.save(model);
            return;
        }

        long delay = (startAt.getTime() - now.getTime().getTime()) / 1000;
        long interval = recurringTaskModel.getRecurringIntervalInSecs();
        future.set(scheduler.scheduleAtFixedRate(
                arachneTimerTask,
                delay,
                interval,
                TimeUnit.SECONDS
        ));

        scheduledTasks.put(model.getTaskClassName(), future.get());
    }

    @PostConstruct
    public void init() {
        taskTypes.add(UpdateVpnServerCert.class);
        taskTypes.add(UpdateDhParams.class);
        taskTypes.add(UpdateCrl.class);
        taskTypes.add(UpdateWebServerCertificate.class);
        taskTypes.add(RefreshLdapUsers.class);

        killTerminatedTasks();
        registerTaskTypes();
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

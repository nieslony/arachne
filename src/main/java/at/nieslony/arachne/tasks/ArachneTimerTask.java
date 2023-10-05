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

import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;

/**
 *
 * @author claas
 */
public class ArachneTimerTask extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(ArachneTimerTask.class);

    private Task task;
    private final BeanFactory beanFactory;
    private TaskModel taskModel;
    private final TaskRepository taskRepository;
    private final TaskScheduler taskScheduler;
    private final AtomicReference<ScheduledFuture<?>> future;

    public ArachneTimerTask(
            BeanFactory beanFactory,
            TaskModel model,
            AtomicReference<ScheduledFuture<?>> future)
            throws Exception {
        this.beanFactory = beanFactory;
        this.taskModel = model;
        this.future = future;
        this.taskRepository = beanFactory.getBean(TaskRepository.class);
        this.taskScheduler = beanFactory.getBean(TaskScheduler.class);
        this.task = model.getTaskClass().getDeclaredConstructor().newInstance();
    }

    @Override
    public void run() {
        taskModel.setStatus(TaskModel.Status.RUNNING);
        taskModel.setStarted();
        taskRepository.save(taskModel);
        try {
            task.run(beanFactory);
            taskModel.setStatus(TaskModel.Status.SUCCESS);
        } catch (Exception ex) {
            String msg = "Error executing task %s: %s"
                    .formatted(
                            taskModel.getTaskClassName(),
                            ex.getMessage()
                    );
            logger.error(msg);
            taskModel.setStatusMsg(msg);
            taskModel.setStatus(TaskModel.Status.ERROR);
        }
        taskModel.setStopped();
        taskRepository.save(taskModel);

        taskScheduler.getScheduler().schedule(
                () -> {
                    logger.info(
                            "Creating next scheduled entry %s on %s"
                                    .formatted(
                                            taskModel.getTaskClassName(),
                                            taskModel.getScheduled().toString()
                                    )
                    );
                    long nextRun
                    = new Date().getTime()
                    + future.get().getDelay(TimeUnit.MILLISECONDS);
                    TaskModel newTaskModel = new TaskModel();
                    newTaskModel.setStatus(TaskModel.Status.SCHEDULED);
                    newTaskModel.setScheduled(new Date(nextRun));
                    newTaskModel.setTaskClassName(taskModel.getTaskClassName());
                    taskRepository.save(newTaskModel);
                    taskModel = newTaskModel;
                },
                10,
                java.util.concurrent.TimeUnit.MILLISECONDS
        );
    }
}

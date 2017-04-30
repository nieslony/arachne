/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.tasks.ScheduledTask;
import at.nieslony.openvpnadmin.tasks.ScheduledTaskInfo;
import at.nieslony.utils.classfinder.ClassFinder;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

/**
 *
 * @author claas
 */
@ManagedBean(eager = true)
@ApplicationScoped
public class TaskScheduler {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    public class AvailableTask {
        private final Class klass;
        private final String name;
        private final String description;

        AvailableTask(Class klass, String name, String description) {
            this.klass = klass;
            this.name = name;
            this.description = description;
        }

        public Class getKlass() {
            return klass;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    public class TaskListEntry {
        private String name;
        private String comment = null;
        private long  startupDelay = -1;
        private long interval = -1;
        private boolean isEnabled = false;
        private final Class<ScheduledTask> taskClass;

        public TaskListEntry(Class taskClass) {
            this.taskClass = taskClass;
            ScheduledTaskInfo info = (ScheduledTaskInfo) taskClass.getAnnotation(ScheduledTaskInfo.class);
            if (info != null) {
                name = info.name();
            }
            else {
                name = taskClass.getName();
            }
        }

        public Class getTaskClass() {
            return taskClass;
        }

        public String getName() {
            return name;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public long getStartupDelay() {
            return startupDelay;
        }

        public void setStartupDelay(long startupDelay) {
            this.startupDelay = startupDelay;
        }

        public long getInterval() {
            return interval;
        }

        public void setInterval(long interval) {
            this.interval = interval;
        }

        public boolean isEnabled() {
            return isEnabled;
        }

        public void setEnabled(boolean enabled) {
            this.isEnabled = enabled;
        }
    }

    final List<AvailableTask> availableTasks = new LinkedList<>();
    final List<TaskListEntry> scheduledTasks = new LinkedList<>();

    /**
     * Creates a new instance of TaskScheduler
     */
    public TaskScheduler() {
    }

    @PostConstruct
    public void init() {
        try {
            ClassFinder classFinder = new ClassFinder((getClass().getClassLoader()));

            List<Class> classes = classFinder.getAllClassesImplementing(ScheduledTask.class);
            for (Class c : classes) {
                logger.info(String.format("Found task scheduler class %s", c.getName()));
                if (c.isAnnotationPresent(ScheduledTaskInfo.class)) {
                    ScheduledTaskInfo info =
                            (ScheduledTaskInfo) c.getAnnotation(ScheduledTaskInfo.class);

                    availableTasks.add(
                            new AvailableTask(c, info.name(), info.description()));
                }
                else {
                    availableTasks.add(new AvailableTask(c, null, null));
                }
            }
        } catch (URISyntaxException | ClassNotFoundException | IOException ex) {
            Logger.getLogger(TaskScheduler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public List<AvailableTask> getAvailableTasks() {
        return availableTasks;
    }

    public List<TaskListEntry> getScheduledTasks() {
        return scheduledTasks;
    }

    public void onAddTask() {

    }

    public void onEditTask() {

    }

    public void onRemoveTask() {

    }
}

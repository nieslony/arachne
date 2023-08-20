/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.tasks;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
public class TaskListEntry implements Serializable {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private String name;
    private String comment = null;
    private long startupDelay = -1;
    private long interval = -1;
    private boolean isEnabled = false;
    private Class<ScheduledTask> taskClass;
    private long id;
    transient private ScheduledFuture<?> future;

    public TaskListEntry(Class taskClass) {
        this.taskClass = taskClass;
        ScheduledTaskInfo info = (ScheduledTaskInfo) taskClass.getAnnotation(ScheduledTaskInfo.class);
        if (info != null) {
            name = info.name();
        } else {
            name = taskClass.getName();
        }
    }

    private int getSecs(long l) {
        long secs = l % 60;
        return (int) secs;
    }

    private int getMins(long l) {
        long mins = l % (60 * 60) / 60;
        return (int) mins;
    }

    private int getHours(long l) {
        long hours = l % (24 * 60 * 60) / (60 * 60);
        return (int) hours;
    }

    private int getDays(long l) {
        long days = l / (24 * 60 * 60);
        return (int) days;
    }

    private long getLongTime(int days, int hours, int mins, int secs) {
        long time = secs + mins * 60 + hours * 60 * 60 + days * 60 * 60 * 24;
        return time;
    }

    public void setInterval(int days, int hours, int mins, int secs) {
        interval = getLongTime(days, hours, mins, secs);
    }

    public void setStartupDelay(int days, int hours, int mins, int secs) {
        startupDelay = getLongTime(days, hours, mins, secs);
    }

    private String formatTime(long l) {
        int days = getDays(l);
        int hours = getHours(l);
        int mins = getMins(l);
        int secs = getSecs(l);
        String s;
        if (days > 0) {
            s = String.format("%d days, %02d:%02d:%02d hours", days, hours, mins, secs);
        } else {
            s = String.format("%02d:%02d:%02d hours", hours, mins, secs);
        }
        return s;
    }

    public String getFormatStartupDelay() {
        return formatTime(startupDelay);
    }

    public String getFormatInterval() {
        return formatTime(interval);
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

    public int getStartupDelayDays() {
        return getDays(startupDelay);
    }

    public int getStartupDelayHours() {
        return getHours(startupDelay);
    }

    public int getStartupDelayMins() {
        return getMins(startupDelay);
    }

    public int getStartupDelaySecs() {
        return getSecs(startupDelay);
    }

    public int getIntervalDays() {
        return getDays(interval);
    }

    public int getIntervalHours() {
        return getHours(interval);
    }

    public int getIntervalMins() {
        return getMins(interval);
    }

    public int getIntervalSecs() {
        return getSecs(interval);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private final Runnable runnable = new Runnable() {
        ScheduledTask task = null;

        @Override
        public void run() {
            if (task == null) {
                try {
                    task = taskClass.newInstance();
                }
                catch (IllegalAccessException | InstantiationException ex) {
                    logger.warning(String.format("Cannot create task %s: %s",
                            getName(), ex.getMessage()));
                    return;
                }
            }

            logger.info(String.format("Executing task \"%s\"", getName()));
            try {
                task.run();
            }
            catch (Throwable ex) {
                logger.warning(String.format("Task \"%s\" failed: %s",
                        getName(), ex.getMessage()));
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                logger.warning(sw.toString());
            }
            logger.info(String.format("Task \"%s\" terminated", getName()));
        }
    };

    public void scheduleTask(ScheduledThreadPoolExecutor scheduler) {
        future = scheduler.scheduleAtFixedRate(runnable,
                getStartupDelay(),
                getInterval(),
                TimeUnit.SECONDS);
    }

    public void scheduleTask(ScheduledThreadPoolExecutor scheduler, long delay) {
        future = scheduler.scheduleAtFixedRate(runnable,
                delay,
                getInterval(),
                TimeUnit.SECONDS);
    }

    public void cancel() {
        future.cancel(false);
    }


    public long getRemainingDelay() {
        if (future == null)
            return -1;
        return future.getDelay(TimeUnit.SECONDS);
    }

    public String getScheduledExecutionTime() {
        if (isEnabled()) {
            long remainiung = getRemainingDelay();
            if (remainiung <= 0)
                return "unknown";

            Date date = new Date(System.currentTimeMillis() + remainiung * 1000);

            return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(date);
        }
        else {
            return "";
        }
    }
}

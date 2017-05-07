/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.tasks;

import at.nieslony.openvpnadmin.tasks.ScheduledTask;
import at.nieslony.openvpnadmin.tasks.ScheduledTaskInfo;
import java.io.Serializable;

/**
 *
 * @author claas
 */
public class TaskListEntry implements Serializable {

    private String name;
    private String comment = null;
    private long startupDelay = -1;
    private long interval = -1;
    private boolean isEnabled = false;
    private final Class<ScheduledTask> taskClass;

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
        long secs = l % (60 * 60) / (60);
        return (int) secs;
    }

    private int getMins(long l) {
        long mins = l % (60 * 60 * 60) / (60 * 60);
        return (int) mins;
    }

    private int getHours(long l) {
        long hours = l % (60 * 60 * 60 * 24) / (60 * 60 * 60);
        return (int) hours;
    }

    private int getDays(long l) {
        long days = l / (60 * 60 * 60 * 24);
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
        return getDays(startupDelay);
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
        return getDays(interval);
    }

    public int getIntervalMins() {
        return getMins(interval);
    }

    public int getIntervalSecs() {
        return getSecs(interval);
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.beans.TaskScheduler;
import at.nieslony.openvpnadmin.tasks.TaskListEntry;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import org.primefaces.context.RequestContext;

/**
 *
 * @author claas
 */
@ManagedBean
@ViewScoped
public class EditTaskScheduler
    implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{taskScheduler}")
    private TaskScheduler taskScheduler;

    public void setTaskScheduler(TaskScheduler ts) {
        taskScheduler = ts;
    }

    /**
     * Creates a new instance of EditTaskScheduler
     */
    public EditTaskScheduler() {
    }

    private int dlgAddTask_startupDelaySecs;
    private int dlgAddTask_startupDelayMins;
    private int dlgAddTask_startupDelayHours;
    private int dlgAddTask_startupDelayDays;
    private int dlgAddTask_intervalSecs;
    private int dlgAddTask_intervalMins;
    private int dlgAddTask_intervalHours;
    private int dlgAddTask_intervalDays;
    private boolean dlgAddTask_isEnabled;
    private String dlgAddTask_comment;
    private String dlgAddTask_taskType = null;

    public void onAddTaskOk() {
        logger.info(String.format("Creating task for class %s", dlgAddTask_taskType));

        Class cl = null;

        try {
            cl = Class.forName(dlgAddTask_taskType);
        }
        catch (ClassNotFoundException ex) {
            logger.warning(String.format("Cannot create task for class %s", ex));
        }

        if (cl != null) {
            TaskListEntry task = null;
            task = new TaskListEntry(cl);
            task.setComment(dlgAddTask_comment);
            task.setEnabled(dlgAddTask_isEnabled);
            task.setInterval(dlgAddTask_intervalDays, dlgAddTask_intervalHours, dlgAddTask_intervalMins, dlgAddTask_intervalSecs);
            task.setStartupDelay(dlgAddTask_intervalDays, dlgAddTask_intervalHours, dlgAddTask_intervalMins, dlgAddTask_intervalSecs);

            taskScheduler.addTask(task);
        }
        else {
            logger.info(String.format("There's no task class, don't create task"));
        }

        RequestContext.getCurrentInstance().execute("PF('dlgAddTask').hide();");
    }

    public void onEditTask() {
    }

    public void onRemoveTask() {

    }

    public String getDlgAddTask_taskType() {
        return dlgAddTask_taskType;
    }

    public void setDlgAddTask_taskType(String tt) {
        dlgAddTask_taskType = tt;
    }

    public int getDlgAddTask_startupDelaySecs() {
        return dlgAddTask_startupDelaySecs;
    }

    public void setDlgAddTask_startupDelaySecs(int secs) {
        dlgAddTask_startupDelaySecs = secs;
    }

    public int getDlgAddTask_startupDelayMins() {
        return dlgAddTask_startupDelayMins;
    }

    public void setDlgAddTask_startupDelayMins(int mins) {
        dlgAddTask_startupDelayMins = mins;
    }

    public int getDlgAddTask_startupDelayHours() {
        return dlgAddTask_startupDelayHours;
    }

    public void setDlgAddTask_startupDelayHours(int hours) {
        dlgAddTask_startupDelayHours = hours;
    }

    public int getDlgAddTask_startupDelayDays() {
        return dlgAddTask_startupDelayDays;
    }

    public void setDlgAddTask_startupDelayDays(int days) {
        dlgAddTask_startupDelayDays = days;
    }

    public int getDlgAddTask_intervalSecs() {
        return dlgAddTask_intervalSecs;
    }

    public void setDlgAddTask_intervalSecs(int secs) {
        dlgAddTask_intervalSecs = secs;
    }

    public int getDlgAddTask_intervalMins() {
        return dlgAddTask_intervalMins;
    }

    public void setDlgAddTask_intervalMins(int mins) {
        dlgAddTask_intervalMins = mins;
    }

    public int getDlgAddTask_intervalHours() {
        return dlgAddTask_intervalHours;
    }

    public void setDlgAddTask_intervalHours(int hours) {
        dlgAddTask_intervalHours = hours;
    }

    public int getDlgAddTask_intervalDays() {
        return dlgAddTask_intervalDays;
    }

    public void setDlgAddTask_intervalDays(int days) {
        dlgAddTask_intervalDays = days;
    }

    public boolean getDlgAddTask_isEnabled() {
        return dlgAddTask_isEnabled;
    }

    public void setDlgAddTask_isEnabled(boolean enabled) {
        dlgAddTask_isEnabled = enabled;
    }

    public String getDlgAddTask_comment() {
        return dlgAddTask_comment;
    }

    public void setDlgAddTask_comment(String comment) {
        dlgAddTask_comment = comment;
    }

    public boolean isInputOk() {
        return dlgAddTask_taskType != null;
    }
}
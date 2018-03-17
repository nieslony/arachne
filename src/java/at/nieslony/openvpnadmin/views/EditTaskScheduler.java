/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.views;

import at.nieslony.openvpnadmin.beans.TaskScheduler;
import at.nieslony.openvpnadmin.tasks.TaskListEntry;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import org.primefaces.PrimeFaces;

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

    enum DlgTaskMode {
        ADD("Add task"),
        EDIT("Edit task");

        private String title;

        DlgTaskMode(String title) {
            this.title = title;
        }

        String getTitle() {
            return title;
        }
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

    private TaskListEntry selectedTask = null;

    private DlgTaskMode dlgTaskMode = DlgTaskMode.ADD;

    public void setSelectedTask(TaskListEntry tle) {
        selectedTask = tle;
    }

    public TaskListEntry getSelectedTask() {
        return selectedTask;
    }

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
            task.setStartupDelay(dlgAddTask_startupDelayDays, dlgAddTask_startupDelayHours, dlgAddTask_startupDelayMins, dlgAddTask_startupDelaySecs);

            if (dlgTaskMode == DlgTaskMode.ADD) {
                try {
                    taskScheduler.addTask(task);
                }
                catch (ClassNotFoundException | SQLException ex) {
                    String msg = String.format("Cannot add task: %s", ex.getMessage());
                    logger.warning(msg);
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", msg));
                }
            }
            else {
                try {
                    task.setId(selectedTask.getId());
                    taskScheduler.updateTask(task);
                }
                catch (ClassNotFoundException | SQLException ex) {
                    String msg = String.format("Cannot update task: %s", ex.getMessage());
                    logger.warning(msg);
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", msg));
                }
            }
        }
        else {
            logger.info(String.format("There's no task class, don't create task"));
        }
        PrimeFaces.current().executeScript("PF('dlgAddTask').hide();");
    }

    public void onAddTask() {
        dlgAddTask_comment = "";
        dlgAddTask_intervalDays = 0;
        dlgAddTask_intervalHours = 0;
        dlgAddTask_intervalMins = 0;
        dlgAddTask_intervalSecs = 0;
        dlgAddTask_startupDelayDays = 0;
        dlgAddTask_startupDelayHours = 0;
        dlgAddTask_startupDelayMins = 0;
        dlgAddTask_startupDelaySecs = 0;
        dlgAddTask_isEnabled = false;
        dlgAddTask_taskType = null;
        dlgTaskMode = DlgTaskMode.ADD;

        PrimeFaces.current().executeScript("PF('dlgAddTask').show();");
    }

    public void onEditTask() {
        dlgAddTask_comment = selectedTask.getComment();
        dlgAddTask_intervalDays = selectedTask.getIntervalDays();
        dlgAddTask_intervalHours = selectedTask.getIntervalHours();
        dlgAddTask_intervalMins = selectedTask.getIntervalMins();
        dlgAddTask_intervalSecs = selectedTask.getIntervalSecs();
        dlgAddTask_startupDelayDays = selectedTask.getStartupDelayDays();
        dlgAddTask_startupDelayHours = selectedTask.getStartupDelayHours();
        dlgAddTask_startupDelayMins = selectedTask.getStartupDelayMins();
        dlgAddTask_startupDelaySecs = selectedTask.getStartupDelaySecs();
        dlgAddTask_isEnabled = selectedTask.isEnabled();
        dlgAddTask_taskType = selectedTask.getTaskClass().getName();

        dlgTaskMode = DlgTaskMode.EDIT;

        PrimeFaces.current().executeScript("PF('dlgAddTask').show();");
    }

    public void onRemoveTask() {
        if (selectedTask != null) {
            logger.info(String.format("Removing task %d", selectedTask.getId()));

            try {
                taskScheduler.removeTask(selectedTask);
            }
            catch (ClassNotFoundException | SQLException ex) {
                String msg = String.format("Cannot remove task: %s", ex.getMessage());
                logger.warning(msg);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", msg));
            }
        }
        else {
            logger.info("No task selected, nothing to remove");
        }
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
        return dlgAddTask_taskType != null && !dlgAddTask_taskType.isEmpty();
    }

    public String getDlgTaskMode() {
        return dlgTaskMode.getTitle();
    }

    public boolean getSelectTaskTypeEnabled() {
        return dlgTaskMode == DlgTaskMode.ADD;
    }
}


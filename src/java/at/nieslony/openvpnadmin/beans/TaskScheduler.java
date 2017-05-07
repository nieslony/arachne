/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.tasks.AvailableTask;
import at.nieslony.openvpnadmin.tasks.ScheduledTask;
import at.nieslony.openvpnadmin.tasks.TaskListEntry;
import at.nieslony.utils.DbUtils;
import at.nieslony.utils.classfinder.ClassFinder;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

/**
 *
 * @author claas
 */
@ManagedBean(eager = true)
@ApplicationScoped
public class TaskScheduler
        implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{databaseSettings}")
    private DatabaseSettings databaseSettings;

    public void setDatabaseSettings(DatabaseSettings dbs) {
        databaseSettings = dbs;
    }

    @ManagedProperty(value = "#{folderFactory}")
    private FolderFactory folderFactory;

    public void setFolderFactory(FolderFactory ff) {
        folderFactory = ff;
    }



    transient final List<AvailableTask> availableTasks = new LinkedList<>();
    transient final Map<Long, TaskListEntry> scheduledTasks = new HashMap<>();

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
                availableTasks.add(new AvailableTask(c));
            }

            reloadTasks();
        } catch (URISyntaxException | ClassNotFoundException | IOException ex) {
            Logger.getLogger(TaskScheduler.class.getName()).log(Level.SEVERE, null, ex);
        }

        reloadTasks();
    }

    private void reloadTasks() {
        try {
            Connection con = databaseSettings.getDatabseConnection();
            Statement stm = con.createStatement();
            String sql = "SELECT * FROM scheduledTasks";
            ResultSet result = stm.executeQuery(sql);
            while (result.next()) {
                String taskClass = result.getString("taskClass");
                long startupDelay = result.getLong("startupDelay");
                long interval = result.getLong("interval");
                boolean isEnabled = result.getBoolean("isEnabled");
                String comment = result.getString("comment");
                long id = result.getLong("id");
                TaskListEntry task = null;

                if (!scheduledTasks.containsKey(id)) {
                    task = new TaskListEntry(Class.forName(taskClass));
                    scheduledTasks.put(id, task);
                }
                else {
                    task = scheduledTasks.get(id);
                }
                task.setInterval(interval);
                task.setStartupDelay(startupDelay);
                task.setEnabled(isEnabled);
                task.setComment(comment);
            }
        }
        catch (ClassNotFoundException | SQLException ex) {
            logger.warning(String.format("Cannot read scheduled tasks: %s", ex.getMessage()));
        }
    }

   public void createTables()
            throws IOException, SQLException, ClassNotFoundException
    {
        logger.info("Creating tables for propertiesStorage...");
        String resourceName = "create-task-scheduler-tables.sql";
        Reader r = null;
        try {
            r = new FileReader(String.format("%s/%s", folderFactory.getSqlDir(), resourceName));

            if (r == null) {
                logger.severe(String.format("Cannot open %s as resource", resourceName));
            }
            Connection con = databaseSettings.getDatabseConnection();
            if (con == null) {
                logger.severe("Cannot get database connection");
            }
            DbUtils.executeSql(con, r);
        }
        finally {
            if (r != null) {
                try {
                    r.close();
                }
                catch (IOException ex) {
                    logger.severe(String.format("Cannot close reader: %s", ex.getMessage()));
                }
            }
        }
    }
    public void setupDatebase() {
        reloadTasks();
    }

    public List<AvailableTask> getAvailableTasks() {
        return availableTasks;
    }

    public Collection<TaskListEntry> getScheduledTasks() {
        return scheduledTasks.values();
    }

    public void addTask(TaskListEntry tle) {
        try {
            Connection con = databaseSettings.getDatabseConnection();
            String sql = "INSERT INTO scheduledTasks " +
                    "(taskClass, startupDelay, interval, isEnabled, comment) " +
                    "VALUES (?, ?, ?, ?, ?);";
            PreparedStatement stm = con.prepareStatement(sql);
            logger.info(String.format("Executing sql: %s", stm.toString()));
            int pos = 1;
            stm.setString(pos++, tle.getTaskClass().getName());
            stm.setLong(pos++, tle.getStartupDelay());
            stm.setLong(pos++, tle.getInterval());
            stm.setBoolean(pos++, tle.isEnabled());
            stm.setString(pos++, tle.getComment());

            int ret = stm.executeUpdate();
            logger.info(String.format("%d entries inserted", ret));
            stm.close();
        }
        catch (ClassNotFoundException | SQLException ex) {
            String msg = String.format("Cannot add task: %s", ex.getMessage());
            logger.warning(msg);
        }

        reloadTasks();
    }
}

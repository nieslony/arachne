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
import at.nieslony.utils.classfinder.BeanInjector;
import at.nieslony.utils.classfinder.ClassFinder;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;

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
    transient final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1,
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger();

                @Override
                public Thread newThread(Runnable r) {
                    final String threadName =
                            String.format("arachne-task-executor-%d", counter.incrementAndGet());

                    return new Thread(r, threadName);
                }
    });

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
            FacesContext ctx = FacesContext.getCurrentInstance();
            for (Class c : classes) {
                logger.info(String.format("Found task scheduler class %s", c.getName()));
                availableTasks.add(new AvailableTask(c));

                try {
                    BeanInjector.injectStaticBeans(ctx, c);
                }
                catch (NoSuchMethodException ex) {
                    logger.warning(String.format("Cannot find method in class %s: %s",
                        c.getName(), ex.getMessage()));
                }
                catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    logger.warning(String.format("Cannot invoke method: %s",
                            c.getName(),  ex.getMessage()));
                }

            }

            reloadTasks();
        } catch (URISyntaxException | ClassNotFoundException | IOException ex) {
            Logger.getLogger(TaskScheduler.class.getName()).log(Level.SEVERE, null, ex);
        }

        reloadTasks();

        scheduledTasks.values().forEach((tle) -> {
            logger.info(
                    String.format("Scheduling task %s: delay: %d days, %d:%d:%d interval: %d days, %d:%d:%d",
                            tle.getName(),
                            tle.getStartupDelayDays(),
                            tle.getStartupDelayHours(), tle.getStartupDelayMins(), tle.getStartupDelaySecs(),
                            tle.getIntervalDays(),
                            tle.getIntervalHours(), tle.getIntervalMins(), tle.getIntervalSecs()
                    ));

            if (tle.isEnabled())
                tle.scheduleTask(scheduler);
        });
    }

    @PreDestroy
    public void destroy() {
        logger.info("Destroying task scheduler");
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.shutdownNow();
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
                task.setId(id);
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

    public List<AvailableTask> getAvailableTasks() {
        return availableTasks;
    }

    public Collection<TaskListEntry> getScheduledTasks() {
        return scheduledTasks.values();
    }

    public void removeTask(TaskListEntry tle)
            throws ClassNotFoundException, SQLException
    {
        Connection con = databaseSettings.getDatabseConnection();
        String sql = "DELETE FROM scheduledTasks WHERE id = ?;";
        PreparedStatement stm = con.prepareStatement(sql);
        int pos = 1;
        stm.setLong(pos, tle.getId());
        stm.executeUpdate();

        tle.cancel();

        scheduledTasks.remove(tle.getId());
        scheduler.purge();
    }

    public void addTask(TaskListEntry tle)
            throws ClassNotFoundException, SQLException
    {
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

        if (tle.isEnabled())
            tle.scheduleTask(scheduler);

        reloadTasks();
    }

    public void updateTask(TaskListEntry tle)
            throws ClassNotFoundException, SQLException
    {
        TaskListEntry entry = scheduledTasks.get(tle.getId());

        if (entry != null) {
            long restTime = entry.getRemainingDelay();
            long intervalOld = entry.getInterval();

            entry.setComment(tle.getComment());
            entry.setInterval(tle.getInterval());
            entry.setStartupDelay(tle.getStartupDelay());
            entry.setEnabled(tle.isEnabled());

            Connection con = databaseSettings.getDatabseConnection();
            String sql = "UPDATE scheduledTasks " +
                    "SET taskClass = ?, startupDelay = ?, interval = ?, isEnabled = ?, comment = ? " +
                    "WHERE id = ?;";
            PreparedStatement stm = con.prepareStatement(sql);
            logger.info(String.format("Executing sql: %s", stm.toString()));
            int pos = 1;
            stm.setString(pos++, tle.getTaskClass().getName());
            stm.setLong(pos++, tle.getStartupDelay());
            stm.setLong(pos++, tle.getInterval());
            stm.setBoolean(pos++, tle.isEnabled());
            stm.setString(pos++, tle.getComment());
            stm.setLong(pos, tle.getId());

            stm.executeUpdate();

            entry.cancel();
            scheduler.purge();

            if (entry.isEnabled()) {
                long delay;
                if (tle.getInterval() > intervalOld) {
                    delay = entry.getInterval() - restTime;
                }
                else {
                    if (entry.getInterval() - restTime > intervalOld) {
                        delay = entry.getInterval();
                    }
                    else {
                        delay = 0;
                    }
                }
                entry.scheduleTask(scheduler, delay);
            }
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.tasks.ScheduledTask;
import at.nieslony.openvpnadmin.tasks.ScheduledTaskInfo;
import at.nieslony.utils.DbUtils;
import at.nieslony.utils.classfinder.ClassFinder;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.sql.Connection;
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
public class TaskScheduler {
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
            }
            else {
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

    final List<AvailableTask> availableTasks = new LinkedList<>();
    final Map<Long, TaskListEntry> scheduledTasks = new HashMap<>();

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
                task.interval = interval;
                task.startupDelay = startupDelay;
                task.isEnabled = isEnabled;
                task.comment = comment;
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

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.VpnUser;
import at.nieslony.utils.DbUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author claas
 */
@ManagedBean
@ApplicationScoped
public class LocalUsers implements Serializable {
    private static final long serialVersionUID = 12345L;

    private final HashMap<String, VpnUser> users;

    private static final String USERS_FILE = "users";
    private String usersFilePath;
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{databaseSettings}")
    private DatabaseSettings databaseSettings;

    public void setDatabaseSettings(DatabaseSettings databaseSettings) {
        this.databaseSettings = databaseSettings;
    }

    @ManagedProperty(value = "#{folderFactory}")
    private FolderFactory folderFactory;

    public void setFolderFactory(FolderFactory ff) {
        folderFactory = ff;
    }

    /**
     * Creates a new instance of LocalUsers
     */
    public LocalUsers() {
        ExternalContext extCtx = FacesContext.getCurrentInstance().getExternalContext();
        users = new HashMap<>();
        usersFilePath = extCtx.getRealPath("/" + extCtx.getInitParameter("dynamic-data-dir"));
        File dynDir = new File(usersFilePath);
        if (!dynDir.exists()) {
            logger.info(String.format("Creating directoty %s", usersFilePath));
            dynDir.mkdirs();
        }
        usersFilePath = usersFilePath + "/" + USERS_FILE;
        loadUsers();
    }

    public void loadUsers() {
        logger.info("Reading local users");
        try(BufferedReader br = new BufferedReader(new FileReader(usersFilePath))) {
            for(String line; (line = br.readLine()) != null; ) {
                String[] tokens = line.split(":");
                if (tokens.length != 2) {
                    logger.warning(String.format("Cannot parse user: " + line));
                    continue;
                }
                VpnUser user = new VpnUser(tokens[0]);
                user.setPasswordHash(tokens[1]);
                user.setUserType(VpnUser.UserType.UT_LOCAL);
                users.put(user.getUsername(), user);
            }
        }
        catch (FileNotFoundException ex) {
            logger.warning("There's no local users file");
        }
        catch (IOException ex) {
            logger.severe(ex.getMessage());
        }
    }

    @PostConstruct
    public void init() {
        loadUsers();
    }

    public VpnUser auth(String username, String password) {
        VpnUser user = users.get(username);
        if (user == null) {
            logger.severe(String.format("Local user %s not found", username));
            return null;
        }

        boolean authSeccessful = checkHash(user.getPasswordHash(), password);
        if (authSeccessful)
            logger.info(String.format("Authentication for user %s successful", username));
        else
            logger.severe(String.format("Authentication for user %s failed", username));

        return authSeccessful ? user : null;
    }

    public VpnUser addUser(String userName, String password) {
        VpnUser user = null;

        if (!users.containsKey(userName)) {
            user = new VpnUser(userName);
            user.setPasswordHash(createSaltedHash(password));
            user.setUserType(VpnUser.UserType.UT_LOCAL);
            users.put(userName, user);

            saveUsers();
        }
        else {
            logger.severe(String.format("Cannot add local user %s, because user already exists.", userName));
        }

        return user;
    }

    public VpnUser getUser(String username) {
        return users.get(username);
    }

    public void saveUsers() {
        logger.info(String.format("Writing localUsers to %s", usersFilePath));
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(usersFilePath));

            for (HashMap.Entry<String, VpnUser> entry: users.entrySet()) {
                VpnUser user = entry.getValue();

                writer.println(user.getUsername() + ":" + user.getPasswordHash());
            }
            writer.close();
        }
        catch (IOException ex) {
            logger.severe(String.format("Error writing local users: %s",
                   ex.getMessage()));
        }
    }

    public String createSaltedHash(String password) {
        String ret = null;

        try {
            byte[] salt = new byte[16];
            Random random = new Random();
            random.nextBytes(salt);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            digest.update(salt);
            byte[] hash = digest.digest(password.getBytes("UTF-8"));

            ret = DatatypeConverter.printBase64Binary(salt) + "$" + DatatypeConverter.printBase64Binary(hash);
        }
        catch (Exception ex) {
            logger.severe(String.format("Cannot create salted hash: %s", ex.getMessage()));
        }

        return ret;
    }

    public boolean checkHash(String saltedHash, String password) {
        boolean ret = false;

        String saltStr = saltedHash.split("\\$")[0];
        String hashStr = saltedHash.split("\\$")[1];

        try {
            byte[] salt = DatatypeConverter.parseBase64Binary(saltStr);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            digest.update(salt);
            String hash2 = DatatypeConverter.printBase64Binary(digest.digest(password.getBytes("UTF-8")));

            ret = hashStr.equals(hash2);
            if (ret)
                logger.info("Password matches hash");
            else
                logger.warning("Password doesn't match hash");
        }
        catch (Exception ex) {
              logger.severe(String.format("Cannot compare hash and password: %s", ex.getMessage()));
        }

        return ret;
    }

    public boolean isValid() {
        boolean valid = users.containsKey("admin");
        if (valid)
            logger.info("localUsers is valid");
        else
            logger.warning("ocalUsers is invalid");
        return valid;
    }

    public List<VpnUser> getUsers() {
        return new LinkedList<>(users.values());
    }

    public void removeUser(String username) {
        users.remove(username);
    }

    public void createTables()
            throws IOException, SQLException, ClassNotFoundException
    {
        logger.info("Creating tables for propertiesStorage...");
        String resourceName = "create-local-users-and-roles.sql";
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
}

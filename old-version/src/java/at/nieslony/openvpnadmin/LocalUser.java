/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.LocalUserFactory;
import java.io.Serializable;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author claas
 */
public class LocalUser
        extends AbstractUser
        implements Serializable
{
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    private LocalUserFactory localUsers;
    private String id;
    private String password;

    public LocalUser(LocalUserFactory luf, String username) {
        localUsers = luf;
        setUsername(username);
    }

    public LocalUser(LocalUserFactory luf, ResultSet result)
            throws SQLException
    {
        localUsers = luf;
        setUsername(result.getString("username"));
        setId(result.getString("id"));
        setEmail(result.getString("email"));
        setFullName(result.getString("fullName"));
        setGivenName(result.getString("givenName"));
        setSurName(result.getString("surName"));
        setPasswordHash(result.getString("password"));
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public void save()
            throws SQLException, ClassNotFoundException
    {
        Connection con = localUsers.getDatabaseConnection();

        Statement stm = con.createStatement();
        String sql = String.format(
                "UPDATE users " +
                "SET email = '%s', surName = '%s', givenName = '%s', fullName = '%s', password = '%s' " +
                "WHERE id = '%s'",
                getEmail(), getSurName(), getGivenName(), getFullName(), password,
                id);
        logger.info(String.format("SQL update: %s", sql));
        stm.executeUpdate(sql);
        stm.close();
    }

    @Override
    public void setPassword(String pwd) {
        password = createSaltedHash(pwd);
    }

    public void setPasswordHash(String pwd) {
        password = pwd;
    }

    @Override
    public boolean auth(String password) {
        return checkHash(this.password, password);
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
        if (saltedHash == null) {
            logger.warning("I don't have a password hash => check failed");
            return false;
        }
        if (password == null) {
            logger.warning("Empry password suuplied => check failed");
            return false;
        }

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
                logger.warning(String.format("Password doesn't match hash: '%s' ≠ '%s'",
                        saltedHash, hash2));
        }
        catch (Exception ex) {
              logger.severe(String.format("Cannot compare hash and password: %s", ex.getMessage()));
        }

        return ret;
    }

    @Override
    public String getUserTypeStr() {
        return "Local";
    }
}

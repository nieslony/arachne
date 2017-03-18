/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.beans;

import at.nieslony.openvpnadmin.Role;
import at.nieslony.openvpnadmin.RoleRule;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedProperty;

/**
 *
 * @author claas
 */
@ManagedBean
@ApplicationScoped
public class Roles implements Serializable {
    Map<String, Role> roles = new HashMap<>();
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @ManagedProperty(value = "#{folderFactory}")
    FolderFactory folderFactory;

    public void setFolderFactory(FolderFactory ff) {
        this.folderFactory = ff;
    }

    /**
     * Creates a new instance of Roles
     */
    public Roles() {
    }

    @PostConstruct
    public void init() {
        logger.info("Loading roles...");
        roles.put("admin", new Role("admin"));
        roles.put("user", new Role("user"));

        for (Role role : roles.values()) {
            String filename = folderFactory.getRolesDir() + "/" + role.getName();
            try (FileReader fr = new FileReader(filename)) {
                role.load(fr);
            }
            catch (IOException ex) {
                logger.severe(String.format("Error reading %s: %s", filename, ex.getMessage()));
            }
        }
    }

    public boolean hasUserRole(String username, String rolename) {
        Role role = roles.get(rolename);
        if (role == null) {
            logger.warning(String.format("Unknown role: %s", rolename));
            return false;
        }

        return role.isAssumedByUser(username);
    }

    public List<Role> getRoles() {
        LinkedList<Role> rs = new LinkedList<>(roles.values());
        return rs;
    }

    public void save() {
        for (Role role : roles.values()) {
            try (FileWriter fw = new FileWriter(folderFactory.getRolesDir() + "/" + role.getName())) {
                role.save(fw);
            }
            catch (IOException ex) {

            }
        }
    }

    public void addRule(String rolename, RoleRule rule) {
        Role role = roles.get(rolename);
        if (role == null) {
            logger.severe(String.format("Cannot add rule to non existing role %s",
                    rolename));
            return;
        }
        role.addRule(rule);
    }

    public void removeRuleFromRole(Role role, RoleRule rule) {
        role.removeRole(rule);
    }
}

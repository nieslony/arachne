/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.ldap;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
public class LdapGroup {

    private static final Logger logger = LoggerFactory.getLogger(LdapGroup.class);

    private String dn;
    private String name;
    private String description;
    private List<String> members;

    public void setMembers(String[] members) {
        this.members = Arrays.asList(members);
    }

    public boolean hasMember(LdapUser user) {
        if (members.contains(user.getUsername())) {
            logger.info("User %s is member of %s".formatted(user.getUsername(), dn));
            return true;
        }
        if (members.contains(user.getDn())) {
            logger.info("User %s is member of %s".formatted(user.getDn(), dn));
            return true;
        }
        logger.info("User %s is not member of %s".formatted(user.getDn(), dn));
        return false;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.ldap;

import at.nieslony.arachne.users.ArachneUser;
import java.util.Arrays;
import java.util.LinkedList;
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
        if (members != null) {
            this.members = Arrays.asList(members);
        } else {
            this.members = new LinkedList<>();
        }
    }

    public boolean hasMember(ArachneUser user) {
        if (!user.getExternalProvider().equals(LdapUserSource.getName())) {
            logger.info("User %s is not a LDAP user".formatted(user.getUsername()));
        }
        if (members.contains(user.getUsername())) {
            logger.info("User %s is member of %s".formatted(user.getUsername(), dn));
            return true;
        }
        if (members.contains(user.getExternalId())) {
            logger.info("User %s is member of %s".formatted(user.getExternalId(), dn));
            return true;
        }
        logger.info("User %s is not member of %s".formatted(user.getExternalId(), dn));
        return false;
    }
}

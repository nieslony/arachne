/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.ldap;

import at.nieslony.arachne.users.UserModel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
@Slf4j
public class LdapGroup {

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

    public boolean hasMember(UserModel user) {
        if (!user.getExternalProvider().equals(LdapUserSource.getName())) {
            log.info("User %s is not a LDAP user".formatted(user.getUsername()));
        }
        if (members.contains(user.getUsername())) {
            log.info("User %s is member of %s".formatted(user.getUsername(), dn));
            return true;
        }
        if (members.contains(user.getExternalId())) {
            log.info("User %s is member of %s".formatted(user.getExternalId(), dn));
            return true;
        }
        log.info("User %s is not member of %s".formatted(user.getExternalId(), dn));
        return false;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.ldap;

import at.nieslony.arachne.settings.Settings;
import java.util.HashMap;
import java.util.Map;
import javax.naming.directory.Attribute;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
public class LdapUser {

    private static final Logger logger = LoggerFactory.getLogger(LdapUser.class);

    public LdapUser(Settings settings, String username) {
        LdapSettings ldapSettings = new LdapSettings(settings);
        LdapTemplate ldap;
        try {
            ldap = ldapSettings.getLdapTemplate();
        } catch (Exception ex) {
            return;
        }
        String filter = ldapSettings.getUsersFilter(username);
        logger.info("LDAP filter: " + filter);
        var result = ldap.search(
                ldapSettings.getUsersOu(),
                filter,
                (AttributesMapper<Map<String, String>>) attrs -> {
                    Map<String, String> userInfo = new HashMap<>();
                    Attribute attr;
                    attr = attrs.get(ldapSettings.getUsersAttrUsername());
                    if (attr != null) {
                        userInfo.put("uid", attr.get().toString());
                    }
                    attr = attrs.get(ldapSettings.getUsersAttrDisplayName());
                    if (attr != null) {
                        userInfo.put("displayName", attr.get().toString());
                    }
                    attr = attrs.get(ldapSettings.getUsersAttrEmail());
                    if (attr != null) {
                        userInfo.put("mail", attr.get().toString());
                    }
                    return userInfo;
                }
        );

        this.username = result.get(0).get("uid");
        this.displayName = result.get(0).get("displayName");
        this.email = result.get(0).get("mail");
    }

    private String username;
    private String displayName;
    private String email;
}

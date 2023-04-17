/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

import at.nieslony.arachne.ldap.LdapUser;
import at.nieslony.arachne.ldap.LdapUserCacheModel;
import java.util.Collection;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

@Getter
@Setter
public class ArachneUserDetails extends User {

    private String displayName;

    public ArachneUserDetails(
            ArachneUser arachneUser,
            Set<String> roles
    ) {
        super(
                arachneUser.getUsername(),
                arachneUser.getPassword(),
                rolesToGrantedAuthorities(roles)
        );

        displayName = arachneUser.getDisplayName();
    }

    public ArachneUserDetails(
            LdapUser ldapUser,
            Set<String> roles
    ) {
        super(
                ldapUser.getUsername(),
                "",
                rolesToGrantedAuthorities(roles)
        );

        displayName = ldapUser.getDisplayName();
    }

    public ArachneUserDetails(LdapUserCacheModel lucm) {
        super(
                lucm.getUsername(),
                "",
                rolesToGrantedAuthorities(lucm.getRoles())
        );

        displayName = lucm.getDisplayName();
    }

    static private Collection<? extends GrantedAuthority>
            rolesToGrantedAuthorities(Set<String> roles) {
        return roles
                .stream()
                .map(e -> new SimpleGrantedAuthority("ROLE_" + e))
                .toList();
    }
}

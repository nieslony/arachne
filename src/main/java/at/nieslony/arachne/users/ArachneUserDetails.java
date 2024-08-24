/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

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

    private UserModel user;

    public ArachneUserDetails(UserModel arachneUser) {
        super(
                arachneUser.getUsername(),
                arachneUser.getPassword(),
                rolesToGrantedAuthorities(arachneUser.getRoles())
        );

        this.user = arachneUser;
    }

    public String getDisplayName() {
        return user.getDisplayName();
    }

    static private Collection<? extends GrantedAuthority>
            rolesToGrantedAuthorities(Set<String> roles) {
        return roles
                .stream()
                .map(e -> new SimpleGrantedAuthority("ROLE_" + e))
                .toList();
    }
}

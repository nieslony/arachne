/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.ldap;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.LastModifiedDate;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "ldap_user_cache")
public class LdapUserCacheModel {

    public LdapUserCacheModel() {
    }

    public void update(LdapUser ldapUser, Set<String> roles) {
        this.lastModified = new Date();
        this.username = ldapUser.getUsername();
        this.dn = ldapUser.getDn();
        this.displayName = ldapUser.getDisplayName();
        this.email = ldapUser.getEmail();
        this.roles = roles;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @NotNull
    @LastModifiedDate
    private Date lastModified;

    @NotNull
    private String username;

    @NotNull
    private String dn;

    private String email;
    private String displayName;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles;

    public boolean isExpired(int maxAgeMins) {
        Date now = new Date();
        Calendar expired = Calendar.getInstance();
        expired.setTime(lastModified);
        expired.add(Calendar.MINUTE, maxAgeMins);

        return now.after(expired.getTime());
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

import at.nieslony.arachne.apiindex.ShowApiDetails;
import at.nieslony.arachne.roles.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ToString
@Builder
@Entity
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "users")
@ShowApiDetails
public class UserModel implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(UserModel.class);

    public UserModel(String username, String password, String displayName, String email) {
        this.username = username;
        setPassword(password);
        this.displayName = displayName;
        this.email = email;
        this.expirationEnforced = false;
    }

    public UserModel() {
        this.expirationEnforced = false;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column
    private String displayName;

    @Column
    @JsonIgnore
    @ToString.Exclude
    private String password;

    @JsonSetter("password")
    public void setEncryptedPassword(String password) {
        this.password = password;
    }

    public void setPassword(String password) {
        this.password = new BCryptPasswordEncoder().encode(password);
    }

    @Column
    private String email;

    @Column
    private String externalId;

    @Column
    private String externalProvider;

    @Column
    private Date lastModified;

    @Column
    private boolean expirationEnforced;

    @ElementCollection(fetch = FetchType.EAGER)
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    @PrePersist
    @PreUpdate
    public void onSave() {
        logger.info("Resetting expiration date");
        lastModified = new Date();
    }

    public boolean isExpired(int maxAgeMins) {
        if (expirationEnforced) {
            logger.info("User expiration enforced");
            return true;
        }
        if (lastModified == null) {
            logger.info("never modified => expired");
            return true;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(lastModified);
        cal.add(Calendar.MINUTE, maxAgeMins);

        Calendar now = Calendar.getInstance();
        return cal.before(now);
    }

    public void update(UserModel user) {
        this.displayName = user.getDisplayName();
        this.email = user.getEmail();
        this.expirationEnforced = false;
        this.lastModified = new Date();
        this.roles.addAll(user.getRoles());
    }

    @JsonIgnore
    public Set<String> getRolesWithName() {
        Set<String> roleNames = new HashSet<>();
        roles.forEach((role) -> {
            roleNames.add(Role.valueOf(role).toString());
        });

        return roleNames;
    }
}

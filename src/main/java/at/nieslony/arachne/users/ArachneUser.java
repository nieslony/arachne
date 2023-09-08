/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

import at.nieslony.arachne.roles.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class ArachneUser implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(ArachneUser.class);

    public ArachneUser(String username, String password, String displayName, String email) {
        this.username = username;
        setPassword(password);
        this.displayName = displayName;
        this.email = email;
        this.expirationEnforced = false;
    }

    public ArachneUser() {
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
    @Setter(AccessLevel.NONE)
    private String password;

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
        lastModified = new Date();
    }

    public boolean isExpired(int maxAgeMins) {
        if (expirationEnforced) {
            logger.info("User expiration enforced");
            return true;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(lastModified);
        cal.add(Calendar.MINUTE, maxAgeMins);

        Calendar now = Calendar.getInstance();
        return cal.before(now);
    }

    public void update(ArachneUser user) {
        this.displayName = user.getDisplayName();
        this.email = user.getEmail();
        this.expirationEnforced = false;
        this.lastModified = new Date();
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

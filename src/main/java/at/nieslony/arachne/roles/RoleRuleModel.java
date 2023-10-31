/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.roles;

import at.nieslony.arachne.usermatcher.UserMatcher;
import at.nieslony.arachne.usermatcher.UserMatcherDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.util.CastUtils;

/**
 *
 * @author claas
 */
@Entity
@Table(name = "roleRules")
@Getter
@Setter
@ToString
public class RoleRuleModel implements Serializable {

    public RoleRuleModel(
            Class<? extends UserMatcher> userMatcherClass,
            String parameter,
            Role role
    ) {
        this.userMatcherClassName = userMatcherClass.getName();
        this.parameter = parameter;
        this.role = role;
    }

    public RoleRuleModel() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String userMatcherClassName;

    @JsonIgnore
    public String getRoleRuleDescription() {
        try {
            Class userMatcherClass = Class.forName(userMatcherClassName);
            if (userMatcherClass.isAnnotationPresent(
                    UserMatcherDescription.class)) {
                UserMatcherDescription descr
                        = CastUtils.cast(userMatcherClass.getAnnotation(UserMatcherDescription.class));
                return descr.description();
            } else {
                return this.getClass().getName();
            }
        } catch (ClassNotFoundException ex) {
            return "???";
        }
    }

    @Column(nullable = false)
    private String parameter;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @JsonIgnore
    public String getRoleReadable() {
        return role.toString();
    }

    @Column
    private String description;
}

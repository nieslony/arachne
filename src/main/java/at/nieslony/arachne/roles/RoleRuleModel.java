/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.roles;

import at.nieslony.arachne.users.UserMatcher;
import at.nieslony.arachne.users.UserMatcherDescription;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Entity
@Table(name = "roleRules")
@Data
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

    public String getRoleRuleDescription() {
        try {
            Class userMatcherClass = Class.forName(userMatcherClassName);
            if (userMatcherClass.isAnnotationPresent(UserMatcherDescription.class)) {
                UserMatcherDescription descr
                        = (UserMatcherDescription) userMatcherClass.getAnnotation(UserMatcherDescription.class);
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

    public String getRoleReadable() {
        return role.toString();
    }

    @Column
    private String description;
}

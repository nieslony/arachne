/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.roles;

import at.nieslony.arachne.usermatcher.UserMatcherInfo;
import jakarta.annotation.security.RolesAllowed;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author claas
 */
@RestController
@RequestMapping("/api/roles")
public class RoleRestController {

    private static final String MSG_RULE_NOT_FOUND = "Role rule with id %d not found";

    @Autowired
    private RoleRuleRepository roleRuleRepository;

    @Autowired
    private RolesCollector rolesCollector;

    @GetMapping("/rules")
    @RolesAllowed(value = {"ADMIN"})
    public Map<String, Object> findAllRoleRules() {
        Map<String, Object> map = new HashMap<>();
        List<RoleRuleModel> roleRules = roleRuleRepository.findAll();
        map.put("data", roleRules);
        return map;
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    @RolesAllowed(value = {"ADMIN"})
    public RoleRuleModel createRoleRule(@RequestBody RoleRuleModel roleRule) {
        return roleRuleRepository.save(roleRule);
    }

    @PutMapping("/rules/{id}")
    @RolesAllowed(value = {"ADMIN"})
    public RoleRuleModel updateRoleRule(@RequestBody RoleRuleModel roleRule, @PathVariable Long id) {
        roleRuleRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                MSG_RULE_NOT_FOUND.formatted(id)));
        return roleRuleRepository.save(roleRule);
    }

    @DeleteMapping("/rules/{id}")
    @RolesAllowed(value = {"ADMIN"})
    public ResponseEntity<Long> deleteRoleRule(@PathVariable Long id) {
        roleRuleRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                MSG_RULE_NOT_FOUND.formatted(id)));
        roleRuleRepository.deleteById(id);
        return new ResponseEntity<>(id, HttpStatus.OK);
    }

    @GetMapping("/user_matchers")
    @RolesAllowed(value = {"ADMIN"})
    public List<UserMatcherInfo> userMatchers() {
        return rolesCollector.getAllUserMatcherInfo();
    }

    @Getter
    @RolesAllowed(value = {"ADMIN"})
    public class RoleInfo {

        public RoleInfo(Role r) {
            this.value = r.name();
            this.description = r.toString();
        }

        private final String value;
        private final String description;
    }

    @GetMapping
    @RolesAllowed(value = {"ADMIN"})
    public List<RoleInfo> roles() {
        List<RoleInfo> roles = new LinkedList<>();
        for (Role r : Role.values()) {
            roles.add(new RoleInfo(r));
        }
        return roles;
    }
}

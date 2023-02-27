/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.users;

import at.nieslony.arachne.roles.RolesCollector;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/api/users")
public class UserRestController {

    private static final Logger logger = LoggerFactory.getLogger(UserRestController.class);
    private static final String MSG_USER_NOT_FOUND = "User with id %d not found";
    private static final String MSG_USER_ALREADY_EXISTS = "User %s already exists";
    private static final String MSG_USERNAME_EMPTY = "Username cannot empty";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolesCollector rolesCollector;

    @GetMapping
    public Map<String, Object> findAll() {
        Map<String, Object> root = new HashMap<>();

        List<Map<String, Object>> users = new LinkedList<>();
        for (ArachneUser arachneUser : userRepository.findAll()) {
            Map<String, Object> user = new HashMap<>();
            user.put("details", arachneUser);

            Set<String> roles
                    = rolesCollector.findRoleDescriptionsForUser(arachneUser.getUsername());
            user.put("roles", roles);

            users.add(user);
        }

        root.put("data", users);
        return root;
    }

    @GetMapping("/{id}")
    public ArachneUser findUser(@PathVariable Long id) {
        ArachneUser user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                MSG_USER_NOT_FOUND.formatted(id)));
        return user;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArachneUser create(@RequestBody ArachneUser user) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    MSG_USERNAME_EMPTY);
        }
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    MSG_USER_ALREADY_EXISTS.formatted(user.getUsername())
            );
        }

        return userRepository.save(user);
    }

    @PutMapping("/{id}")
    public ArachneUser update(@RequestBody ArachneUser newUser, @PathVariable Long id) {
        ArachneUser user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                MSG_USER_NOT_FOUND.formatted(id)));

        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    MSG_USERNAME_EMPTY);
        }
        if (!user.getUsername().equals(newUser.getUsername())
                && userRepository.findByUsername(newUser.getUsername()) != null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    MSG_USER_ALREADY_EXISTS.formatted(newUser.getUsername())
            );
        }

        return userRepository.save(newUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Long> delete(@PathVariable Long id) {
        userRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                MSG_USER_NOT_FOUND.formatted(id)));
        userRepository.deleteById(id);
        return new ResponseEntity<>(id, HttpStatus.OK);
    }
}

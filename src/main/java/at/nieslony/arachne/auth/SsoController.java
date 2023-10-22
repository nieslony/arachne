/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.auth;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * @author claas
 */
@Controller
public class SsoController {

    @GetMapping("/sso")
    @AnonymousAllowed
    public ResponseEntity<String> sso() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.getSession().invalidate();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/arachne");

        ResponseEntity<String> response
                = new ResponseEntity<>(
                        "Redirect",
                        headers,
                        HttpStatus.PERMANENT_REDIRECT
                );

        return response;

    }
}

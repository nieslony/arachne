/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.auth;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author claas
 */
@RestController
public class Unauthenticated {

    private static final org.slf4j.Logger logger
            = LoggerFactory.getLogger(LoginOrSetupView.class);

    @GetMapping("/unauthorized")
    @AnonymousAllowed
    public ResponseEntity<String> unauthorized() {
        logger.info("Unauthorized");
        return ResponseEntity
                .status(HttpStatusCode.valueOf(401))
                .headers((headers) -> {
                    headers.add("WWW-Authenticate", "Negotiate");
                })
                .body("""
                      <html>
                      <head>
                      </head>
                      <body>
                      <h1>Unauthorized</h1>
                      <p>You will be redirected to <a href="/arachne/login">login page</a>.</p>
                      </body>
                      </html>
                      """);

//                          <meta http-equiv="Refresh" content="1; url='/arachne/login'" />
    }
}

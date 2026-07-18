/*
 * Copyright (C) 2024 claas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.nieslony.arachne;

import at.nieslony.arachne.users.UserModel;
import at.nieslony.arachne.users.UserRepository;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author claas
 */
@RestController
@Slf4j
public class ThemeRestController {

    @Autowired
    UserRepository userRepository;

    private static final String LIGHT_STYLE
            = """
            """;

    private static final String DARK_STYLE
            = """
                --aura-content-color-scheme: dark;
                --aura-notification-color-scheme: dark;
                color-scheme: dark;
            """;
    private static final String MIXED_STYLE
            = """
                --aura-content-color-scheme: light dark;
                --aura-notification-color-scheme: dark;
                color-scheme: dark;
            """;
    private static final String AUTO_STYLE
            = """
                --aura-content-color-scheme: light dark;
                --aura-notification-color-scheme: light dark;
                color-scheme: light dark;
            """;
    private static final String STYLE
            = """
            html {
                --aura-accent-color-dark: #34D399;
                --aura-accent-color-light: #009966;
                --aura-background-color-dark: #171717;
                --aura-base-font-size: 15;
                --aura-contrast-level: 2;

            %s
            }
            vaadin-list-box {
                background: var(--aura-surface-color);
                border: 1px solid var(--aura-accent-border-color);
                border-radius: var(--vaadin-radius-l);
                //padding: var(--vaadin-padding-l);
            }
            """;

    @GetMapping("/theme/styles.css")
    @AnonymousAllowed
    public String getStyles() {
        UserModel.ThemeVariant themeVariant = UserModel.ThemeVariant.Auto;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String username = authentication.getName();
            UserModel user = userRepository.findByUsername(username);
            if (user != null && user.getThemeVariant() != null) {
                themeVariant = user.getThemeVariant();
            }
        }

        return STYLE.formatted(switch (themeVariant) {
            case Auto ->
                AUTO_STYLE;
            case Dark ->
                DARK_STYLE;
            case Light ->
                LIGHT_STYLE;
            case Mixed ->
                MIXED_STYLE;
        });
    }

    @GetMapping("/icons/arachne.png")
    @ResponseBody
    @AnonymousAllowed
    public ResponseEntity<InputStreamResource> getFaviconAsPng(@RequestParam int size) {
        try {
            String fileName = "/icons/arachne_%d.png".formatted(size);
            InputStream in = new ClassPathResource(fileName).getInputStream();

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(new InputStreamResource(in));
        } catch (IOException ex) {
            log.error("Cannot load icon icons/arachne.png: " + ex.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/icons/arachne_dark.png")
    @ResponseBody
    @AnonymousAllowed
    public ResponseEntity<InputStreamResource> getDarkFaviconAsPng(@RequestParam int size) {
        try {
            String fileName = "/icons/arachne_dark_%d.png".formatted(size);
            InputStream in = new ClassPathResource(fileName).getInputStream();

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(new InputStreamResource(in));
        } catch (IOException ex) {
            log.error("Cannot load icon icons/arachne_dark.png: " + ex.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}

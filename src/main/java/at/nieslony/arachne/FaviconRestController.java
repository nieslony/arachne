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

import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author claas
 */
@RestController
@Slf4j
public class FaviconRestController {

    @GetMapping("/icons/arachne.png")
    @ResponseBody
    @AnonymousAllowed
    public ResponseEntity<InputStreamResource> getFaviconAsPng() {
        try {
            InputStream in = new ClassPathResource("/icons/arachne.png").getInputStream();

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(new InputStreamResource(in));
        } catch (IOException ex) {
            log.error("Cannot load icon icons/arachne.png: " + ex.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}

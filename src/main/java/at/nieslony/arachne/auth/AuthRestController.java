/*
 * Copyright (C) 2023 claas
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
package at.nieslony.arachne.auth;

import at.nieslony.arachne.apiindex.ShowApiDetails;
import at.nieslony.arachne.auth.token.TokenController;
import at.nieslony.arachne.users.ArachneUserDetails;
import jakarta.annotation.security.RolesAllowed;
import java.util.Calendar;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author claas
 */
@RestController
public class AuthRestController {

    @Autowired
    TokenController tokenController;

    @Getter
    @Setter
    @AllArgsConstructor
    @ShowApiDetails
    public class AuthResult {

        private String status;
        private String tokenValidUntil;
        private String apiAuthToken;
    }

    private static final Logger logger = LoggerFactory.getLogger(AuthRestController.class);

    private Date createValidUntilDate(String vu) {
        Calendar validUntilCal = Calendar.getInstance();
        logger.info("User authenticated for VPN access");
        try {
            if (vu == null) {
                validUntilCal.add(Calendar.MINUTE, 10);
            } else if (vu.endsWith("min")) {
                validUntilCal.add(
                        Calendar.MINUTE,
                        Integer.parseInt(
                                vu.substring(0, vu.length() - 3)
                        )
                );
            } else if (vu.endsWith("sec")) {
                validUntilCal.add(
                        Calendar.SECOND,
                        Integer.parseInt(
                                vu.substring(0, vu.length() - 3)
                        )
                );
            } else if (vu.endsWith("h")) {
                validUntilCal.add(
                        Calendar.HOUR,
                        Integer.parseInt(
                                vu.substring(0, vu.length() - 1)
                        )
                );
            } else {
                String msg = "Invalid time unit. Only sec, min and h are allowed";
                logger.error(msg);
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        msg
                );
            }
        } catch (NumberFormatException ex) {
            String msg = "Unvalid number format: " + vu;
            logger.error(msg);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    msg
            );
        }
        Calendar latest = Calendar.getInstance();
        latest.add(Calendar.HOUR, 24);
        if (validUntilCal.after(latest)) {
            String msg = "Requested tocked validity time too long";
            logger.error(msg);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    msg
            );
        }

        return validUntilCal.getTime();
    }

    @GetMapping("/api/auth")
    @RolesAllowed(value = {"USER"})
    public AuthResult auth(
            @RequestParam(required = false, defaultValue = "10min") String validTime,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Date validUntil = createValidUntilDate(validTime);
        String apiAuthToken;
        if (userDetails instanceof ArachneUserDetails arachneUserDetails) {
            apiAuthToken = tokenController.createToken(
                    arachneUserDetails.getUser(),
                    validUntil
            );
        } else {
            apiAuthToken = "";
        }

        AuthResult authResult = new AuthResult(
                "Authenticated",
                validUntil.toString(),
                apiAuthToken
        );
        return authResult;
    }
}

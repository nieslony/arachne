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
package at.nieslony.arachne.auth.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 *
 * @author claas
 */
@Slf4j
@Service
public class BearerAuthenticationProvider implements AuthenticationProvider {

    private final TokenController tokenController;

    public BearerAuthenticationProvider(TokenController tokenController) {
        this.tokenController = tokenController;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.info("authenticate");
        if (authentication instanceof BearerAuthenticationToken authToken) {
            log.info("Authentication %s token of type %s granted"
                    .formatted(authToken.toString(), authToken.getClass().getName())
            );
            return authToken;
        }
        log.info("authentication of type %s not supported "
                + authentication.getClass().getName()
        );

        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        boolean supported = authentication.isAssignableFrom(BearerAuthenticationToken.class);
        log.info("authentication with %s supported: %b"
                .formatted(authentication.toString(), supported));

        return supported;
    }
}

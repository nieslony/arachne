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
package at.nieslony.arachne.utils.validators;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 */
@Slf4j
public class HostnameValidator implements Validator<String> {

    private boolean emptyAllowed;
    private boolean ipAllowed;
    private boolean resolvableRequired;

    public HostnameValidator() {
        this.emptyAllowed = true;
        this.ipAllowed = false;
        this.resolvableRequired = false;
    }

    public HostnameValidator withEmptyAllowed(boolean emptyAllowed) {
        this.emptyAllowed = emptyAllowed;
        return this;
    }

    public HostnameValidator withIpAllowed(boolean ipAllowed) {
        this.ipAllowed = ipAllowed;
        return this;
    }

    public HostnameValidator withResolvableRequired(boolean resolveRequired) {
        this.resolvableRequired = resolveRequired;
        return this;
    }

    @Override
    public ValidationResult apply(String hostname, ValueContext vc) {
        if (hostname == null || hostname.equals("")) {
            if (emptyAllowed) {
                log.debug("Empty value allowed");
                return ValidationResult.ok();
            } else {
                log.debug("Empty value not allowed");
                return ValidationResult.error("Hostname expected");
            }
        }

        log.debug("Validating " + hostname);
        Pattern pattern = Pattern.compile(
                "(?=^.{1,253}$)(^((?!-)[a-zA-Z0-9-]{1,63}(?<!-)\\.)+[a-zA-Z]{2,63}$)"
        );
        Matcher matcher = pattern.matcher(hostname);
        if (matcher.find()) {
            if (resolvableRequired) {
                try {
                    InetAddress.getByName(hostname);
                } catch (UnknownHostException ex) {
                    return ValidationResult.error("Cannot resolve hostname ");
                }
            }
            log.debug(hostname + " is a valid hostname");
            return ValidationResult.ok();
        }
        if (ipAllowed) {
            String[] bytes = hostname.split("\\.");
            if (bytes.length != 4) {
                return ValidationResult.error(getErrorMsg());
            }
            try {
                for (String b : bytes) {
                    int intVal = Integer.parseInt(b);
                    if (intVal < 0 || intVal > 255) {
                        return ValidationResult.error(getErrorMsg());
                    }
                }
            } catch (NumberFormatException ex) {
                return ValidationResult.error(getErrorMsg());
            }
            log.debug(hostname + " is a valid IP address");
            return ValidationResult.ok();
        }

        return ValidationResult.error(getErrorMsg());
    }

    private String getErrorMsg() {
        return ipAllowed
                ? "Not a valid hostname or IP address"
                : "  Not a valid hostname";
    }
}

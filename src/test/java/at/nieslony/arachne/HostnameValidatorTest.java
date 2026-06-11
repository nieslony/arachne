/*
 * Copyright (C) 2026 claas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package at.nieslony.arachne;

import at.nieslony.arachne.utils.validators.HostnameValidator;
import com.vaadin.flow.data.binder.ValidationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author claas
 */
public class HostnameValidatorTest {

    @Test
    public void testHostname() {
        HostnameValidator validator = new HostnameValidator();

        Assertions.assertEquals(
                validator.apply("", null),
                ValidationResult.ok()
        );
        Assertions.assertEquals(
                validator.apply("example.com", null),
                ValidationResult.ok()
        );
        Assertions.assertNotEquals(
                validator.apply("1.1.1.1", null),
                ValidationResult.ok()
        );
        Assertions.assertNotEquals(
                validator.apply("1.1.1", null),
                ValidationResult.ok()
        );
    }

    @Test
    public void testHostnameWithIpAllowed() {
        HostnameValidator validator = new HostnameValidator()
                .withIpAllowed(true);

        Assertions.assertEquals(
                validator.apply("", null),
                ValidationResult.ok()
        );
        Assertions.assertEquals(
                validator.apply("example.com", null),
                ValidationResult.ok()
        );
        Assertions.assertEquals(
                validator.apply("1.1.1.1", null),
                ValidationResult.ok()
        );
        Assertions.assertNotEquals(
                validator.apply("1.1.1", null),
                ValidationResult.ok()
        );
    }

    @Test
    public void testHostnameWithEmptyNotAllowed() {
        HostnameValidator validator = new HostnameValidator()
                .withEmptyAllowed(false);

        Assertions.assertNotEquals(
                validator.apply("", null),
                ValidationResult.ok()
        );
        Assertions.assertEquals(
                validator.apply("example.com", null),
                ValidationResult.ok()
        );
        Assertions.assertNotEquals(
                validator.apply("1.1.1.1", null),
                ValidationResult.ok()
        );
        Assertions.assertNotEquals(
                validator.apply("1.1.1", null),
                ValidationResult.ok()
        );
    }

    @Test
    public void testHostnameWithResolvableRequired() {
        HostnameValidator validator = new HostnameValidator()
                .withResolvableRequired(true);

        Assertions.assertEquals(
                validator.apply("example.com", null),
                ValidationResult.ok()
        );
        Assertions.assertNotEquals(
                validator.apply("example.commm", null),
                ValidationResult.ok()
        );
    }
}

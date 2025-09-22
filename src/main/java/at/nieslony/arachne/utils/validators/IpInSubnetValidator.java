/*
 * Copyright (C) 2025 claas
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
package at.nieslony.arachne.utils.validators;

import at.nieslony.arachne.utils.net.NetUtils;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import java.net.UnknownHostException;
import java.util.function.Supplier;

/**
 *
 * @author claas
 */
public class IpInSubnetValidator implements Validator<String> {

    public record Subnet(String network, int mask) {

        @Override
        public String toString() {
            return network + "/" + String.valueOf(mask);
        }
    }
    ;

    final private Supplier<Subnet> subnetSupplier;

    public IpInSubnetValidator(Supplier<Subnet> subnetSupplier) {
        this.subnetSupplier = subnetSupplier;
    }

    public IpInSubnetValidator(String subnet, int mask) {
        this.subnetSupplier = () -> new Subnet(subnet, mask);
    }

    @Override
    public ValidationResult apply(String value, ValueContext context) {
        String subnetStr = subnetSupplier.get().toString();

        try {
            if (NetUtils.isSubnetOf(value, subnetStr)) {
                return ValidationResult.ok();
            } else {
                return ValidationResult.error("Subnet %s doesn't contain %s"
                        .formatted(subnetStr, value)
                );
            }
        } catch (UnknownHostException ex) {
            return ValidationResult.error("Cannot resolve " + value);
        }
    }
}

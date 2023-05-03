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
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.utils.TransportProtocol;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

/**
 *
 * @author claas
 */
@Getter
@Setter
@Entity
@Table(name = "firewallWhat")
public class FirewallWhat {

    public enum Type {
        OnePort("One Port"),
        PortRange("Port Range"),
        Service("Firewalld Service");

        final private String label;

        private Type(String l) {
            label = l;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @Cascade(CascadeType.SAVE_UPDATE)
    @JoinColumn(name = "firewallRules_id")
    private FirewallRuleModel firewallRule;

    private Type type = Type.OnePort;
    private int port = 1;
    private TransportProtocol portProtocol;
    private int portFrom = 1;
    private int portTo = 65535;
    private TransportProtocol portRangeProtocol;
    private String service = "";

    @Override
    public String toString() {
        return switch (type) {
            case OnePort ->
                "%d/%s".formatted(port, portProtocol.name());
            case PortRange ->
                "%d-%d/%s".formatted(portFrom, portTo, portRangeProtocol.name());
            case Service ->
                FirewalldService.getService(service).getShortDescription();
        };
    }
}

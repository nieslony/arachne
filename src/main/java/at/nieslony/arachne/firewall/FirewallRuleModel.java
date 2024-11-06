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

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author claas
 */
@Entity
@Getter
@Setter
@ToString
@Table(name = "firewallRules")
public class FirewallRuleModel {

    public enum VpnType {
        USER, SITE
    }

    public enum RuleDirection {
        INCOMING, OUTGOING
    }

    public FirewallRuleModel() {
    }

    public FirewallRuleModel(VpnType vpnType, RuleDirection ruleDirection) {
        this.vpnType = vpnType;
        this.ruleDirection = ruleDirection;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column
    private String description;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private VpnType vpnType;

    @Column(nullable = false)
    private RuleDirection ruleDirection;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FirewallWho> who;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FirewallWhere> from;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FirewallWhere> to;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FirewallWhat> what;

    @JsonIgnore
    public Boolean isValid() {
        if (who == null || to == null || what == null) {
            return false;
        }
        return !who.isEmpty() && !to.isEmpty() && !what.isEmpty();
    }
}

// who  from   to
//   y          y     user incoming
//   y     y          user outgoing
//              y     site incoming
//         y          site outgoing

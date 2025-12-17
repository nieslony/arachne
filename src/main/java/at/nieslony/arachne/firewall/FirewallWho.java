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

import at.nieslony.arachne.usermatcher.EverybodyMatcher;
import at.nieslony.arachne.usermatcher.UserMatcher;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author claas
 */
@Getter
@Setter
@Entity
@Table(name = "firewallWho")
public class FirewallWho {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "firewallRules_id")
    private FirewallRuleModel firewallRule;

    private String userMatcherClassName = "";
    private String parameter = "";

    @Override
    public String toString() {
        return UserMatcher.getMatcherDetails(
                getUserMatcherClassName(),
                getParameter());
    }

    @JsonIgnore
    public boolean isEverybody() {
        return userMatcherClassName.equals(EverybodyMatcher.class.getName());
    }

    public static FirewallWho createEverybody() {
        FirewallWho who = new FirewallWho();
        who.setUserMatcherClassName(EverybodyMatcher.class.getName());
        return who;
    }
}

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

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author claas
 */
public interface FirewallRuleRepository extends JpaRepository<FirewallRuleModel, Long> {

    List<FirewallRuleModel> findAllByVpnTypeAndRuleDirection(
            FirewallRuleModel.VpnType vpnType,
            FirewallRuleModel.RuleDirection ruleDirection
    );

    List<FirewallRuleModel> findAllByVpnTypeAndRuleDirection(
            FirewallRuleModel.VpnType vpnType,
            FirewallRuleModel.RuleDirection ruleDirection,
            Pageable pageable
    );

    List<FirewallRuleModel> findAllByVpnType(
            FirewallRuleModel.VpnType vpnType
    );

    long countByVpnTypeAndRuleDirection(
            FirewallRuleModel.VpnType vpnType,
            FirewallRuleModel.RuleDirection ruleDirection
    );
}

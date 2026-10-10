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
package at.nieslony.arachne.tasks.scheduled;

import at.nieslony.arachne.firewall.FirewallRuleModel;
import at.nieslony.arachne.firewall.FirewallService;
import at.nieslony.arachne.openvpn.management.OpenVpnManagementService;
import at.nieslony.arachne.tasks.RecurringTaskDescription;
import at.nieslony.arachne.tasks.Task;
import at.nieslony.arachne.tasks.TaskDescription;
import at.nieslony.arachne.utils.ArachneTimeUnit;
import at.nieslony.arachne.utils.FolderFactory;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.springframework.beans.factory.BeanFactory;

/**
 *
 * @author claas
 */
@TaskDescription(name = "Refresh Firewall Wheres")
@RecurringTaskDescription(timeUnit = ArachneTimeUnit.HOUR, defaulnterval = 1)
@Slf4j
public class RefreshFirewallWheres extends Task {

    @Override
    public String run(BeanFactory beanFactory) {
        FirewallService firewallService = beanFactory.getBean(FirewallService.class);
        FolderFactory folderFactory = beanFactory.getBean(FolderFactory.class);
        OpenVpnManagementService openVpnManagementService = beanFactory.getBean(OpenVpnManagementService.class);

        List<String> msgs = new LinkedList<>();

        switch (openVpnManagementService.getSiteManagement().getManagementConnectionStatus()) {
            case Connected -> {
                String fn = folderFactory.getFirewallUpdatesPath(FirewallRuleModel.VpnType.SITE);
                try {
                    firewallService.writeRules(fn, FirewallRuleModel.VpnType.SITE);
                    msgs.add("Site Firewall where updated.");
                } catch (IOException | JSONException ex) {
                    msgs.add("Cannot update site wheres: " + ex.getMessage());
                }
            }
            case Disconnected -> {
                msgs.add("Site Server is disconnected");
            }
            case Hold -> {
                msgs.add("Site Server is in Hold Status");
            }
        }

        switch (openVpnManagementService.getUserManagement().getManagementConnectionStatus()) {
            case Connected -> {
                String fn = folderFactory.getFirewallUpdatesPath(FirewallRuleModel.VpnType.USER);
                try {
                    firewallService.writeRules(fn, FirewallRuleModel.VpnType.USER);
                    msgs.add("User Firewall where updated.");
                } catch (IOException | JSONException ex) {
                    msgs.add("Cannot update user wheres: " + ex.getMessage());
                }
            }
            case Disconnected -> {
                msgs.add("User Server is disconnected");
            }
            case Hold -> {
                msgs.add("User Server is in Hold Status");
            }
        }

        return String.join(". ", msgs);
    }
}

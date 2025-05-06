/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import at.nieslony.arachne.firewall.basicsettings.IcmpRules;
import at.nieslony.arachne.apiindex.ShowApiDetails;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author claas
 */
@Getter
@Setter
@ShowApiDetails
public class UserFirewallEverybodyRules {

    private IcmpRules icmpRules;
    private List<FirewallRestController.RichRule> richRules;
}

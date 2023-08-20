/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.firewall;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author claas
 */
@Getter
@Setter
public class FirewallEverybodyRules {

    private FirewallBasicsSettings.IcmpRules icmpRules;
    private List<FirewallRestController.RichRule> richRules;
}

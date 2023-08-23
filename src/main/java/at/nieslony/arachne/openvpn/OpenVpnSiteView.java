/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.openvpn;

import at.nieslony.arachne.ViewTemplate;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 *
 * @author claas
 */
@Route(value = "openvpn-site2site", layout = ViewTemplate.class)
@PageTitle("OpenVPN Site 2 Site | Arachne")
@RolesAllowed("ADMIN")
public class OpenVpnSiteView extends VerticalLayout {

    public OpenVpnSiteView() {
    }
}

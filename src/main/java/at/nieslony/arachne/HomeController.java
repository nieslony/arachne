/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 *
 * @author claas
 */
@Route(value = "")
@RolesAllowed({"ADMIN", "USER"})
//@Controller
public class HomeController extends VerticalLayout {

    public HomeController() {
        add(new Text("Redirecting..."));
    }

    @Override
    public void onAttach(AttachEvent event) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (userDetails
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            UI.getCurrent().navigate(AdminHome.class);
        } else {
            UI.getCurrent().navigate(UserHome.class);
        }
    }
}

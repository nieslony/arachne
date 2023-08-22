/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 *
 * @author claas
 */
@Route(value = "user-home")
@PageTitle("Arachne | User Home")
@RolesAllowed("USER")
public class UserHome extends VerticalLayout {

    public UserHome() {
        H1 header = new H1("Arachne - User Home");

        add(
                header
        );
    }
}

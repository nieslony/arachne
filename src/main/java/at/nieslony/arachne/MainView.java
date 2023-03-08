/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

/**
 *
 * @author claas
 */
@Route(value = "", layout = ViewTemplate.class)
@PageTitle("Arachne")
@PermitAll
public class MainView extends VerticalLayout {

    public MainView() {
        Text text = new Text("Home");

        add(text);
    }
}

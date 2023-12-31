/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.validators;

import com.vaadin.flow.data.validator.RegexpValidator;

/**
 *
 * @author claas
 */
public class SerivePrincipalValidator extends RegexpValidator {

    public SerivePrincipalValidator() {
        super(
                "Not a valid Service Principal",
                "^"
                + "([a-zA-Z]+)"
                + "/"
                + "[a-z][a-z0-9\\-]*(\\.[a-z][a-z0-9\\-]*)*"
                + "@[A-Z0-9]+(\\.[A-Z0-9]+)*"
                + "$"
        );
    }

}

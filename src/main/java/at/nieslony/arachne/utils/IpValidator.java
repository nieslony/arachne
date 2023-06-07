/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;

/**
 *
 * @author claas
 */
public class IpValidator implements Validator<String> {

    private final String ERROR_MSG = "Not a valid IP address";

    @Override
    public ValidationResult apply(String value, ValueContext vc) {
        String[] bytes = value.split("\\.");
        if (bytes.length != 4) {
            return ValidationResult.error(ERROR_MSG);
        }
        try {
            for (String b : bytes) {
                int intVal = Integer.parseInt(b);
                if (intVal < 0 || intVal > 255) {
                    return ValidationResult.error(ERROR_MSG);
                }
            }
        } catch (NumberFormatException ex) {
            return ValidationResult.error(ERROR_MSG);
        }

        return ValidationResult.ok();
    }

}

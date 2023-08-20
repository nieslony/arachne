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
public class RequiredIfVisibleValidator implements Validator<String> {

    @Override
    public ValidationResult apply(String value, ValueContext vc) {
        if (!vc.getComponent().get().isVisible()) {
            return ValidationResult.ok();
        }
        if (value != null && !value.isEmpty()) {
            return ValidationResult.ok();
        } else {
            return ValidationResult.error("Value required");
        }
    }
}

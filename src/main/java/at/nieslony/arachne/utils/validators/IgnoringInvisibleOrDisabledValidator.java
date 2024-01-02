/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.validators;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;

/**
 *
 * @author claas
 * @param <Value>
 */
public class IgnoringInvisibleOrDisabledValidator<Value> implements Validator<Value> {

    private final Validator<Value> validator;

    public IgnoringInvisibleOrDisabledValidator(Validator<Value> validator) {
        this.validator = validator;
    }

    @Override
    public ValidationResult apply(Value t, ValueContext vc) {
        Component comp = vc.getComponent().get();
        if (comp == null) {
            return ValidationResult.error("Null Component");
        }

        if (comp instanceof HasEnabled) {
            if (!((HasEnabled) comp).isEnabled()) {
                return ValidationResult.ok();
            }
        }

        if (!comp.isVisible()) {
            return ValidationResult.ok();
        } else {
            return validator.apply(t, vc);
        }
    }
}

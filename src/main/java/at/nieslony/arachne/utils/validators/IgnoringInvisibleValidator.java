/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.validators;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;

/**
 *
 * @author claas
 * @param <Value>
 */
public class IgnoringInvisibleValidator<Value> implements Validator<Value> {

    private final Validator<Value> validator;

    public IgnoringInvisibleValidator(Validator<Value> validator) {
        this.validator = validator;
    }

    @Override
    public ValidationResult apply(Value t, ValueContext vc) {
        if (!vc.getComponent().get().isVisible()) {
            return ValidationResult.ok();
        } else {
            return validator.apply(t, vc);
        }
    }
}

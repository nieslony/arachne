/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.validators;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import java.util.function.Supplier;

/**
 *
 * @author claas
 * @param <Value>
 */
public class ConditionalValidator<Value> implements Validator<Value> {

    final private Supplier<Boolean> ignored;
    final private Validator<Value> validator;

    public ConditionalValidator(Supplier<Boolean> ignored, Validator<Value> validator) {
        this.ignored = ignored;
        this.validator = validator;
    }

    @Override
    public ValidationResult apply(Value t, ValueContext vc) {
        if (ignored.get()) {
            return ValidationResult.ok();
        } else {
            return validator.apply(t, vc);
        }
    }
}

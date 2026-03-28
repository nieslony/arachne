/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.validators;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author claas
 * @param <Value>
 */
@Slf4j
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

        String labelStr;
        if (comp instanceof HasLabel hasLabel) {
            labelStr = hasLabel.getLabel();
        } else {
            labelStr = "unknown";
        }

        if (comp instanceof HasEnabled hasEnabled) {
            if (!hasEnabled.isEnabled()) {
                log.debug(labelStr + " is disabled => OK");
                return ValidationResult.ok();
            }
        }

        if (!comp.isVisible()) {
            log.debug(labelStr + " is not visible  => OK");
            return ValidationResult.ok();
        } else {
            var ret = validator.apply(t, vc);
            if (ret.isError()) {
                log.debug(labelStr + " is invalid" + ret.getErrorMessage());
            } else {
                log.debug(labelStr + " is valid");
            }
            return ret;
        }
    }
}

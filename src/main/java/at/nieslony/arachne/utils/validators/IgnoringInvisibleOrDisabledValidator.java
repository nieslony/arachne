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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 * @param <Value>
 */
public class IgnoringInvisibleOrDisabledValidator<Value> implements Validator<Value> {

    private static final Logger logger = LoggerFactory.getLogger(IgnoringInvisibleOrDisabledValidator.class);

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
                logger.debug(labelStr + " is disabled => OK");
                return ValidationResult.ok();
            }
        }

        if (!comp.isVisible()) {
            logger.debug(labelStr + " is not visible  => OK");
            return ValidationResult.ok();
        } else {
            var ret = validator.apply(t, vc);
            if (ret.isError()) {
                logger.debug(labelStr + " is invalid" + ret.getErrorMessage());
            } else {
                logger.debug(labelStr + " is valid");
            }
            return ret;
        }
    }
}

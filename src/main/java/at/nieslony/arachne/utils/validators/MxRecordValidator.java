/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.validators;

import at.nieslony.arachne.utils.net.NetUtils;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import javax.naming.NamingException;

/**
 *
 * @author claas
 */
public class MxRecordValidator implements Validator<String> {

    @Override
    public ValidationResult apply(String domain, ValueContext vc) {
        try {
            return NetUtils.mxLookup(domain).isEmpty()
                    ? ValidationResult.error("No MX record for domain found")
                    : ValidationResult.ok();
        } catch (NamingException ex) {
            return ValidationResult.error(
                    "Cannot find MX record for domain %s: %s"
                            .formatted(domain, ex.getMessage())
            );
        }
    }

}

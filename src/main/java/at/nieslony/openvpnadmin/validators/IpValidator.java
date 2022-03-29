/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin.validators;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.ValidatorException;
import java.util.Map;
import org.primefaces.validate.ClientValidator;

/**
 *
 * @author claas
 */
@FacesValidator("ipValidator")
public class IpValidator  implements jakarta.faces.validator.Validator<Object>, ClientValidator {

    @Override
    public void validate(FacesContext fc, UIComponent uic, Object o)
            throws ValidatorException {
        String ip = o.toString();

        String[] octets = ip.split("\\.");
        if (octets == null || octets.length != 4)
                throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error",
                        "IP address must consist of exacly 4 octets"));

        for (String oct : octets) {
            Integer value = Integer.valueOf(oct);
            if (value < 0 | value > 255)
                throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error",
                        "Octet value must be between 0 and 255"));
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        return null;
    }

    @Override
    public String getValidatorId() {
        return "ipValidator";
    }

}

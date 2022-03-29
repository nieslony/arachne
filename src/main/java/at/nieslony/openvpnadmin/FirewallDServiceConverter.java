/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.nieslony.openvpnadmin;

import at.nieslony.openvpnadmin.beans.FirewallDServices;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import java.util.logging.Logger;

/**
 *
 * @author claas
 */
@FacesConverter(value = "firewallDServiceConverter")
public class FirewallDServiceConverter implements Converter<Object> {
    private static final transient Logger logger = Logger.getLogger(java.util.logging.ConsoleHandler.class.toString());

    @Override
    public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
        if(value != null && value.trim().length() > 0) {
            try {
                int index = Integer.valueOf(value);
                FirewallDService s = FirewallDServices.getAllServices().get(index);

                return s;
            }
            catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                logger.warning(String.format("%s is not a valid index", value));
                return null;
            }
        }
        else {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext fc, UIComponent uic, Object object) {
        if(object != null) {
            int index = FirewallDServices.getAllServices().indexOf(object);

            logger.info(String.format("Object index; %d", index));

            return String.valueOf(index);
        }
        else {
            return null;
        }
    }
}

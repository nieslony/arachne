/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils.validators;

import at.nieslony.arachne.utils.net.NetMask;
import at.nieslony.arachne.utils.net.NetUtils;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author claas
 */
public class SubnetValidator implements Validator<String> {

    private final static String ERROR_MSG = "Not a valid network address";
    private static final Logger logger = LoggerFactory.getLogger(SubnetValidator.class);

    private final boolean emptyAllowed;
    private final Supplier<Integer> getPrefix;
    private final Supplier<NetMask> getNetMask;

    public SubnetValidator(Supplier<Integer> getPrefix) {
        this.emptyAllowed = true;
        this.getPrefix = getPrefix;
        this.getNetMask = null;
    }

    public SubnetValidator(boolean emptyAllowed, Supplier<Integer> getPrefix) {
        this.emptyAllowed = emptyAllowed;
        this.getPrefix = getPrefix;
        this.getNetMask = null;
    }

    @Override
    public ValidationResult apply(String value, ValueContext vc) {
        if (emptyAllowed && (value == null || value.equals(""))) {
            return ValidationResult.ok();
        }

        String[] bytesStr = value.split("\\.");
        if (bytesStr.length != 4) {
            return ValidationResult.error(ERROR_MSG);
        }
        byte[] bytes = new byte[4];
        try {
            for (int i = 0; i < 4; i++) {
                int intVal = Integer.parseInt(bytesStr[i]);
                if (intVal < 0 || intVal > 255) {
                    return ValidationResult.error(ERROR_MSG);
                }
                bytes[i] = (byte) intVal;
            }
        } catch (NumberFormatException ex) {
            return ValidationResult.error(ERROR_MSG);
        }

        int prefix = getPrefix.get();
        if (prefix < 1 || prefix > 32) {
            return ValidationResult.error("prefix not in 1…32");
        }

        try {
            Inet4Address network = (Inet4Address) Inet4Address.getByAddress(bytes);
            Inet4Address masked = NetUtils.maskInet4Address(network, prefix);
            if (!network.equals(masked)) {
                return ValidationResult.error("Network does not match prefix");
            }
        } catch (UnknownHostException ex) {
            String msg = "Cannot get IPv4 address: " + ex.getMessage();
            logger.error(msg);
            return ValidationResult.error("Internal error");
        }

        return ValidationResult.ok();
    }
}

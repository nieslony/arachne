/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.utils;

import com.vaadin.flow.function.SerializablePredicate;

/**
 *
 * @author claas
 */
public class IpValidator implements SerializablePredicate<String> {

    @Override
    public boolean test(String value) {
        if (value == null || value.equals("")) {
            return true;
        }

        String[] bytes = value.split("\\.");
        if (bytes.length != 4) {
            return false;
        }
        try {
            for (String b : bytes) {
                int intVal = Integer.parseInt(b);
                if (intVal < 0 || intVal > 255) {
                    return false;
                }
            }
        } catch (NumberFormatException ex) {
            return false;
        }

        return true;
    }

}

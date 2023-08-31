package at.nieslony.arachne.settings;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.regex.Pattern;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author claas
 */
abstract public class AbstractSettingsGroup {

    public void load(Settings settings) throws SettingsException {
        for (Field field : getClass().getDeclaredFields()) {
            field.setAccessible(true);
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) | Modifier.isFinal(modifiers)) {
                continue;
            }
            String n = groupName() + "." + makeKey(field.getName());
            var v = settings.get(n, field.getType());
            if (v != null) {
                try {
                    field.set(this, v);
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw new SettingsException(
                            "Cannot set: " + ex.getMessage(),
                            ex
                    );
                }
            }
        }
    }

    public void save(Settings settings) throws SettingsException {
        for (Field field : getClass().getDeclaredFields()) {
            field.setAccessible(true);
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) | Modifier.isFinal(modifiers)) {
                continue;
            }
            Class c = field.getType();
            String n = groupName() + "." + makeKey(field.getName());
            try {
                var value = field.get(this);
                settings.put(n, value);
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw new SettingsException(
                        "Cannot get value: %s" + ex.getMessage(),
                        ex
                );
            }
        }
    }

    private String makeKey(String s) {
        return Pattern
                .compile("([A-Z])")
                .matcher(s)
                .replaceAll((match) -> "-" + match.group().toLowerCase())
                .replaceAll("\\.-", ".")
                .replaceFirst("^-", "");
    }

    protected String groupName() {
        return makeKey(getClass().getSimpleName());
    }
}

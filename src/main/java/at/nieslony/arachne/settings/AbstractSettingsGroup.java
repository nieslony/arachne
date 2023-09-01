package at.nieslony.arachne.settings;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author claas
 */
abstract public class AbstractSettingsGroup {

    protected List<Field> getSettingFields() {
        return Stream.of(getClass().getDeclaredFields())
                .map((field) -> {
                    field.setAccessible(true);
                    return field;
                })
                .filter((field) -> {
                    int modifiers = field.getModifiers();
                    return !Modifier.isStatic(modifiers)
                            && !Modifier.isFinal(modifiers);
                })
                .toList();
    }

    public void load(Settings settings) throws SettingsException {
        for (Field field : getSettingFields()) {
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
        for (Field field : getSettingFields()) {
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

    public void delete(Settings settings) {
        for (Field field : getSettingFields()) {
            String n = groupName() + "." + makeKey(field.getName());
            settings.delete(n);
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

package at.nieslony.arachne.settings;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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

    protected List<Field> getSettingFields() {
        List<Field> fields = new LinkedList<>();
        for (Class<?> cl = getClass();
                !cl.equals(AbstractSettingsGroup.class);
                cl = cl.getSuperclass()) {
            fields.addAll(Arrays.asList(cl.getDeclaredFields()));
        }
        return fields.stream()
                .peek((f) -> f.setAccessible(true))
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

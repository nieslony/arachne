/*
 * Copyright (C) 2023 claas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.nieslony.arachne.settings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
public class Settings {

    @Autowired
    private SettingsRepository settingsRepository;

    public <T extends AbstractSettingsGroup> T getSettings(Class<T> c)
            throws SettingsException {
        try {
            T obj = c.getDeclaredConstructor().newInstance();
            obj.load(this);
            return obj;
        } catch (IllegalAccessException | IllegalArgumentException
                | InstantiationException | InvocationTargetException
                | NoSuchMethodException | SecurityException
                | SettingsException ex) {
            throw new SettingsException(
                    "Cannot create settings class %s".formatted(c.getName()),
                    ex);
        }
    }

    private static <T> byte[] makeBytes(T value) throws SettingsException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new SettingsException("Cannot serialize value:" + ex.getMessage());
        }
    }

    private static Object fromBytes(byte[] s) throws SettingsException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(s);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            throw new SettingsException("Cannot deserialize value", ex);
        }
    }

    public <T extends Object> T get(String setting, Class<? extends Object> c)
            throws SettingsException {
        Optional<SettingsModel> value = settingsRepository.findBySetting(setting);
        if (value.isEmpty()) {
            return null;
        }
        return (T) fromBytes(value.get().getContent());
    }

    public <T> void put(String setting, T value) throws SettingsException {
        byte[] bytes = makeBytes(value);
        Optional<SettingsModel> settingsModel = settingsRepository.findBySetting(setting);

        if (settingsModel.isPresent()) {
            settingsRepository.save(settingsModel.get().setContent(bytes));
        } else {
            settingsRepository.save(new SettingsModel(setting, bytes));
        }
    }
}

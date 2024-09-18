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
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.data.util.CastUtils;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
public class Settings {

    private static final Logger logger = LoggerFactory.getLogger(Settings.class);

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private ServerProperties serverProperties;

    private static Settings settings = null;

    public Settings() {
        settings = this;
    }

    public static Settings getInstance() {
        return settings;
    }

    public <T extends AbstractSettingsGroup> T getSettings(Class<T> c) {
        T obj;
        try {
            obj = c.getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException | IllegalArgumentException
                | InstantiationException | InvocationTargetException
                | NoSuchMethodException | SecurityException ex) {
            if (ex.getCause() != null) {
                logger.error(
                        "Cannot instanciate %s: exception=%s cause=%s msg=%s"
                                .formatted(
                                        c.getName(),
                                        ex.getClass().getName(),
                                        ex.getCause().getClass().getName(),
                                        ex.getCause().getMessage()
                                )
                );
            } else {
                logger.error(
                        "Cannot instanciate %s: %s %s"
                                .formatted(
                                        c.getName(),
                                        ex.getClass().getName(),
                                        ex.getMessage()
                                )
                );
            }
            return null;
        }
        try {
            obj.load(this);
        } catch (SettingsException ex) {
            logger.error("Cannot load %s: %s"
                    .formatted(c.getName(), ex.getMessage()
                    )
            );
        }
        return obj;
    }

    private static <T> String makeBase64(T value) throws SettingsException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
            byte[] bytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException ex) {
            throw new SettingsException("Cannot serialize value:" + ex.getMessage());
        }
    }

    private static <T extends Serializable> T fromBase64(String s)
            throws ClassNotFoundException, IOException {
        byte[] bytes = Base64.getDecoder().decode(s);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object obj = ois.readObject();
        return CastUtils.cast(obj);
    }

    public <T extends Object> T fromString(
            String value,
            Class<? extends Object> c
    ) throws SettingsException {
        if (c.equals(String.class)) {
            return CastUtils.cast(value);
        }
        if (c.isPrimitive()) {
            if (c.equals(boolean.class)) {
                return CastUtils.cast(Boolean.valueOf(value));
            }
            if (c.equals(byte.class)) {
                return CastUtils.cast(Byte.valueOf(value));
            }
            if (c.equals(short.class)) {
                return CastUtils.cast(Short.valueOf(value));
            }
            if (c.equals(int.class)) {
                return CastUtils.cast(Integer.valueOf(value));
            }
            if (c.equals(long.class)) {
                return CastUtils.cast(Long.valueOf(value));
            }
            if (c.equals(float.class)) {
                return CastUtils.cast(Float.valueOf(value));
            }
            if (c.equals(double.class)) {
                return CastUtils.cast(Double.valueOf(value));
            }
        }
        try {
            Method valueOf = c.getMethod("valueOf", String.class);
            Object obj = valueOf.invoke(null, value);
            return CastUtils.cast(obj);

        } catch (IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException ex) {
            throw new SettingsException("Cannot invoke valueOf", ex);
        }
    }

    public <T extends Object> T get(
            String setting,
            Class<? extends Object> c
    ) throws SettingsException {
        Optional<SettingsModel> value = settingsRepository.findBySetting(setting);
        if (value.isEmpty()) {
            return null;
        }
        String ValueStr = value.get().getContent();
        if (ValueStr == null) {
            return null;
        }

        logger.info("Parsing value: " + ValueStr);
        try {
            return fromBase64(ValueStr);
        } catch (ClassNotFoundException | IOException | IllegalArgumentException ex) {
            logger.info("Cannot deserialize (%s), try as string".formatted(ex.getMessage()));
            return fromString(value.get().getContent(), c);
        }
    }

    public <T extends Object> void put(String setting, T value) throws SettingsException {
        String retValue;
        if (value == null) {
            retValue = null;
        } else if (value instanceof Enum enumValue) {
            retValue = enumValue.name();
        } else if (value instanceof String strValue) {
            retValue = strValue;
        } else {
            try {
                value.getClass().getMethod("valueOf", String.class);
                retValue = value.toString();
            } catch (NoSuchMethodException | SecurityException ex) {
                retValue = makeBase64(value);
            }
        }

        Optional<SettingsModel> settingsModel = settingsRepository.findBySetting(setting);

        if (settingsModel.isPresent()) {
            settingsRepository.save(settingsModel.get().setContent(retValue));
        } else {
            settingsRepository.save(new SettingsModel(setting, retValue));
        }
    }

    public void delete(String setting) {
        settingsRepository.deleteBySetting(setting);
    }

    public ServerProperties getServerProperties() {
        return serverProperties;
    }
}

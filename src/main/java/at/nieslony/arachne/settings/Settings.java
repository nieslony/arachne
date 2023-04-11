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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;

/**
 *
 * @author claas
 */
@Component
public class Settings {

    private static Settings settings;

    @Autowired
    SettingsRepository settingsRepository;

    @Autowired
    private ServerProperties serverProperties;

    public Settings() {
        settings = this;
    }

    public static Settings getInstance() {
        return settings;
    }

    public String get(String setting, String defaultValue) {
        Optional<SettingsModel> settingsModel = settingsRepository.findBySetting(setting);
        if (settingsModel.isPresent()) {
            return settingsModel.get().getContent();
        } else {
            return defaultValue;
        }
    }

    public int getInt(String setting, int defaultValue) {
        Optional<SettingsModel> settingsModel = settingsRepository.findBySetting(setting);
        if (settingsModel.isPresent()) {
            return Integer.parseInt(settingsModel.get().getContent());
        } else {
            return defaultValue;
        }
    }

    public boolean getBoolean(String setting, boolean defaultValue) {
        Optional<SettingsModel> settingsModel = settingsRepository.findBySetting(setting);
        if (settingsModel.isPresent()) {
            return Boolean.parseBoolean(settingsModel.get().getContent());
        } else {
            return defaultValue;
        }
    }

    public List<String> getList(String setting, List<String> defaultValue) {
        Optional<SettingsModel> settingsModel = settingsRepository.findBySetting(setting);
        if (settingsModel.isPresent()) {
            return SettingsModel.splitString(settingsModel.get().getContent());
        } else {
            if (defaultValue != null) {
                return defaultValue;
            }
            return new LinkedList<>();
        }
    }

    public void put(String setting, String content) {
        Optional<SettingsModel> settingsModel = settingsRepository.findBySetting(setting);

        if (settingsModel.isPresent()) {
            settingsRepository.save(settingsModel.get().setContent(content));
        } else {
            settingsRepository.save(new SettingsModel(setting, content));
        }
    }

    public void put(String setting, int content) {
        put(setting, String.valueOf(content));
    }

    public void put(String setting, List<String> content) {
        put(setting, SettingsModel.joinList(content));
    }

    public void put(String setting, boolean content) {
        put(setting, String.valueOf(content));
    }

    public ServerProperties getServerProperties() {
        return serverProperties;
    }
}

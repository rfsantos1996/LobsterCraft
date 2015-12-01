package com.jabyftw.pacocacraft.configuration;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

/**
 * Copyright (C) 2015  Rafael Sartori for PacocaCraft Plugin
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Email address: rafael.sartori96@gmail.com
 */
public class ConfigurationFile {

    private final File configurationFile;
    private final YamlConfiguration yamlConfiguration;

    public ConfigurationFile(Plugin plugin, String fileName) throws IOException, InvalidConfigurationException {
        this.configurationFile = new File(plugin.getDataFolder(), fileName + ".yml");
        this.yamlConfiguration = new YamlConfiguration();
        loadFile();
    }

    public void saveFile() throws IOException {
        yamlConfiguration.save(configurationFile);
    }

    public void loadFile() throws IOException, InvalidConfigurationException {
        // If doesn't exist, create default
        if(!configurationFile.exists()) {
            for(ConfigValue configValue : ConfigValue.values())
                yamlConfiguration.set(configValue.getPath(), configValue.getDefaultValue());
            saveFile();
        }
        yamlConfiguration.load(configurationFile);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(ConfigValue configValue) {
        return (T) yamlConfiguration.get(configValue.getPath(), configValue.getDefaultValue());
    }

}

package com.jabyftw.lobstercraft.util;

import com.jabyftw.lobstercraft.ConfigValue;
import com.sun.istack.internal.NotNull;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

/**
 * Copyright (C) 2015  Rafael Sartori for LobsterCraft Plugin
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
public class ConfigurationFile extends YamlConfiguration {

    private final File configurationFile;

    public ConfigurationFile(@NotNull final Plugin plugin, @NotNull final String fileName) throws IOException, InvalidConfigurationException {
        super();
        this.configurationFile = new File(plugin.getDataFolder(), fileName + ".yml");
        loadFile();
    }

    public void saveFile() throws IOException {
        save(configurationFile);
    }

    public void loadFile() throws IOException, InvalidConfigurationException {
        // Load file if it exists
        if (configurationFile.exists())
            load(configurationFile);

        // Insert non-present default configurations
        for (ConfigValue configValue : ConfigValue.values())
            if (!contains(configValue.getPath()))
                set(configValue.getPath(), configValue.getDefaultValue());

        // Save changes and load file
        saveFile();
        load(configurationFile);
    }
}

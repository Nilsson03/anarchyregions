package ru.nilsson03.anarchyregions.menu.util;

import org.bukkit.configuration.ConfigurationSection;
import ru.nilsson03.library.text.util.ReplaceData;
import ru.nilsson03.anarchyregions.menu.api.CustomItem;
import ru.nilsson03.anarchyregions.menu.api.StaticCustomItem;
import ru.nilsson03.anarchyregions.menu.api.UpdatableCustomItem;
import ru.nilsson03.library.NPlugin;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MenuUtil {

    public static List<CustomItem> parseSection(NPlugin plugin, ConfigurationSection section, ReplaceData... replacesData) {
        Objects.requireNonNull(section, "Items section cannot be null");
        List<CustomItem> customItems = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection itemConfig = section.getConfigurationSection(key);
            if (itemConfig == null) {
                ConsoleLogger.warn(plugin, "Could not get item section for %s from configuration", key);
                continue;
            }

            boolean update = itemConfig.getBoolean("update", false);
            if (update) {
                UpdatableCustomItem item = new UpdatableCustomItem(itemConfig, replacesData);
                customItems.add(item);
            } else {
                StaticCustomItem item = new StaticCustomItem(itemConfig);
                customItems.add(item);
            }
        }
        return customItems;
    }
}

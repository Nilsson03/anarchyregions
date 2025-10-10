package ru.nilsson.anarchyregions;

import org.bukkit.Bukkit;

import ru.nilsson03.library.NPlugin;
import ru.nilsson03.library.bukkit.file.FileHelper;

public class CultAnarchyRegions extends NPlugin {

    private static CultAnarchyRegions instance;

    @Override
    public void enable() {
        instance = this;

        FileHelper.loadConfigurations(this,
            getDataFolder(),
            "messages.yml",
            "inventories.yml",
            "regions.yml",
            "data.yml", 
            "tempRegions.yml"
        );

    }

    @Override
    public void disable() {
        Bukkit.getScheduler().cancelTasks(this); // Останавливаем оставшиеся таски плагина, работающие через Bukkit API
    }

    public static CultAnarchyRegions getInstance() {
        return instance;
    }
}

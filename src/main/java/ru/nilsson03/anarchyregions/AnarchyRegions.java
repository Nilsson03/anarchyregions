package ru.nilsson03.anarchyregions;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import ru.nilsson03.anarchyregions.region.listener.RegionListener;
import ru.nilsson03.anarchyregions.storage.RegionStorage;
import ru.nilsson03.library.NPlugin;
import ru.nilsson03.library.bukkit.file.BukkitDirectory;
import ru.nilsson03.library.bukkit.file.FileHelper;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;

public class AnarchyRegions extends NPlugin {

    private static AnarchyRegions instance;
    private RegionStorage regionStorage;

    @Override
    public void enable() {
        instance = this;

        FileHelper.loadConfigurations(this,
            getDataFolder(),
            "messages.yml"
        );

        FileHelper.loadConfiguration(this, "regions/example.yml");

        this.fileRepository().addExcludePaths(
            "data",
            "regions"
        );

        BukkitDirectory rooDirectory = getDirectory("/");
        BukkitConfig messagesConfig = rooDirectory.getBukkitConfig("messages.yml");

         regionStorage = new RegionStorage(this);

        RegionListener regionListener = new RegionListener(regionStorage, messagesConfig);
        getServer().getPluginManager().registerEvents(regionListener, (JavaPlugin)this);

    }

    @Override
    public void disable() {
        regionStorage.saveAllRegions();
        Bukkit.getScheduler().cancelTasks(this); // Останавливаем оставшиеся таски плагина, работающие через Bukkit API
    }

    public static AnarchyRegions getInstance() {
        return instance;
    }
}

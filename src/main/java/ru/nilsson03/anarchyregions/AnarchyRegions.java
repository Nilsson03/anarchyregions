package ru.nilsson03.anarchyregions;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;

import lombok.Getter;
import ru.nilsson03.anarchyregions.command.InviteCommand;
import ru.nilsson03.anarchyregions.hologram.HologramManager;
import ru.nilsson03.anarchyregions.properties.PropertiesStorage;
import ru.nilsson03.anarchyregions.region.listener.RegionListener;
import ru.nilsson03.anarchyregions.region.manager.RegionManager;
import ru.nilsson03.anarchyregions.region.manager.RegionUpdateManager;
import ru.nilsson03.anarchyregions.region.service.RegionIndexService;
import ru.nilsson03.anarchyregions.request.RequestManager;
import ru.nilsson03.anarchyregions.request.RequestService;
import ru.nilsson03.anarchyregions.storage.RegionStorage;
import ru.nilsson03.library.NPlugin;
import ru.nilsson03.library.bukkit.file.BukkitDirectory;
import ru.nilsson03.library.bukkit.file.FileHelper;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;
import ru.nilsson03.library.bukkit.file.configuration.ParameterFile;

@Getter
public class AnarchyRegions extends NPlugin {

    @Getter
    private static AnarchyRegions instance;
    private RegionStorage regionStorage;
    private ParameterFile configFile;
    private BukkitDirectory inventoriesDirectory;
    
    private RegionUpdateManager regionUpdateService;
    private RegionManager regionManager;
    private RegionIndexService regionIndexService;

    @Override
    public void enable() {
        instance = this;

        FileHelper.loadConfigurations(this,
            getDataFolder(),
            "config.yml",
            "messages.yml"
        );

        FileHelper.loadConfigurations(this,
            getDataFolder(),
            "inventories/region-menu.yml"
        );

        FileHelper.loadConfiguration(this, "regions/example.yml");

        this.fileRepository().addExcludePaths(
            "data",
            "config.yml",
            "regions"
        );

        configFile = ParameterFile.of(this, "config.yml");

        BukkitDirectory rootDirectory = getDirectory("/");
        inventoriesDirectory = getDirectory("inventories");
        
        BukkitConfig messagesConfig = rootDirectory.getBukkitConfig("messages.yml");

        PropertiesStorage propertiesStorage = PropertiesStorage.getInstance(this);


        new HologramManager(this, configFile);

        regionStorage = new RegionStorage(this);
        Bukkit.getPluginManager().registerEvents(regionStorage, this);

        // Запускать обновление после инициализации regionStorage, дабы подписаться Bukkit.getPluginManager().registerEvents(requestListener, this);
        RequestManager requestManager = new RequestManager(configFile);
        regionManager = new RegionManager(this, configFile, regionStorage); // Тут инициализируется кэш в конструкторе, тоже нужно до стореджа
        regionIndexService = new RegionIndexService(regionManager);
        RequestService requestService = new RequestService(regionManager, requestManager, messagesConfig);
        Bukkit.getPluginManager().registerEvents(regionManager, this); 
        regionUpdateService = new RegionUpdateManager(configFile, regionManager);
        Bukkit.getPluginManager().registerEvents(regionUpdateService, this);
        regionUpdateService.startGlobalUpdateTask(); 

        regionStorage.loadRegions();
        
        RegionListener regionListener = new RegionListener(messagesConfig, propertiesStorage, regionManager);
        Bukkit.getPluginManager().registerEvents(regionListener, this);

        InviteCommand inviteCommand = new InviteCommand(regionIndexService, requestService, messagesConfig);
        PluginCommand cmd = getCommand("invite");
        cmd.setExecutor(inviteCommand);
        cmd.setTabCompleter(inviteCommand);
    }

    @Override
    public void disable() {
        if (regionUpdateService != null) {
            regionUpdateService.shutdown();
        }
        if (regionStorage != null) {
            regionStorage.saveAllRegions();
        }
        Bukkit.getScheduler().cancelTasks(this); // Останавливаем оставшиеся таски плагина, работающие через Bukkit API
    }
}

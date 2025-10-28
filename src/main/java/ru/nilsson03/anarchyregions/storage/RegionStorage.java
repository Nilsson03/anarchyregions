package ru.nilsson03.anarchyregions.storage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import lombok.Getter;
import ru.nilsson03.anarchyregions.AnarchyRegions;
import ru.nilsson03.anarchyregions.event.RegionCreatedEvent;
import ru.nilsson03.anarchyregions.event.RegionDestroyEvent;
import ru.nilsson03.anarchyregions.event.RegionPreCreateEvent;
import ru.nilsson03.anarchyregions.event.RegionsLoadEvent;
import java.util.ArrayList;
import ru.nilsson03.anarchyregions.properties.RegionProperties;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.library.bukkit.file.BukkitDirectory;
import ru.nilsson03.library.bukkit.file.FileRepository;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;
import ru.nilsson03.library.bukkit.util.loc.LocationUtil;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;

public class RegionStorage implements Listener {

    private final BukkitDirectory regionsDirectory;
    @Getter
    private final Map<UUID, Region> regions;

    public RegionStorage(AnarchyRegions plugin) throws IllegalStateException {
        FileRepository fileRepository = plugin.fileRepository();
        
        try {
            this.regionsDirectory = plugin.getDirectory("data");
            fileRepository.loadFiles(regionsDirectory, false);
        } catch (IllegalStateException e) {
            ConsoleLogger.error("anarchyregions", "Failed to load data directory: %s", e.getMessage());
            throw e;
        }

        this.regions = new HashMap<>();
    }

    public void saveAllRegions() {
        for (Region region : regions.values()) {
            saveRegion(region);
        }
    }

    public void saveRegion(Region region) {
        String regionIdString = region.getRegionId().toString();
        BukkitConfig config = regionsDirectory.addNewConfig(regionIdString);
        if (config != null) {
            FileConfiguration fileConfiguration = config.getFileConfiguration();
            fileConfiguration.set("center", LocationUtil.locationToString(region.getCenterLocation()));
            fileConfiguration.set("territory.one", LocationUtil.locationToString(region.getRegionTerritory().getMinimumPoint()));
            fileConfiguration.set("territory.two", LocationUtil.locationToString(region.getRegionTerritory().getMaximumPoint()));
            fileConfiguration.set("owner", region.getRegionOwner().toString());
            fileConfiguration.set("durability", region.getDurability());
            fileConfiguration.set("block-type", region.getBlockType().name());
            config.saveConfiguration();
        }
    }

    @EventHandler
    public void onRegionDestroy(RegionDestroyEvent event) {
        Region region = event.getRegion();
        removeRegion(region);
    }

    @EventHandler
    public void onRegionPreCreate(RegionPreCreateEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Location location = event.getLocation();
        UUID regionOwner = event.getOwner();
        RegionProperties properties = event.getProperties();

        createRegion(location, regionOwner, properties);
    }

    public void removeRegion(Region region) {
        UUID regionId = region.getRegionId();
        regions.remove(regionId);

        String regionIdString = regionId.toString() + ".yml";

        if (regionsDirectory.containsFileWithName(regionIdString)) {
            BukkitConfig config = regionsDirectory.getBukkitConfig(regionIdString);
            regionsDirectory.removeAndDeleteConfig(config);
            ConsoleLogger.debug("anarchyregions", "Removed region %s from data directory", regionIdString);
        }
    }

    public Region createRegion(Location location, UUID regionOwner, RegionProperties properties) {
        Region region = new Region(location, regionOwner, properties);
        regions.put(region.getRegionId(), region);
        ConsoleLogger.debug("anarchyregions", "Created region %s in storage", region.getRegionId().toString());
        RegionCreatedEvent event = new RegionCreatedEvent(region);
        Bukkit.getPluginManager().callEvent(event);
        return region;
    }

    public void loadRegions() {
        long startTime = System.currentTimeMillis();
        List<Region> loadedRegions = new ArrayList<>();
        
        for (BukkitConfig config : regionsDirectory.getCached()) {
            UUID uuid = UUID.fromString(config.getName().replace(".yml", ""));
            Region region = new Region(uuid, config);
            regions.put(uuid, region);
            loadedRegions.add(region);
        }
        
        if (!loadedRegions.isEmpty()) {
            RegionsLoadEvent event = new RegionsLoadEvent(loadedRegions);
            ConsoleLogger.info("anarchyregions", "Calling RegionsLoadEvent for %d regions", loadedRegions.size());
            Bukkit.getPluginManager().callEvent(event);
            ConsoleLogger.info("anarchyregions", "RegionsLoadEvent called and processed");
        } else {
            ConsoleLogger.info("anarchyregions", "No regions to load, skipping event");
        }
        
        ConsoleLogger.info("anarchyregions", "Loaded %d regions in %dms", regions.size(), System.currentTimeMillis() - startTime);
    }

    @Nullable
    public Region getRegion(Location location) {
        return regions.values().stream()
                .filter(region -> region.getRegionTerritory().contains(location))
                .findFirst()
                .orElse(null);
    }
    
    @Nullable
    public Region getRegion(UUID regionId) {
        return regions.get(regionId);
    }

    public void reload() {
        long startTime = System.currentTimeMillis();
        regions.clear();
        regionsDirectory.reloadAll();
        loadRegions();
        ConsoleLogger.debug("anarchyregions", "Reloaded region storage in %dms", System.currentTimeMillis() - startTime);
    }
}

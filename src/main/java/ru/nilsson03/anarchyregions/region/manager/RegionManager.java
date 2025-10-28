package ru.nilsson03.anarchyregions.region.manager;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import org.bukkit.event.Listener;

import ru.nilsson03.anarchyregions.AnarchyRegions;
import ru.nilsson03.anarchyregions.event.RegionPreCreateEvent;
import ru.nilsson03.anarchyregions.properties.RegionProperties;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.anarchyregions.region.cache.RegionCache;
import ru.nilsson03.anarchyregions.storage.RegionStorage;
import ru.nilsson03.library.bukkit.file.configuration.ParameterFile;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;

public class RegionManager implements Listener {

    private RegionCache regionCache;
    private RegionStorage regionStorage;

    public RegionManager(AnarchyRegions plugin,ParameterFile configFile, RegionStorage regionStorage) {
        this.regionCache = new RegionCache(configFile);
        this.regionStorage = regionStorage;
        Bukkit.getPluginManager().registerEvents(regionCache, plugin);
    }

    public boolean regionExistsInLocation(Location location) {
        boolean foundInCache = regionCache.findRegionsInLocation(location) != null;
        if (foundInCache) {
            ConsoleLogger.debug("anarchyregions", "Region found in cache at %s", location.toString());
            return true;
        }
        ConsoleLogger.debug("anarchyregions", "No region found in cache at %s, checking storage", location.toString());
        return false;
    }

    public boolean isOwner(Region region, UUID uuid) {
        return region.getRegionOwner().equals(uuid);
    }

    @Nullable
    public Region getRegionByLocation(Location location) {
        Region foundRegion = regionCache.findRegionsInLocation(location);
        if (foundRegion != null) {
            ConsoleLogger.debug("anarchyregions", "Found region %s at %s", foundRegion.getRegionId().toString(), location.toString());
            return foundRegion;
        }
        ConsoleLogger.debug("anarchyregions", "No region found at %s, checking storage", location.toString());
        return regionStorage.getRegion(location);
    }

    public void createRegion(Location location, UUID owner, RegionProperties properties) {
        RegionPreCreateEvent event = new RegionPreCreateEvent(owner, location, properties);
        Bukkit.getPluginManager().callEvent(event);
    }

    public boolean wouldRegionOverlap(Location centerLocation, int radius) {
        return regionCache.wouldRegionOverlap(centerLocation, radius);
    }

    /**
     * Получить регион по его ID
     */
    @Nullable
    public Region getRegion(UUID regionId) {
        return regionStorage.getRegion(regionId);
    }

    /**
     * Проверить существование региона
     */
    public boolean regionExists(UUID regionId) {
        return regionStorage.getRegion(regionId) != null;
    }

    /**
     * Получить все регионы
     */
    public Map<UUID, Region> getAllRegions() {
        return regionStorage.getRegions();
    }
}

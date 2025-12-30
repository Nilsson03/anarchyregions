package ru.nilsson03.anarchyregions.region.manager;

import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import ru.nilsson03.anarchyregions.AnarchyRegions;
import ru.nilsson03.anarchyregions.event.RegionPreCreateEvent;
import ru.nilsson03.anarchyregions.event.RequestAcceptedEvent;
import ru.nilsson03.anarchyregions.properties.RegionProperties;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.anarchyregions.region.cache.RegionCache;
import ru.nilsson03.anarchyregions.storage.RegionStorage;
import ru.nilsson03.library.bukkit.file.configuration.ParameterFile;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;

public class RegionManager implements Listener {

    private RegionCache regionCache;
    private RegionStorage regionStorage;

    public RegionManager(AnarchyRegions plugin,
                        ParameterFile configFile, 
                        RegionStorage regionStorage) {
        this.regionCache = new RegionCache(configFile);
        this.regionStorage = regionStorage;
        Bukkit.getPluginManager().registerEvents(regionCache, plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public boolean regionExistsInLocation(Location location) {
        boolean foundInCache = regionCache.findRegionsInLocation(location) != null;
        if (foundInCache) {
            ConsoleLogger.debug("anarchyregions", "Region found in cache at %s", location.toString());
            return true;
        }
        ConsoleLogger.debug("anarchyregions", "No region found in cache at %s, checking storage", location.toString());
        Region regionInLocation = regionStorage.getRegion(location);
        if (regionInLocation != null) {
            ConsoleLogger.debug("anarchyregions", "Region found in storage at %s", location.toString());
            regionCache.updateCacheForNewRegion(regionInLocation);
            return true;
        }
        return false;
    }

    public boolean isOwner(Region region, UUID uuid) {
        return region.getRegionOwner().equals(uuid);
    }

    @Nullable
    public Region getRegionByLocation(Location location) {
        Region foundRegion = regionCache.findRegionsInLocation(location);
        if (foundRegion != null) {
            ConsoleLogger.debug("anarchyregions", "Found region %s at %s", foundRegion.getRegionId().toString(),
                    location.toString());
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

    public Set<Region> getPlayerRegions(UUID playerUuid) {
        Map<UUID, Region> regions = regionStorage.getRegions();
        return regions.values().stream()
                .filter(region -> region.getRegionOwner().equals(playerUuid))
                .collect(Collectors.toSet());
    }

    @Nullable
    public Region getRegion(UUID regionId) {
        return regionStorage.getRegion(regionId);
    }

    public boolean regionExists(UUID regionId) {
        return regionStorage.getRegion(regionId) != null;
    }

    public Map<UUID, Region> getAllRegions() {
        return regionStorage.getRegions();
    }

    @EventHandler
    public void onRequestAccepted(RequestAcceptedEvent event) {
        UUID regionId = event.getRegionUUID();
        Region region = regionStorage.getRegion(regionId);
        if (region == null) {
            return;
        }

        UUID target = event.getTarget();
        region.addMember(target);
    }
}

package ru.nilsson03.anarchyregions.region.manager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import ru.nilsson03.anarchyregions.event.RegionCreateEvent;
import ru.nilsson03.anarchyregions.event.RegionDestroyEvent;
import ru.nilsson03.anarchyregions.event.RegionsLoadEvent;
import ru.nilsson03.anarchyregions.properties.RegionProperties;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.anarchyregions.service.RegionUpdateService;
import ru.nilsson03.anarchyregions.storage.RegionStorage;
import ru.nilsson03.library.bukkit.file.configuration.ParameterFile;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;

public class RegionManager implements Listener {

    private final RegionStorage regionStorage;
    private RegionUpdateService regionUpdateService;
    
    private final LinkedHashMap<String, Set<Region>> chunkRegionCache;

    private int maxCacheSize;

    public RegionManager(RegionStorage regionStorage, ParameterFile configFile) {
        this.regionStorage = regionStorage;
        this.maxCacheSize = configFile.getValueAs("settings.region-cache-size", Integer.class);
        this.chunkRegionCache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Set<Region>> eldest) {
            boolean shouldRemove = size() > maxCacheSize;
            if (shouldRemove) {
                ConsoleLogger.debug("anarchyregions", "Removing eldest cache entry: %s (%d regions)", 
                    eldest.getKey(), eldest.getValue().size());
            }
            return shouldRemove; 
        }};
        
        ConsoleLogger.info("anarchyregions", "RegionManager initialized with cache (max size: %d)", maxCacheSize);
    }
    
    /**
     * Устанавливает сервис обновления регионов для интеграции
     */
    public void setRegionUpdateService(RegionUpdateService regionUpdateService) {
        this.regionUpdateService = regionUpdateService;
    }
    
    /**
     * Получает хранилище регионов для доступа из других сервисов
     */
    public RegionStorage getRegionStorage() {
        return regionStorage;
    }

    public boolean regionExistsInLocation(Location location) {
        String chunkKey = getChunkKey(location.getChunk());
        Set<Region> regionsInChunk = chunkRegionCache.get(chunkKey);
        
        ConsoleLogger.debug("anarchyregions", "Checking region existence at %s (chunk: %s)", 
            formatLocation(location), chunkKey);
        
        if (regionsInChunk == null) {
            ConsoleLogger.debug("anarchyregions", "No regions cached for chunk %s", chunkKey);
            return false;
        }
        
        boolean exists = regionsInChunk.stream()
                .anyMatch(region -> region.getRegionTerritory().contains(location));
        
        ConsoleLogger.debug("anarchyregions", "Region exists check result: %s (cached regions: %d)", 
            exists, regionsInChunk.size());
        
        return exists;
    }

    public boolean isOwner(Region region, UUID uuid) {
        return region.getRegionOwner().equals(uuid);
    }

    @Nullable
    public Region getRegionByLocation(Location location) {
        String chunkKey = getChunkKey(location.getChunk());
        Set<Region> regionsInChunk = chunkRegionCache.get(chunkKey);
        
        ConsoleLogger.debug("anarchyregions", "Getting region by location %s (chunk: %s)", 
            formatLocation(location), chunkKey);
        
        Region foundRegion = regionsInChunk.stream()
                .filter(region -> region.getRegionTerritory().contains(location))
                .findFirst()
                .orElse(null);
        
        if (foundRegion != null) {
            ConsoleLogger.debug("anarchyregions", "Found region %s at %s", 
                foundRegion.getRegionId().toString(), formatLocation(location));
        } else {
            ConsoleLogger.debug("anarchyregions", "No region found at %s (checked %d cached regions)", 
                formatLocation(location), regionsInChunk.size());
        }
        
        return foundRegion;
    }

    public void createRegion(Location location, UUID owner, RegionProperties properties) {
        Region createdRegion = regionStorage.createRegion(location, owner, properties);
        if (createdRegion != null) {
            RegionCreateEvent event = new RegionCreateEvent(createdRegion, location);
            Bukkit.getPluginManager().callEvent(event);
            updateCacheForNewRegion(createdRegion);
            
            // Запускаем обновления для нового региона
            if (regionUpdateService != null) {
                regionUpdateService.startRegionUpdates(createdRegion);
            }
            
            ConsoleLogger.info("anarchyregions", "Region created: %s at %s by %s", 
                properties.getDisplayName(), 
                formatLocation(location), 
                owner.toString());
        } else {
            ConsoleLogger.warn("anarchyregions", "Failed to create region at %s - region not found after creation", 
                formatLocation(location));
        }
    }

    @EventHandler
    public void onRegionDestroy(RegionDestroyEvent event) {
        Region region = event.getRegion();

        if (event.isCancelled()) {
            ConsoleLogger.info("anarchyregions", "Region destruction cancelled at %s by owner %s", 
            formatLocation(region.getCenterLocation()), 
            region.getRegionOwner().toString());
            return;
        } 

        ConsoleLogger.info("anarchyregions", "Removing region %s from cache and storage", region.getRegionId());
        removeRegionFromCache(region);
        regionStorage.removeRegion(region);
        regionUpdateService.stopRegionUpdates(region);

        ConsoleLogger.info("anarchyregions", "Region destroyed and removed at %s by owner %s", 
            formatLocation(region.getCenterLocation()), 
            region.getRegionOwner().toString());
    }

    @EventHandler
    public void onRegionsLoad(RegionsLoadEvent event) {
        List<Region> regions = event.getRegions();

        preloadRegionsToCache();

        for (Region region : regions) {
            regionUpdateService.startRegionUpdates(region);
        }
    }

    public boolean wouldRegionOverlap(Location centerLocation, int radius) {
        Set<String> affectedChunks = getAffectedChunks(centerLocation, radius);
        
        ConsoleLogger.debug("anarchyregions", "Checking region overlap at %s with radius %d (affected chunks: %d)", 
            formatLocation(centerLocation), radius, affectedChunks.size());

        int checkedRegions = 0;
        for (String chunkKey : affectedChunks) {
            Set<Region> regionsInChunk = chunkRegionCache.get(chunkKey);
            if (regionsInChunk == null) {
                ConsoleLogger.debug("anarchyregions", "No regions in chunk %s", chunkKey);
                continue;
            }
            
            ConsoleLogger.debug("anarchyregions", "Checking %d regions in chunk %s", 
                regionsInChunk.size(), chunkKey);
            
            for (Region existingRegion : regionsInChunk) {
                checkedRegions++;
                if (isRegionsOverlapping(centerLocation, radius, existingRegion)) {
                    ConsoleLogger.debug("anarchyregions", "Overlap detected with region %s at %s", 
                        existingRegion.getRegionId().toString(), 
                        formatLocation(existingRegion.getCenterLocation()));
                    return true;
                }
            }
        }
        
        ConsoleLogger.debug("anarchyregions", "No overlap detected (checked %d regions)", checkedRegions);
        return false;
    }

    public long getPlayerRegionCount(UUID playerId) {
        return regionStorage.getRegions().values()
                .stream()
                .filter(region -> region.getRegionOwner().equals(playerId))
                .count();
    }
    
    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }
    
    private Set<String> getAffectedChunks(Location centerLocation, int radius) {
        Set<String> chunks = new HashSet<>();
        Chunk centerChunk = centerLocation.getChunk();
        
        int chunkRadius = (radius / 16) + 1;
        
        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                Chunk chunk = centerLocation.getWorld().getChunkAt(
                    centerChunk.getX() + x, 
                    centerChunk.getZ() + z
                );
                chunks.add(getChunkKey(chunk));
            }
        }
        
        return chunks;
    }
    
    private boolean isRegionsOverlapping(Location centerLocation, int radius, Region existingRegion) {
        Location existingCenter = existingRegion.getCenterLocation();
        
        double distance = centerLocation.distance(existingCenter);
        int existingRadius = (existingRegion.getRegionTerritory().getMaxX() - existingRegion.getRegionTerritory().getMinX()) / 2;
        
        return distance < (radius + existingRadius);
    }
    
    public void updateCacheForNewRegion(Region region) {
        Set<String> affectedChunks = getAffectedChunks(region.getCenterLocation(), 
            (region.getRegionTerritory().getMaxX() - region.getRegionTerritory().getMinX()) / 2);
        
        ConsoleLogger.debug("anarchyregions", "Updating cache for new region %s, affected chunks: %d", 
            region.getRegionId().toString(), affectedChunks.size());
        
        for (String chunkKey : affectedChunks) {
            Set<Region> chunkRegions = chunkRegionCache.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet());
            chunkRegions.add(region);
            ConsoleLogger.debug("anarchyregions", "Added region to chunk %s (total regions in chunk: %d)", 
                chunkKey, chunkRegions.size());
        }
        
        ConsoleLogger.debug("anarchyregions", "Cache update completed. Total cached chunks: %d", 
            chunkRegionCache.size());
    }
    
    public void removeRegionFromCache(Region region) {
        ConsoleLogger.debug("anarchyregions", "Removing region %s from cache", 
            region.getRegionId().toString());
        
        int removedCount = 0;
        for (Set<Region> regions : chunkRegionCache.values()) {
            if (regions.remove(region)) {
                removedCount++;
            }
        }
        
        ConsoleLogger.debug("anarchyregions", "Removed region from %d chunks. Total cached chunks: %d", 
            removedCount, chunkRegionCache.size());
    }
    
    private void preloadRegionsToCache() {
        long startTime = System.currentTimeMillis();
        int totalRegionsInStorage = regionStorage.getRegions().size();
        
        ConsoleLogger.info("anarchyregions", "Preloading %d regions from storage to cache...", totalRegionsInStorage);
        
        for (Region region : regionStorage.getRegions().values()) {
            updateCacheForNewRegion(region);
        }
        
        ConsoleLogger.info("anarchyregions", "Preloaded %d regions to cache in %dms", 
            totalRegionsInStorage, System.currentTimeMillis() - startTime);
        
        logCacheStats();
    }
    
    public void reloadCache() {
        ConsoleLogger.info("anarchyregions", "Reloading cache from storage...");
        chunkRegionCache.clear();
        preloadRegionsToCache();
    }
    
    public void logCacheStats() {
        int totalChunks = chunkRegionCache.size();
        
        Set<Region> uniqueRegions = new HashSet<>();
        chunkRegionCache.values().forEach(uniqueRegions::addAll);
        int uniqueRegionCount = uniqueRegions.size();
        
        int totalCacheEntries = chunkRegionCache.values().stream()
                .mapToInt(Set::size)
                .sum();
        
        ConsoleLogger.info("anarchyregions", "Cache stats: %d chunks, %d unique regions, %d total cache entries", 
            totalChunks, uniqueRegionCount, totalCacheEntries);
        
        // Логируем детали по каждому чанку
        chunkRegionCache.forEach((chunkKey, regions) -> {
            ConsoleLogger.debug("anarchyregions", "Chunk %s: %d regions", 
                chunkKey, regions.size());
        });
    }
    
    private String formatLocation(Location location) {
        return String.format("%s (%d, %d, %d)", 
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ());
    }
}

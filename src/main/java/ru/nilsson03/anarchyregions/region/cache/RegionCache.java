package ru.nilsson03.anarchyregions.region.cache;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import ru.nilsson03.anarchyregions.event.RegionCreatedEvent;
import ru.nilsson03.anarchyregions.event.RegionDestroyEvent;
import ru.nilsson03.anarchyregions.event.RegionsLoadEvent;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.library.bukkit.file.configuration.ParameterFile;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;

public class RegionCache implements Listener {

    private final LinkedHashMap<String, Set<Region>> chunkRegionCache;

    public RegionCache(ParameterFile configFile) {
        int maxCacheSize = configFile.getValueAs("settings.region-cache-size", Integer.class);
        this.chunkRegionCache = new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Set<Region>> eldest) {
                boolean shouldRemove = size() > maxCacheSize;
                if (shouldRemove) {
                    ConsoleLogger.debug("anarchyregions", "Removing eldest cache entry: %s (%d regions)",
                            eldest.getKey(), eldest.getValue().size());
                }
                return shouldRemove;
            }
        };

        ConsoleLogger.info("anarchyregions", "RegionCache initialized with cache (max size: %d)", maxCacheSize);
    }

    @EventHandler
    public void onRegionsLoad(RegionsLoadEvent event) {
        preloadRegionsToCache(event.getRegions());
        logCacheStats();
    }

    @EventHandler
    public void onRegionDestroy(RegionDestroyEvent event) {
        if (event.isCancelled()) {
            return;
        }

        removeRegionFromCache(event.getRegion());
        ConsoleLogger.info("anarchyregions", "Region %s destroyed and removed from cache",
                event.getRegion().getRegionId().toString());
    }

    @EventHandler
    public void onRegionCreated(RegionCreatedEvent event) {
        updateCacheForNewRegion(event.getRegion());
        ConsoleLogger.info("anarchyregions", "Region %s created and added to cache",
                event.getRegion().getRegionId().toString());
    }

    private void preloadRegionsToCache(List<Region> regions) {
        long startTime = System.currentTimeMillis();
        int totalRegions = regions.size();

        ConsoleLogger.info("anarchyregions", "Preloading %d regions to cache...", totalRegions);

        for (Region region : regions) {
            updateCacheForNewRegion(region);
        }

        ConsoleLogger.info("anarchyregions", "Preloaded %d regions to cache in %dms",
                totalRegions, System.currentTimeMillis() - startTime);
    }

    @Nullable
    public Region findRegionsInLocation(Location location) {
        String chunkKey = getChunkKey(location.getChunk());
        Set<Region> regionsInChunk = chunkRegionCache.get(chunkKey);

        ConsoleLogger.debug("anarchyregions", "Getting region by location %s (chunk: %s)",
                location.toString(), chunkKey);

        if (regionsInChunk == null) {
            ConsoleLogger.debug("anarchyregions", "No regions in chunk %s", chunkKey);
            return null;
        }

        return regionsInChunk.stream()
                .filter(region -> region.getRegionTerritory().contains(location))
                .findFirst()
                .orElse(null);
    }

    public boolean wouldRegionOverlap(Location centerLocation, int radius) {
        Set<String> affectedChunks = getAffectedChunks(centerLocation, radius);

        ConsoleLogger.debug("anarchyregions", "Checking region overlap at %s with radius %d (affected chunks: %d)",
                centerLocation.toString(), radius, affectedChunks.size());

        int checkedRegions = 0;
        for (String chunkKey : affectedChunks) {
            Set<Region> regionsInChunk = getRegionsInChunk(chunkKey);
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
                            existingRegion.getCenterLocation().toString());
                    return true;
                }
            }
        }

        ConsoleLogger.debug("anarchyregions", "No overlap detected (checked %d regions)", checkedRegions);
        return false;
    }

    private boolean isRegionsOverlapping(Location centerLocation, int radius, Region existingRegion) {
        Location existingCenter = existingRegion.getCenterLocation();

        double distance = centerLocation.distance(existingCenter);
        int existingRadius = (existingRegion.getRegionTerritory().getMaxX()
                - existingRegion.getRegionTerritory().getMinX()) / 2;

        return distance < (radius + existingRadius);
    }

    public Set<String> getAffectedChunks(Location centerLocation, int radius) {
        Set<String> chunks = new HashSet<>();
        Chunk centerChunk = centerLocation.getChunk();

        int chunkRadius = (radius / 16) + 1;

        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                Chunk chunk = centerLocation.getWorld().getChunkAt(
                        centerChunk.getX() + x,
                        centerChunk.getZ() + z);
                chunks.add(getChunkKey(chunk));
            }
        }

        return chunks;
    }

    public void updateCacheForNewRegion(Region region) {
        Set<String> affectedChunks = getAffectedChunks(region.getCenterLocation(),
                (region.getRegionTerritory().getMaxX() - region.getRegionTerritory().getMinX()) / 2);

        ConsoleLogger.debug("anarchyregions", "Updating cache for new region %s, affected chunks: %d",
                region.getRegionId().toString(), affectedChunks.size());

        for (String chunkKey : affectedChunks) {
            Set<Region> chunkRegions = chunkRegionCache.computeIfAbsent(chunkKey, k -> new HashSet<>());
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

    public Set<Region> getRegionsInChunk(String chunkKey) {
        return chunkRegionCache.get(chunkKey);
    }

    public String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
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

        chunkRegionCache.forEach((chunkKey, regions) -> {
            ConsoleLogger.debug("anarchyregions", "Chunk %s: %d regions",
                    chunkKey, regions.size());
        });
    }
}

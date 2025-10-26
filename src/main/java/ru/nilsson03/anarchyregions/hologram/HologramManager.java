package ru.nilsson03.anarchyregions.hologram;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import ru.nilsson03.anarchyregions.AnarchyRegions;
import ru.nilsson03.anarchyregions.event.RegionCreateEvent;
import ru.nilsson03.anarchyregions.event.RegionDestroyEvent;
import ru.nilsson03.anarchyregions.event.RegionUpdateEvent;
import ru.nilsson03.anarchyregions.event.RegionsLoadEvent;
import ru.nilsson03.anarchyregions.properties.PropertiesStorage;
import ru.nilsson03.anarchyregions.properties.RegionProperties;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.library.bukkit.file.configuration.ParameterFile;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;
import ru.nilsson03.library.text.util.ReplaceData;

import java.util.HashMap;

public class HologramManager implements Listener {

    private final Map<UUID, RegionHologram> regionHolograms;

    public HologramManager(AnarchyRegions plugin, ParameterFile configFile) {
        this.regionHolograms = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        ConsoleLogger.info("anarchyregions", "HologramManager initialized and registered as event listener");
    }

    @EventHandler
    public void onRegionDestroy(RegionDestroyEvent event) {
        Region region = event.getRegion();

        ConsoleLogger.info("anarchyregions", "HologramManager: Deleting hologram for region %s", region.getRegionId());

        try {
            deleteRegionHologram(region);
            ConsoleLogger.info("anarchyregions", "HologramManager: Successfully deleted hologram for region %s",
                    region.getRegionId());
        } catch (NullPointerException e) {
            ConsoleLogger.error("anarchyregions", "Failed to delete region hologram for region %s: %s",
                    region.getRegionId().toString(), e.getMessage());
            return;
        }
    }

    @EventHandler
    public void onRegionCreate(RegionCreateEvent event) {
        Region region = event.getRegion();

        try {
            createRegionHologram(region);
        } catch (NullPointerException e) {
            ConsoleLogger.error("anarchyregions", "Failed to create region hologram for region %s: %s",
                    region.getRegionId().toString(), e.getMessage());
            return;
        }
    }

    @EventHandler
    public void onRegionsLoad(RegionsLoadEvent event) {
        ConsoleLogger.info("anarchyregions", "Received RegionsLoadEvent with %d regions", event.getRegions().size());
        long startTime = System.currentTimeMillis();
        int createdCount = 0;
        int skippedCount = 0;

        for (Region region : event.getRegions()) {
            UUID regionId = region.getRegionId();

            if (regionHolograms.containsKey(regionId)) {
                skippedCount++;
                continue;
            }

            try {
                createRegionHologram(region);
                createdCount++;
            } catch (NullPointerException e) {
                ConsoleLogger.error("anarchyregions", "Failed to create region hologram for region %s: %s",
                        regionId.toString(), e.getMessage());
            }
        }

        ConsoleLogger.info("anarchyregions", "Loaded holograms: created=%d, skipped=%d, total=%d in %dms",
                createdCount, skippedCount, event.getRegions().size(), System.currentTimeMillis() - startTime);
    }

    @EventHandler
    public void onRegionUpdate(RegionUpdateEvent event) {
        Region region = event.getRegion();
        RegionHologram regionHologram = regionHolograms.get(region.getRegionId());
        RegionProperties regionProperties = PropertiesStorage.getInstance().getProperties(region.getBlockType());

        if (regionHologram != null) {
            regionHologram.updateHologram(
                    new ReplaceData("{durability}", new Random().nextInt(100) + 1),
                    new ReplaceData("{displayName}", regionProperties.getDisplayName()),
                    new ReplaceData("{radius}", String.valueOf(regionProperties.getRadius())),
                    new ReplaceData("{owner}", region.getRegionOwner().toString()));

            ConsoleLogger.debug("anarchyregions", "Updated hologram for region %s", region.getRegionId());
        }
    }

    public void createRegionHologram(Region region) throws NullPointerException {
        if (region == null) {
            throw new NullPointerException(
                    "Region cannot be null in createRegionHologram method in HologramManager class.");
        }
        RegionProperties regionProperties = PropertiesStorage.getInstance().getProperties(region.getBlockType());
        List<String> hologramLines = regionProperties.getHologramLines();

        if (hologramLines == null || hologramLines.isEmpty()) {
            ConsoleLogger.warn("anarchyregions",
                    "Hologram lines are null or empty for region %s. Check the region properties.",
                    region.getRegionId().toString());
            return;
        }

        RegionHologram regionHologram = new RegionHologram(
                region.getRegionId(),
                region.getCenterLocation(),
                hologramLines);

        regionHologram.updateHologram(
                new ReplaceData("{displayName}", regionProperties.getDisplayName()),
                new ReplaceData("{durability}", region.getDurability()),
                new ReplaceData("{radius}", String.valueOf(regionProperties.getRadius())),
                new ReplaceData("{owner}", region.getRegionOwner().toString()));

        regionHolograms.put(region.getRegionId(), regionHologram);
    }

    public void deleteRegionHologram(Region region) {
        RegionHologram regionHologram = regionHolograms.remove(region.getRegionId());
        if (regionHologram != null) {
            Hologram hologram = regionHologram.getHologram();

            if (hologram == null) {
                throw new NullPointerException(
                        "Hologram is null in deleteRegionHologram method in HologramManager class.");
            }

            DHAPI.removeHologram(hologram.getName());
        }
    }
}

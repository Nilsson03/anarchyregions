package ru.nilsson03.anarchyregions.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;
import ru.nilsson03.anarchyregions.AnarchyRegions;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.anarchyregions.region.RegionProperties;
import ru.nilsson03.library.bukkit.file.BukkitDirectory;
import ru.nilsson03.library.bukkit.file.FileRepository;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;
import ru.nilsson03.library.bukkit.util.Namespace;
import ru.nilsson03.library.bukkit.util.loc.LocationUtil;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;

public class RegionStorage {

    private final BukkitDirectory regionsDirectory;
    private final BukkitDirectory regionsDataDirectory;
    @Getter
    private final Set<RegionProperties> regionsProperties;
    @Getter
    private final Map<UUID, Region> regions;

    public RegionStorage(AnarchyRegions plugin) throws IllegalStateException {
        FileRepository fileRepository = plugin.fileRepository();
        
        try {
            this.regionsDirectory = plugin.getDirectory("regions");
            fileRepository.loadFiles(regionsDirectory, false);
        } catch (IllegalStateException e) {
            ConsoleLogger.error("anarchyregions", "Failed to load regions directory: %s", e.getMessage());
            throw e;
        }
        
        try {
            this.regionsDataDirectory = plugin.getDirectory("data");
            fileRepository.loadFiles(regionsDataDirectory, false);
        } catch (IllegalStateException e) {
            ConsoleLogger.error("anarchyregions", "Failed to load data directory: %s", e.getMessage());
            throw e;
        }
        
        ConsoleLogger.debug("anarchyregions", "Loading region storage...");
        this.regionsProperties = loadRegionsProperties();
        this.regions = loadRegions();
    }

    public void saveAllRegions() {
        for (Region region : regions.values()) {
            saveRegion(region);
        }
    }

    public void saveRegion(Region region) {
        String regionIdString = region.getRegionId().toString();
        BukkitConfig config = regionsDataDirectory.addNewConfig(regionIdString);
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

    public void removeRegion(Region region) {
        UUID regionId = region.getRegionId();
        regions.remove(regionId);

        String regionIdString = regionId.toString();

        if (regionsDataDirectory.containsFileWithName(regionIdString)) {
            BukkitConfig config = regionsDataDirectory.getBukkitConfig(regionIdString);
            regionsDataDirectory.removeAndDeleteConfig(config);
            ConsoleLogger.debug("anarchyregions", "Removed region %s from data directory", regionIdString);
        }
    }

    public void createRegion(Location location, UUID regionOwner, RegionProperties properties) {
        Region region = new Region(location, regionOwner, properties);
        regions.put(region.getRegionId(), region);
        ConsoleLogger.debug("anarchyregions", "Created region %s in storage", region.getRegionId().toString());
    }

    private Map<UUID, Region> loadRegions() {
        Map<UUID, Region> regions = new HashMap<>();
        long startTime = System.currentTimeMillis();
        for (BukkitConfig config : regionsDataDirectory.getCached()) {
            UUID uuid = UUID.fromString(config.getName().replace(".yml", ""));
            Region region = new Region(uuid, config);
            regions.put(uuid, region);
        }
        ConsoleLogger.info("anarchyregions", "Loaded %d regions in %dms", regions.size(), System.currentTimeMillis() - startTime);
        return regions;
    }

    private Set<RegionProperties> loadRegionsProperties() {
        Set<RegionProperties> regions = new HashSet<>();
        long startTime = System.currentTimeMillis();
        for (BukkitConfig config : regionsDirectory.getCached()) {
            ConfigurationSection section = config.getConfigurationSection("properties");
            if (section == null) {
                ConsoleLogger.warn("anarchyregions", "Region %s has no properties section", config.getName());
                continue;
            }

            RegionProperties region = new RegionProperties(section);
            regions.add(region);
        }

        ConsoleLogger.info("anarchyregions", "Loaded %d region properties in %dms", regions.size(), System.currentTimeMillis() - startTime);
        return regions;
    }

    @Nullable
    public RegionProperties getProperties(Namespace namespace) {
        return regionsProperties.stream()
                .filter(region -> region.getKey().equals(namespace))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public RegionProperties getProperties(Material blockType) {
        return regionsProperties.stream()
                .filter(region -> region.getBlockType() == blockType)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public Region getRegion(Location location) {
        return regions.values().stream()
                .filter(region -> region.getRegionTerritory().contains(location))
                .findFirst()
                .orElse(null);
    }

    public void reload() {
        long startTime = System.currentTimeMillis();
        regionsDirectory.reloadAll();
        regionsProperties.clear();
        regionsProperties.addAll(loadRegionsProperties());
        ConsoleLogger.debug("anarchyregions", "Reloaded region storage in %dms", System.currentTimeMillis() - startTime);
    }
}

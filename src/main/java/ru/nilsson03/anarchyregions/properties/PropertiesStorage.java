package ru.nilsson03.anarchyregions.properties;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;
import ru.nilsson03.anarchyregions.AnarchyRegions;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.library.bukkit.file.BukkitDirectory;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;
import ru.nilsson03.library.bukkit.util.Namespace;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;

public class PropertiesStorage {

    @Getter
    @Nullable
    private static PropertiesStorage instance;
    private final BukkitDirectory propertiesDirectory;

    @Getter
    private final Set<RegionProperties> regionsProperties;

    private PropertiesStorage(AnarchyRegions plugin) {
        try {
            this.propertiesDirectory = plugin.getDirectory("regions");
            plugin.fileRepository().loadFiles(propertiesDirectory, false);
        } catch (IllegalStateException e) {
            ConsoleLogger.error(
                "anarchyregions",
                "Failed to load regions directory: %s",
                e.getMessage()
            );
            throw e;
        }

        this.regionsProperties = loadRegionsProperties();
    }

    public static PropertiesStorage getInstance(AnarchyRegions plugin) {
        if (instance == null) {
            synchronized (PropertiesStorage.class) {
                if (instance == null) {
                    instance = new PropertiesStorage(plugin);
                }
            }
        }
        return instance;
    }

    private Set<RegionProperties> loadRegionsProperties() {
        Set<RegionProperties> regions = new HashSet<>();
        long startTime = System.currentTimeMillis();
        for (BukkitConfig config : propertiesDirectory.getCached()) {
            ConfigurationSection section = config.getConfigurationSection(
                "properties"
            );
            if (section == null) {
                ConsoleLogger.warn(
                    "anarchyregions",
                    "Region %s has no properties section",
                    config.getName()
                );
                continue;
            }

            RegionProperties region = new RegionProperties(section);
            regions.add(region);
        }

        ConsoleLogger.info(
            "anarchyregions",
            "Loaded %d region properties in %dms",
            regions.size(),
            System.currentTimeMillis() - startTime
        );
        return regions;
    }

    @Nullable
    public RegionProperties getProperties(Namespace namespace) {
        return regionsProperties
            .stream()
            .filter(region -> region.getKey().equals(namespace))
            .findFirst()
            .orElse(null);
    }

    @Nullable
    public RegionProperties getProperties(Material blockType) {
        return regionsProperties
            .stream()
            .filter(region -> region.getBlockType() == blockType)
            .findFirst()
            .orElse(null);
    }

    @Nullable
    public static RegionProperties getPropertiesForRegion(Region region) {
        PropertiesStorage storage = getInstance();
        if (storage == null) {
            return null;
        }
        return storage.getProperties(region.getBlockType());
    }
}

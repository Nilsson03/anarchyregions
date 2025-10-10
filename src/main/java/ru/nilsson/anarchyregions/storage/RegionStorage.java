package ru.nilsson.anarchyregions.storage;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import lombok.Getter;
import ru.nilsson.anarchyregions.CultAnarchyRegions;
import ru.nilsson.anarchyregions.region.RegionProperties;
import ru.nilsson03.library.bukkit.file.BukkitDirectory;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;
import ru.nilsson03.library.bukkit.util.Namespace;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;

public class RegionStorage {

    private final BukkitDirectory regionsDirectory;
    @Getter
    private final Set<RegionProperties> regions;

    public RegionStorage(CultAnarchyRegions plugin) throws IllegalStateException {
        this.regionsDirectory = plugin.getDirectory("regions");
        ConsoleLogger.debug("anarchyregions", "Loading region storage...");
        this.regions = loadRegions();
    }

    private Set<RegionProperties> loadRegions() {
        Set<RegionProperties> regions = new HashSet<>();
        for (BukkitConfig config : regionsDirectory.getCached()) {
            ConfigurationSection section = config.getConfigurationSection("properties");
            if (section == null) {
                ConsoleLogger.warn("anarchyregions", "Region %s has no properties section", config.getName());
                continue;
            }

            RegionProperties region = new RegionProperties(section);
            regions.add(region);
        }

        ConsoleLogger.info("anarchyregions", "Loaded %d regions", regions.size());
        return regions;
    }

    @Nullable
    public RegionProperties getRegion(Namespace namespace) {
        return regions.stream()
                .filter(region -> region.getKey().equals(namespace))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public RegionProperties getRegion(Material blockType) {
        return regions.stream()
                .filter(region -> region.getBlockType() == blockType)
                .findFirst()
                .orElse(null);
    }

    public void reload() {
        long startTime = System.currentTimeMillis();
        regionsDirectory.reloadAll();
        regions.clear();
        regions.addAll(loadRegions());
        ConsoleLogger.debug("anarchyregions", "Reloaded region storage in %dms", System.currentTimeMillis() - startTime);
    }
}

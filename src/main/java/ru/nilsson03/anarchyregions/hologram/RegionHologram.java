package ru.nilsson03.anarchyregions.hologram;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import lombok.Setter;
import ru.nilsson03.library.text.util.ReplaceData;
import ru.nilsson03.library.bukkit.util.loc.LocationUtil;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;
import ru.nilsson03.library.text.api.UniversalTextApi;

@Getter
public class RegionHologram {
    
    private final UUID regionId;

    @Setter
    private Hologram hologram;
    private final List<String> originalHologramLines;
    private final Location location; 

    public RegionHologram(UUID regionId, Location location, List<String> hologramLines) {
        this.regionId = regionId;
        this.location = location;
        this.originalHologramLines = new ArrayList<>(hologramLines);
        this.hologram = null;
    }

    public void updateHologram(ReplaceData... replacesData) {
        List<String> updatedLines = UniversalTextApi.replacePlaceholders(
            new ArrayList<>(originalHologramLines),
             replacesData);
        
        try {
            if (hologram != null) {
                DHAPI.removeHologram(hologram.getName());
            }
            
            Location formatLocation = LocationUtil.updateHologramHeight(location, updatedLines);
            this.hologram = DHAPI.createHologram(regionId.toString(), formatLocation, false);
            
            for (String line : updatedLines) {
                DHAPI.addHologramLine(hologram, UniversalTextApi.colorize(line));
            }
        } catch (Exception e) {
            ConsoleLogger.error("anarchyregions", "Failed to update region hologram for region %s: %s", regionId.toString(), e.getMessage());
        }
    }
}

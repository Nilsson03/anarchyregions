package ru.nilsson03.anarchyregions.region.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.anarchyregions.region.manager.RegionManager;

@AllArgsConstructor
public class RegionIndexService {
    
    private final RegionManager regionManager;

    public List<Region> getOrderedPlayerRegions(UUID playerUuid) {
        return regionManager.getPlayerRegions(playerUuid).stream()
                .sorted(Comparator.comparing(region -> region.getRegionId().toString()))
                .toList();
    }

    public Map<Region, Integer> getPlayerRegionsWithIndex(UUID playerUuid) {
        List<Region> regions = getOrderedPlayerRegions(playerUuid);
        Map<Region, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < regions.size(); i++) {
            result.put(regions.get(i), i + 1);
        }
        return result;
    }

    public int getPlayerRegionIndex(UUID playerUuid, UUID regionId) {
        List<Region> regions = getOrderedPlayerRegions(playerUuid);
        for (int i = 0; i < regions.size(); i++) {
            if (regions.get(i).getRegionId().equals(regionId)) {
                return i + 1; 
            }
        }
        return -1; 
    }

    public Region getPlayerRegionByIndex(UUID playerUuid, int index) {
        if (index <= 0) {
            return null;
        }

        List<Region> regions = getOrderedPlayerRegions(playerUuid);
        if (index > regions.size()) {
        return null;
        }

        return regions.get(index - 1);
    }
}

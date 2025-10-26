package ru.nilsson03.anarchyregions.service;

import java.util.UUID;
import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import ru.nilsson03.anarchyregions.AnarchyRegions;
import ru.nilsson03.anarchyregions.event.RegionUpdateEvent;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.anarchyregions.region.manager.RegionManager;
import ru.nilsson03.library.bukkit.file.configuration.ParameterFile;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;

/**
 * Оптимизированный сервис для обновления данных регионов с соблюдением принципа единственной ответственности.
 * Использует батчевое обновление для минимизации нагрузки на основной поток.
 */
public class RegionUpdateService {
    
    private final RegionManager regionManager;
    private final Queue<UUID> updateQueue;
    private final int updateInterval;
    private final int maxUpdatesPerTick;
    private BukkitTask updateTask;
    
    public RegionUpdateService(RegionManager regionManager, ParameterFile configFile) {
        this.regionManager = regionManager;
        this.updateInterval = configFile.getValueAs("settings.region-update-interval-ticks", Integer.class);
        this.maxUpdatesPerTick = configFile.getValueAs("settings.region-max-updates-per-tick", Integer.class);
        this.updateQueue  = new LinkedList<>();
        
        ConsoleLogger.info("anarchyregions", "RegionUpdateService initialized - interval: %d ticks, max per tick: %d", 
            updateInterval, maxUpdatesPerTick);
    }
    
    public void startRegionUpdates(Region region) {
        UUID regionId = region.getRegionId();
        updateQueue .offer(regionId);
    
        
        if (updateTask == null || updateTask.isCancelled()) {
            startGlobalUpdateTask();
        }
        
        ConsoleLogger.debug("anarchyregions", "Added region %s to update queue (total: %d)", regionId, updateQueue.size());
    }
    
    private void startGlobalUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(AnarchyRegions.getInstance(), () -> {
            processBatchUpdates();
        }, 0, updateInterval);
        
        ConsoleLogger.info("anarchyregions", "Started global region update task");
    }

    public void stopRegionUpdates(Region region) {
        updateQueue.remove(region.getRegionId());
        ConsoleLogger.info("anarchyregions", "Stopped region updates for region %s", region.getRegionId());
    }
    
    private void processBatchUpdates() {
        if (updateQueue .isEmpty()) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        int processedCount = 0;
        int queueSize = updateQueue.size();
        
        while (processedCount < maxUpdatesPerTick && System.currentTimeMillis() - startTime < 2 && processedCount < queueSize) {

            UUID regionId = updateQueue.poll();
            if (regionId == null) break;
            
            try {
                Region region = regionManager.getRegionStorage().getRegion(regionId);

                if (region != null) {
                    updateRegionData(region);
                    updateQueue.offer(regionId);
                    processedCount++;
                }
            } catch (Exception e) {
                ConsoleLogger.error("anarchyregions", "Error updating region %s: %s", regionId, e.getMessage());
            }
        }
        
        if (!updateQueue.isEmpty() && processedCount > 0) {
            ConsoleLogger.debug("anarchyregions", "Processed %d region updates in %dms (remaining: %d)", 
                processedCount, System.currentTimeMillis() - startTime, updateQueue.size());
        }
    }
    
    private void updateRegionData(Region region) {
        try {
            RegionUpdateEvent event = new RegionUpdateEvent(region);
            Bukkit.getPluginManager().callEvent(event);
            
        } catch (Exception e) {
            ConsoleLogger.error("anarchyregions", "Error updating region %s: %s", region.getRegionId(), e.getMessage());
        }
    }
    
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        updateQueue.clear();
        ConsoleLogger.info("anarchyregions", "RegionUpdateService shutdown completed");
    }
}

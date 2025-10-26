package ru.nilsson03.anarchyregions.region.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Player;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import lombok.AllArgsConstructor;
import ru.nilsson03.anarchyregions.menu.RegionMenu;
import ru.nilsson03.anarchyregions.properties.PropertiesStorage;
import ru.nilsson03.anarchyregions.properties.RegionProperties;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.anarchyregions.region.manager.RegionManager;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;

@AllArgsConstructor
public class RegionListener implements Listener {

    private final BukkitConfig messagesConfig;
    private final PropertiesStorage propertiesStorage;
    private final RegionManager regionManager;

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        Block block = event.getBlock();

        if (!regionManager.regionExistsInLocation(location)) {
            return;
        }

        Region region = regionManager.getRegionByLocation(location);

        if (!regionManager.isOwner(region, player.getUniqueId()) && !playerIsAdmin(player)) {
            event.setCancelled(true);
            player.sendMessage(messagesConfig.getString("messages.region_not_owner"));
            ConsoleLogger.warn("anarchyregions", "Player %s tried to break block in region at %s (not owner)", 
                player.getName(), formatLocation(location));
            return;
        }

        Location centerLocation = region.getCenterLocation();
        
        // Проверяем по координатам (location.equals() не работает после разрушения блока)
        boolean isCenterBlock = location.getWorld().equals(centerLocation.getWorld()) &&
            location.getBlockX() == centerLocation.getBlockX() &&
            location.getBlockY() == centerLocation.getBlockY() &&
            location.getBlockZ() == centerLocation.getBlockZ();
        
        if (isCenterBlock && block.getType() == region.getBlockType()) {
            boolean destroy = region.destroy();

            if (destroy) {
            player.sendMessage(messagesConfig.getString("messages.region_destroyed"));
            ConsoleLogger.info("anarchyregions", "Player %s destroyed region at %s", 
                player.getName(), formatLocation(location));
            }
        }
    }

    @EventHandler
    public void interactWithRegion(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null || clickedBlock.getType() == Material.AIR) {
            return;
        }

        Location location = clickedBlock.getLocation();

        if (!regionManager.regionExistsInLocation(location)) {
            return;
        }
        
        Region region = regionManager.getRegionByLocation(location);
        Location coreLocation = region.getCenterLocation();

        if (location == coreLocation && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (regionManager.isOwner(region, player.getUniqueId())) {
                RegionMenu.openMenu(player, region);
            } else {
                event.setCancelled(true);
                player.sendMessage(messagesConfig.getString("messages.region_not_owner"));
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();
        Location location = block.getLocation();

        RegionProperties properties = propertiesStorage.getProperties(material);
        
        if (properties == null) {
            if (regionManager.regionExistsInLocation(location)) {
                Region region = regionManager.getRegionByLocation(location);
                if (region != null && !regionManager.isOwner(region, player.getUniqueId()) && !playerIsAdmin(player)) {
                    event.setCancelled(true);
                    player.sendMessage(messagesConfig.getString("messages.region_not_owner"));
                }
            }
            return;
        }

        if (regionManager.regionExistsInLocation(location)) {
            Region existingRegion = regionManager.getRegionByLocation(location);
            if (existingRegion != null) {
                if (!regionManager.isOwner(existingRegion, player.getUniqueId()) && !playerIsAdmin(player)) {
                    event.setCancelled(true);
                    player.sendMessage(messagesConfig.getString("messages.region_not_owner"));
                    return;
                }
                
                event.setCancelled(true);
                player.sendMessage(messagesConfig.getString("messages.region_already_exists"));
                return;
            }
        }

        if (regionManager.wouldRegionOverlap(location, properties.getRadius())) {
            event.setCancelled(true);
            player.sendMessage(messagesConfig.getString("messages.region_overlap"));
            ConsoleLogger.warn("anarchyregions", "Player %s tried to create overlapping region at %s", 
                player.getName(), formatLocation(location));
            return;
        }

        regionManager.createRegion(location, player.getUniqueId(), properties);
        player.sendMessage(messagesConfig.getString("messages.region_created"));
        ConsoleLogger.info("anarchyregions", "Player %s created region %s at %s", 
            player.getName(), properties.getDisplayName(), formatLocation(location));
    }

    private boolean playerIsAdmin(Player player) {
        return player.hasPermission("anarchyregions.admin");
    }
    
    /**
     * Форматирует локацию для логирования
     */
    private String formatLocation(Location location) {
        return String.format("%s (%d, %d, %d)", 
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ());
    }
}

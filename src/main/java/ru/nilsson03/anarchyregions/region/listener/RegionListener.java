package ru.nilsson03.anarchyregions.region.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import org.bukkit.entity.Player;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import lombok.AllArgsConstructor;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.anarchyregions.region.RegionProperties;
import ru.nilsson03.anarchyregions.storage.RegionStorage;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;

@AllArgsConstructor
public class RegionListener implements Listener {

    private final RegionStorage regionStorage;
    private final BukkitConfig messagesConfig;

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Region region = regionStorage.getRegion(event.getBlock().getLocation());
        if (region != null) {
            region.destroy();
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();
        Player player = event.getPlayer();
        RegionProperties properties = regionStorage.getProperties(material);

        Location location = block.getLocation();

        Region region = regionStorage.getRegion(event.getBlock().getLocation());

        if (region != null && properties != null) {
            event.setCancelled(true);
            player.sendMessage(messagesConfig.getString("messages.region_already_exists"));
        } else if (region != null && region.getRegionOwner().equals(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(messagesConfig.getString("messages.region_already_owned"));
        } else if (region == null && properties != null) {
            regionStorage.createRegion(location, player.getUniqueId(), properties);
            player.sendMessage(messagesConfig.getString("messages.region_created"));
        }
    }
}

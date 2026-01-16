package ru.nilsson03.anarchyregions.region;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.Setter;
import ru.nilsson03.anarchyregions.event.RegionDestroyEvent;
import ru.nilsson03.anarchyregions.properties.RegionProperties;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;
import ru.nilsson03.library.bukkit.util.loc.Cuboid;
import ru.nilsson03.library.bukkit.util.loc.LocationUtil;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;

@Getter
public class Region {

    private final UUID regionId;
    private final Location centerLocation;
    private final Cuboid regionTerritory;
    private final UUID regionOwner;
    private final Material blockType;

    private final Set<UUID> members = new HashSet<>();

    @Setter
    private int durability;

    public Region(Location centerLocation, UUID regionOwner, RegionProperties regionProperties) {
        this.regionId = UUID.randomUUID();
        int regionRadius = regionProperties.getRadius();
        World world = centerLocation.getWorld();

        Vector minPoint = new Vector(
            centerLocation.getX() - regionRadius,
            centerLocation.getY() - regionRadius,
            centerLocation.getZ() - regionRadius
        );
        Vector maxPoint = new Vector(
            centerLocation.getX() + regionRadius,
            centerLocation.getY() + regionRadius,
            centerLocation.getZ() + regionRadius
        );

        this.regionTerritory = new Cuboid(
            minPoint.toLocation(world),
            maxPoint.toLocation(world)
        );
        this.centerLocation = centerLocation;
        this.regionOwner = regionOwner;
        this.durability = regionProperties.getDurability();
        this.blockType = regionProperties.getBlockType();
    }

    public Region(UUID regionId, BukkitConfig config) {
        this.regionId = regionId;
        this.centerLocation = LocationUtil.stringToLocation(config.getString("center"));
        Location locationOne = LocationUtil.stringToLocation(config.getString("territory.one"));
        Location locationTwo = LocationUtil.stringToLocation(config.getString("territory.two"));
        this.regionTerritory = new Cuboid(locationOne, locationTwo);
        this.regionOwner = UUID.fromString(config.getString("owner"));
        this.durability = config.getInt("durability");
        this.blockType = Material.getMaterial(config.getString("block-type"));
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public int getMembersCount() {
        return members.size();
    }

    public void addMember(UUID memberUuid) {
        members.add(memberUuid);
    }

    public boolean isMember(UUID playerUUID) {
        return members.contains(playerUUID);
    }

    public void decrementDurability() {
        this.durability--;
        ConsoleLogger.debug("anarchyregions", "Durability of region %s decremented to %d", regionId, durability);
        if (this.durability <= 0) {

            boolean destroy = destroy();

            if (!destroy) {
                ConsoleLogger.info("anarchyregions", "Region %s destruction cancelled, durability restored", regionId);
                this.durability = 1; 
                return;
            }
            
            ConsoleLogger.info("anarchyregions", "Region %s destroyed due to durability reaching 0", regionId);
        }
    }

    public boolean destroy() {
        RegionDestroyEvent event = new RegionDestroyEvent(this, centerLocation);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }
        
        Block block = centerLocation.getBlock();
        block.setType(Material.AIR);
        return true;
    }
}

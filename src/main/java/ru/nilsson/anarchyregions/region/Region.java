package ru.nilsson.anarchyregions.region;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.Setter;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;
import ru.nilsson03.library.bukkit.util.loc.Cuboid;
import ru.nilsson03.library.bukkit.util.loc.LocationUtil;

@Getter
public class Region {

    private final Location centerLocation;
    private final Cuboid regionTerritory;
    private final UUID regionOwner;
    private final Material blockType;

    @Setter
    private int durability;

    public Region(Location centerLocation, UUID regionOwner, RegionProperties regionProperties) {

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

    public Region(BukkitConfig config) {
        this.centerLocation = LocationUtil.stringToLocation(config.getString("center"));
        Location locationOne = LocationUtil.stringToLocation(config.getString("territory.one"));
        Location locationTwo = LocationUtil.stringToLocation(config.getString("territory.two"));
        this.regionTerritory = new Cuboid(locationOne, locationTwo);
        this.regionOwner = UUID.fromString(config.getString("owner"));
        this.durability = config.getInt("durability");
        this.blockType = Material.getMaterial(config.getString("block-type"));
    }

    public void destroy() {
        Block block = centerLocation.getBlock();
        block.setType(Material.AIR);
    }
}

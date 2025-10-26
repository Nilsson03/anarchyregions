package ru.nilsson03.anarchyregions.properties;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import lombok.Getter;
import ru.nilsson03.library.bukkit.util.Namespace;

@Getter
public class RegionProperties {

    private final Namespace key;
    private final String displayName;
    private final Material blockType;
    private final int durability;
    private final int radius;
    private final List<String> hologramLines;

    public RegionProperties(ConfigurationSection section) {
        this.key = Namespace.of("anarchyregions",section.getString("key"));
        this.displayName = section.getString("name");
        this.blockType = Material.getMaterial(section.getString("block-type"));
        this.durability = section.getInt("durability");
        this.radius = section.getInt("radius");
        this.hologramLines = section.getStringList("hologram-lines");
    }
    
}

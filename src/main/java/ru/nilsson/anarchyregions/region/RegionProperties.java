package ru.nilsson.anarchyregions.region;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import lombok.Getter;
import ru.nilsson03.library.bukkit.util.Namespace;

@Getter
public class RegionProperties {

    private final Namespace key;
    private final String displayName;
    private final Material blockType;

    public RegionProperties(ConfigurationSection section) {
        this.key = Namespace.of("anarchyregions",section.getString("key"));
        this.displayName = section.getString("display-name");
        this.blockType = Material.getMaterial(section.getString("block-type"));
    }
    
}

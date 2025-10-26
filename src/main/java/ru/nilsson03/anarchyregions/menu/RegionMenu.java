package ru.nilsson03.anarchyregions.menu;

import java.util.List;

import org.bukkit.entity.Player;

import ru.nilsson03.anarchyregions.AnarchyRegions;
import ru.nilsson03.anarchyregions.menu.api.CustomItem;
import ru.nilsson03.anarchyregions.menu.util.MenuUtil;
import ru.nilsson03.anarchyregions.properties.PropertiesStorage;
import ru.nilsson03.anarchyregions.properties.RegionProperties;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;
import ru.nilsson03.library.text.util.ReplaceData;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.Gui.Builder;
import xyz.xenondevs.invui.gui.Gui.Builder.Normal;
import xyz.xenondevs.invui.window.Window;

public class RegionMenu {

    private static final BukkitConfig regionMenuConfig;
    private static final AnarchyRegions anarchyRegions;
    

    static {
        anarchyRegions = AnarchyRegions.getInstance();
        regionMenuConfig = anarchyRegions.getInventoriesDirectory().getBukkitConfig("region-menu.yml");
    }

    public static void openMenu(Player player, Region region) {


        String[] structure = regionMenuConfig.operations().getList("region-menu.structure", new ReplaceData[0]).toArray(new String[0]);
        Builder<Gui, Normal> builder = Gui.normal()
                .setStructure(structure);

        RegionProperties properties = PropertiesStorage.getPropertiesForRegion(region);

        List<CustomItem> customItems = MenuUtil.parseSection(
            anarchyRegions, 
            regionMenuConfig.getConfigurationSection("region-menu.items.custom"),
                new ReplaceData("{radius}", properties.getRadius()),
                new ReplaceData("{displayName}", properties.getDisplayName())
        );

        customItems.forEach(item -> {
            builder.addIngredient(item.getChar(), item);
        });

        Gui gui = builder.build();
        Window.single().setViewer(player)
                .setTitle(regionMenuConfig.operations().getString("region-menu.title"))
                .setGui(gui)
                .build()
                .open();
    }
    
}

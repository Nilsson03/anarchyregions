package ru.nilsson03.anarchyregions.menu;

import java.util.List;
import org.bukkit.entity.Player;
import ru.nilsson03.anarchyregions.AnarchyRegions;
import ru.nilsson03.anarchyregions.menu.api.CustomItem;
import ru.nilsson03.anarchyregions.menu.util.MenuUtil;
import ru.nilsson03.anarchyregions.properties.PropertiesStorage;
import ru.nilsson03.anarchyregions.properties.RegionProperties;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.anarchyregions.region.manager.RegionManager;
import ru.nilsson03.anarchyregions.region.service.RegionIndexService;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;
import ru.nilsson03.library.text.util.ReplaceData;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.Gui.Builder;
import xyz.xenondevs.invui.gui.Gui.Builder.Normal;
import xyz.xenondevs.invui.window.Window;

public class RegionMenu {

    private static final BukkitConfig regionMenuConfig;
    private static final AnarchyRegions plugin;
    private static final RegionIndexService regionIndexService;

    private static final String[] menuStructure;

    static {
        plugin = AnarchyRegions.getInstance();
        regionMenuConfig = plugin
                .getInventoriesDirectory()
                .getBukkitConfig("region-menu.yml");
        regionIndexService = plugin.getRegionIndexService();
        menuStructure = regionMenuConfig.getList("region-menu.structure").toArray(new String[0]);
    }

    public static void openMenu(Player player, Region region) {
        String[] structure = menuStructure;
        Builder<Gui, Normal> builder = Gui.normal().setStructure(structure);

        RegionProperties properties = PropertiesStorage.getPropertiesForRegion(
                region);

                

        List<CustomItem> customItems = MenuUtil.parseSection(
                plugin,
                regionMenuConfig.getConfigurationSection(
                        "region-menu.items.custom"),
                new ReplaceData("{numeric}", regionIndexService.getPlayerRegionIndex(player.getUniqueId(), region.getRegionId())),
                new ReplaceData("{radius}", properties.getRadius()),
                new ReplaceData("{displayName}", properties.getDisplayName()),
                new ReplaceData("{membersCount}", region.getMembersCount()));

        customItems.forEach(item -> {
            builder.addIngredient(item.getChar(), item);
        });

        Gui gui = builder.build();
        Window.single()
                .setViewer(player)
                .setTitle(
                        regionMenuConfig.operations().getString("region-menu.title"))
                .setGui(gui)
                .build()
                .open();
    }
}

package ru.nilsson03.anarchyregions.menu;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import ru.nilsson03.anarchyregions.AnarchyRegions;
import ru.nilsson03.anarchyregions.menu.api.BackButton;
import ru.nilsson03.anarchyregions.menu.api.CustomItem;
import ru.nilsson03.anarchyregions.menu.api.ForwardButton;
import ru.nilsson03.anarchyregions.menu.item.MemberItem;
import ru.nilsson03.anarchyregions.menu.util.MenuUtil;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.anarchyregions.region.service.RegionIndexService;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;
import ru.nilsson03.library.text.util.ReplaceData;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.window.Window;

public class MembersMenu {

    private static final BukkitConfig regionMenuConfig;
    private static final AnarchyRegions plugin;
    private static final RegionIndexService regionIndexService;

    private static final String[] structure;

    static {
        plugin = AnarchyRegions.getInstance();
        regionMenuConfig = plugin
                .getInventoriesDirectory()
                .getBukkitConfig("members-menu.yml");
        regionIndexService = plugin.getRegionIndexService();
        structure = regionMenuConfig.getList("menu.structure").toArray(new String[0]);
    }

    public static void openMenu(Player player, Region region) {

        Set<UUID> members = region.getMembers();

        List<Item> items = members.stream()
                .map(uuid -> new MemberItem(Bukkit.getOfflinePlayer(uuid), region))
                .collect(Collectors.toList());

        PagedGui.Builder<Item> builder = PagedGui.items()
            .setStructure(structure)
            .setContent(items)
            .addIngredient('>', new ForwardButton())
            .addIngredient('<', new BackButton())
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL);

        List<CustomItem> customItems = MenuUtil.parseSection(
                plugin,
                regionMenuConfig.getConfigurationSection(
                        "menu.items.custom"),
                new ReplaceData("{numeric}", regionIndexService.getPlayerRegionIndex(player.getUniqueId(), region.getRegionId())),
                new ReplaceData("{membersCount}", region.getMembersCount()));

        customItems.forEach(item -> {
            builder.addIngredient(item.getChar(), item);
        });

        Gui gui = builder.build();
        Window.single()
                .setViewer(player)
                .setTitle(
                        regionMenuConfig.operations().getString("menu.title"))
                .setGui(gui)
                .build()
                .open();
    }
}

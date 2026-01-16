package ru.nilsson03.anarchyregions.menu.item;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import ru.nilsson03.anarchyregions.AnarchyRegions;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;
import ru.nilsson03.library.bukkit.item.builder.impl.UniversalSkullBuilder;
import ru.nilsson03.library.bukkit.item.skull.SkullTextureHandler;
import ru.nilsson03.library.bukkit.item.skull.impl.universal.PaperSkullTextureHandler;
import ru.nilsson03.library.text.util.ReplaceData;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class MemberItem extends AbstractItem {

    private static final BukkitConfig menuConfig;
    private static final SkullTextureHandler textureHandler = new PaperSkullTextureHandler();

    static {
        menuConfig = AnarchyRegions.getInstance()
                .getInventoriesDirectory()
                .getBukkitConfig("members-menu.yml");
    }

    private final OfflinePlayer member;
    private final Region region;

    public MemberItem(OfflinePlayer member, Region region) {
        this.member = member;
        this.region = region;
    }

    @Override
    public ItemProvider getItemProvider() {

        String displayName = member.getName();

        return new ItemBuilder(new UniversalSkullBuilder(textureHandler)
                .setOwner(member)
                .setDisplayName(menuConfig.getString("menu.items.member",
                        new ReplaceData("{name}", displayName)))
                .setLore(menuConfig.getList("menu.items.member.lore",
                        new ReplaceData("{name}", displayName)))
                .build());
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        event.setCancelled(true);
    }
}

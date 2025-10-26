package ru.nilsson03.anarchyregions.menu.api;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.nilsson03.library.bukkit.item.builder.impl.SpigotItemBuilder;
import ru.nilsson03.library.bukkit.util.ItemUtil;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.util.List;

public class StaticCustomItem extends AbstractItem implements CustomItem {

    private final ConfigurationSection section;
    private final ItemProvider itemProvider;
    private final char c;

    public StaticCustomItem(ConfigurationSection section) {
        this.section = section;
        ItemStack itemStack = buildItem();
        this.itemProvider = new ItemBuilder(itemStack);
        c = section.getString("position").charAt(0);
    }

    @Override
    public ConfigurationSection section() {
        return section;
    }

    @Override
    public Character getChar() {
        return c;
    }

    @Override
    public ItemProvider getItemProvider() {
        return itemProvider;
    }

    public char getPosition() {
        return c;
    }

    private ItemStack buildItem() {
        ItemStack item = createBaseItem();
        ItemMeta meta = item.getItemMeta();

        SpigotItemBuilder builder = new SpigotItemBuilder(item)
                .setMeta(meta);

        builder.setDisplayName(section.getString("name"));

        if (section.contains("lore")) {
            List<String> lore = section.getStringList("lore");
            builder.setLore(lore);
        }

        return builder.build();
    }

    private ItemStack createBaseItem() {
        if (section.getString("type", "material").equalsIgnoreCase("head")) {
            return createHeadItem(section.getString("head-id"));
        }
        return new ItemStack(Material.valueOf(section.getString("material")));
    }

    private ItemStack createHeadItem(String texture) {
        return ItemUtil.createHead(texture)
                .build();
    }
}

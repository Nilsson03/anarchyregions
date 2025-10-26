package ru.nilsson03.anarchyregions.menu.api;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import ru.nilsson03.anarchyregions.AnarchyRegions;
import ru.nilsson03.library.bukkit.file.configuration.ParameterFile;
import ru.nilsson03.library.bukkit.item.builder.impl.SpigotItemBuilder;
import ru.nilsson03.library.bukkit.util.ItemUtil;
import ru.nilsson03.library.text.api.UniversalTextApi;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

public class ForwardButton extends PageItem {
    public ForwardButton() {
        super(true);
    }

    public ItemProvider getItemProvider(PagedGui<?> gui) {
        ParameterFile parameterFile = AnarchyRegions.getInstance().getConfigFile();

        String type = parameterFile.getValueAs("inventories.buttons.forward-button.type", String.class);
        String displayName = UniversalTextApi.colorize(parameterFile.getValueAs("inventories.buttons.forward-button.name", String.class));
        List<String> lore = UniversalTextApi.colorize(parameterFile.getValueAs("inventories.buttons.forward-button.lore", List.class));

        ItemStack itemStack;
        if (type.equalsIgnoreCase("head")) {
            String url = parameterFile.getValueAs("inventories.buttons.forward-button.head-id", String.class);
            itemStack = ItemUtil.createHead(url)
                    .setDisplayName(displayName)
                    .setLore(lore)
                    .build();
        } else {
            String materialName = parameterFile.getValueAs("inventories.buttons.forward-button.material", String.class);
            itemStack = new SpigotItemBuilder(Material.valueOf(materialName))
                    .setDisplayName(displayName)
                    .setLore(lore)
                    .build();
        }

        return new ItemBuilder(itemStack);
    }
}

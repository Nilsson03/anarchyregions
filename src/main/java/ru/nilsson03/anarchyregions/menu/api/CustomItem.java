package ru.nilsson03.anarchyregions.menu.api;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import ru.nilsson03.library.bukkit.util.log.ConsoleLogger;
import xyz.xenondevs.invui.item.Item;

public interface CustomItem extends Item {

    ConfigurationSection section();

    Character getChar();

    @Override
    default void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {
        if (inventoryClickEvent.getWhoClicked() instanceof Player) {
            if (section().contains("click-commands")) {
                section().getStringList("click-commands").forEach(cmd -> {
                            ConsoleLogger.debug("anarchyregions",
                                    "Call click command %s from customItem for player %s",
                                    cmd,
                                    player.getUniqueId());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                    cmd.replace("{player}", player.getName()));
                        }
                );
            }
        }
    }
}

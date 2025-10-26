package ru.nilsson03.anarchyregions.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.nilsson03.anarchyregions.region.Region;

/**
 * Событие, вызываемое при обновлении данных региона.
 * Позволяет другим компонентам системы реагировать на изменения в регионе.
 */
@Getter
@AllArgsConstructor
public class RegionUpdateEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    private final Region region;
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}

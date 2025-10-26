package ru.nilsson03.anarchyregions.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import org.bukkit.Location;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.nilsson03.anarchyregions.region.Region;

@Getter
@AllArgsConstructor
public class RegionCreateEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    private final Region region;
    private final Location location;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}

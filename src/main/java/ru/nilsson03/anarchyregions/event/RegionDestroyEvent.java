package ru.nilsson03.anarchyregions.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import org.bukkit.Location;
import lombok.Getter;
import lombok.Setter;
import ru.nilsson03.anarchyregions.region.Region;

@Getter
public class RegionDestroyEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    private final Region region;
    private final Location location;
    @Setter
    private boolean cancelled = false;

    public RegionDestroyEvent(Region region, Location location) {
        this.region = region;
        this.location = location;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}

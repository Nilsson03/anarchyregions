package ru.nilsson03.anarchyregions.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

import org.bukkit.Location;

import lombok.Getter;
import lombok.Setter;
import ru.nilsson03.anarchyregions.properties.RegionProperties;

@Getter
public class RegionPreCreateEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final UUID owner;
    private final Location location;
    private final RegionProperties properties;
    @Setter
    public boolean cancelled = false; 

    public RegionPreCreateEvent(UUID owner, Location location, RegionProperties properties) {
        this.owner = owner;
        this.location = location;
        this.properties = properties;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
}

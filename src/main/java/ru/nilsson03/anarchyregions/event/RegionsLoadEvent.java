package ru.nilsson03.anarchyregions.event;

import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.nilsson03.anarchyregions.region.Region;

@Getter
@AllArgsConstructor
public class RegionsLoadEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    private final List<Region> regions;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}

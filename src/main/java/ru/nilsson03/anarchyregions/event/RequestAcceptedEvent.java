package ru.nilsson03.anarchyregions.event;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;

@Getter
public class RequestAcceptedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final UUID inviter;
    private final UUID target;
    private final UUID regionUUID;

    public RequestAcceptedEvent(UUID inviter, UUID target, UUID regionUUID) {
        this.inviter = inviter;
        this.target = target;
        this.regionUUID = regionUUID;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

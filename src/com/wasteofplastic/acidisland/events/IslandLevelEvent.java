package com.wasteofplastic.acidisland.events;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.wasteofplastic.acidisland.ASkyBlock;

/**
 * This event is fired when an island level is calculated
 * 
 * @author tastybento
 * 
 */
public class IslandLevelEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID player;
    private int level;

    /**
     * @param player
     * @param teamLeader
     */
    public IslandLevelEvent(ASkyBlock plugin, UUID player, int level) {
	this.player = player;
	this.level = level;
    }

    /**
     * @return the player
     */
    public UUID getPlayer() {
        return player;
    }
   
    /**
     * @return the level
     */
    public int getLevel() {
        return level;
    }

    @Override
    public HandlerList getHandlers() {
	return handlers;
    }

    public static HandlerList getHandlerList() {
	return handlers;
    }
}

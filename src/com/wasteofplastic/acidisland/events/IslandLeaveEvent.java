package com.wasteofplastic.acidisland.events;

import java.util.UUID;

import com.wasteofplastic.acidisland.Island;


/**
 * Fired when a player leaves an island team
 * @author tastybento
 *
 */
public class IslandLeaveEvent extends ASkyBlockEvent {

    /**
     * @param player
     * @param island
     */
    public IslandLeaveEvent(UUID player, Island island) {
	super(player, island);
    }

}

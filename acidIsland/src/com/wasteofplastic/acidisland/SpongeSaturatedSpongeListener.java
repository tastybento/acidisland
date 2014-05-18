package com.wasteofplastic.acidisland;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

public class SpongeSaturatedSpongeListener implements Listener {
    final AcidIsland plugin;

    public SpongeSaturatedSpongeListener(final AcidIsland pluginI) {
	plugin = pluginI;
    }

    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
	if (event.getBlock().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (!Settings.pistonMove && plugin.hasSponges(event.getBlocks())) {
		event.setCancelled(true);
	    }
	}
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
	if (event.getBlock().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (!Settings.pistonMove && event.isSticky() && plugin.isSponge(event.getRetractLocation().getBlock())) {
		event.setCancelled(true);
	    }
	}
    }

}

package com.wasteofplastic.acidisland;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class NetherPortals implements Listener {
    private final AcidIsland plugin;

    public NetherPortals(AcidIsland plugin) {
	this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onPlayerPortal(PlayerPortalEvent event) {
	if (event.isCancelled()) {
	    plugin.getLogger().info("PlayerPortalEvent was cancelled! AcidIsland NOT teleporting!");
	    return;
	}
	Location currentLocation = event.getFrom().clone();
	String currentWorld = currentLocation.getWorld().getName();
	if (!currentWorld.equalsIgnoreCase(Settings.worldName) && !currentWorld.equalsIgnoreCase(Settings.worldName + "_nether")) {
	    return;
	}
	//this.plugin.getLogger().info("Get from is " + currentLocation.toString());

	if (currentWorld.equalsIgnoreCase(Settings.worldName)) {
	    // Going to the nether
	    event.setTo(plugin.getServer().getWorld(Settings.worldName + "_nether").getSpawnLocation());
	    event.useTravelAgent(true);
	} else {
	    // Returning to island
	    event.setTo(plugin.getSafeHomeLocation(event.getPlayer().getUniqueId())); 
	    event.useTravelAgent(false);
	}
    }
}

package com.wasteofplastic.acidisland;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
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
	// Check that we know this player (they could have come from another world)
	Location destination = plugin.getSafeHomeLocation(event.getPlayer().getUniqueId());
	if (destination == null) {
	    event.getPlayer().sendMessage(ChatColor.YELLOW + "Type /island to start an island.");
	    event.setCancelled(true);
	    return;
	}
	if (currentWorld.equalsIgnoreCase(Settings.worldName)) {
	    // Going to the nether
	    event.setTo(plugin.getServer().getWorld(Settings.worldName + "_nether").getSpawnLocation());
	    event.useTravelAgent(true);
	} else {
	    // Returning to island
	    event.setTo(destination); 
	    event.useTravelAgent(false);
	}
    }

    // Nether portal spawn protection

    // Function to check proximity to nether spawn location
    private boolean awayFromSpawn(Player player) {
	Location spawn = player.getWorld().getSpawnLocation();
	Location loc = player.getLocation();
	if (spawn.distance(loc) < Settings.netherSpawnRadius) {
	    player.sendMessage(Locale.netherSpawnIsProtected);
	    return false;
	} else {
	    return true;
	}
    }
    /**
     * Prevents blocks from being broken
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(final BlockBreakEvent e) {
	//plugin.getLogger().info("Block break");
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")) {
	    //plugin.getLogger().info("Block break in acid island nether");
	    if (!awayFromSpawn(e.getPlayer()) && !e.getPlayer().isOp()) {

		e.setCancelled(true);
	    }
	}

    }

    /**
     * Prevents placing of blocks
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")) {
	    if (!awayFromSpawn(e.getPlayer()) && !e.getPlayer().isOp()) {		   
		e.setCancelled(true);
	    }
	}

    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")) {
	    if (!awayFromSpawn(e.getPlayer()) && !e.getPlayer().isOp()) {
		e.setCancelled(true);
	    }
	}

    }

    /**
     * This method protects players from PVP if it is not allowed and from arrows fired by other players
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
	// Check world
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")) {
	    return;
	}
	// If the target is not a player return
	if (!(e.getEntity() instanceof Player)) {
	    return;
	}
	// If PVP is okay then return
	if (Settings.allowPvP.equalsIgnoreCase("allow")) {
	    return;
	}
	// If the attacker is non-human and not an arrow then everything is okay
	if (!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Projectile)) {
	    return;
	}
	// Only damagers who are players or arrows are left
	// If the projectile is anything else than an arrow don't worry about it in this listener
	// Handle splash potions separately.
	if (e.getDamager() instanceof Arrow) {
	    Arrow arrow = (Arrow)e.getDamager();
	    // It really is an Arrow
	    if (arrow.getShooter() instanceof Player) {
		// Arrow shot by a player at another player
		if (Settings.allowPvP.equalsIgnoreCase("allow")) {
		    return;
		} else {
		    e.setCancelled(true);
		    return;
		}
	    }
	}
	return;
    }

    /**
     * Prevent the Nether spawn from being blown up
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onExplosion(final EntityExplodeEvent e) {
	// Check world
	if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")) {
	    return;
	}
	Location spawn = e.getLocation().getWorld().getSpawnLocation();
	Location loc = e.getLocation();
	if (spawn.distance(loc) < Settings.netherSpawnRadius) {
	    e.blockList().clear();
	}
    }

}

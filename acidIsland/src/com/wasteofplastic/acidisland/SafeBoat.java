/**
 * 
 */
package com.wasteofplastic.acidisland;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * @author ben This file improves the safety of boats in Acid Island It enables
 *         players to get out of boats without being dropped into the acid. It
 *         enables players to hit a boat and have it pop into their inventory
 *         immediately
 */
public class SafeBoat implements Listener {
    // Flags to indicate if a player has exited a boat recently or not
    private static HashMap<Player, Boolean> exitedBoat = new HashMap<Player, Boolean>();
    // private static HashMap<Player,Long> hitterTime = new
    // HashMap<Player,Long>();
    // private static HashMap<Player,Integer> hitBoat = new
    // HashMap<Player,Integer>();
    private final AcidIsland plugin;

    public SafeBoat(AcidIsland acidIsland) {
	plugin = acidIsland;
    }

    /**
     * @param e
     *            This event check throws the boat at a player when they hit it
     *            unless someone is in it
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onClick(VehicleDamageEvent e) {
	// plugin.getLogger().info("Damage event " + e.getDamage());
	// Find out what block is being clicked
	Vehicle boat = e.getVehicle();
	final World playerWorld = boat.getWorld();
	if (!playerWorld.getName().equalsIgnoreCase(Settings.worldName)) {
	    // Not the right world
	    return;
	}

	// This triggers if you hit a boat a few times
	if (!boat.getType().equals(EntityType.BOAT))
	    return;
	// plugin.getLogger().info("Boat ");
	// Find out who is doing the clicking
	Player p = (Player) e.getAttacker();
	if (p == null) {
	    return;
	}
	if (!boat.isEmpty()) {
	    return;
	}
	// Unless there is sufficient damage, return
	// TODO: if the damage is just 1, then the boat is being hit by a hand
	// or block
	// There has to be a way to detect if it has been done rapidly and then
	// step in and remove the boat
	// It will have to be player specific and track timing somehow.
	/*
	 * if (e.getDamage() < 2) { // Did this player hit the boat already if
	 * (hitBoat.containsKey(p)) { if
	 * (hitBoat.get(p).equals(boat.getEntityId())) { // It's the same boat
	 * did they hit it recently? if ((System.currentTimeMillis() -
	 * hitterTime.get(p)) < 500) { // Less than 500ms
	 * 
	 * } } } // Log that the player hit the boat and what boat it was
	 * hitBoat.put(p, boat.getEntityId()); hitterTime.put(p,
	 * System.currentTimeMillis()); e.setCancelled(true); return; } else {
	 */
	// Try to remove the boat and throw it at the player
	Location boatSpot = new Location(boat.getWorld(), boat.getLocation().getX(), boat.getLocation().getY() + 2, boat.getLocation().getZ());
	Location throwTo = new Location(boat.getWorld(), p.getLocation().getX(), p.getLocation().getY() + 1, p.getLocation().getZ());
	ItemStack newBoat = new ItemStack(Material.BOAT, 1);
	// Find the direction the boat should move in
	Vector dir = throwTo.toVector().subtract(boatSpot.toVector()).normalize();
	dir = dir.multiply(0.5);
	Entity newB = boat.getWorld().dropItem(boatSpot, newBoat);
	newB.setVelocity(dir);
	boat.remove();
	e.setCancelled(true);
	// Move the boat to the player inventory and remove the boat
	/*
	 * 
	 * HashMap<Integer, ItemStack> check =
	 * p.getInventory().addItem(newBoat); if (check.isEmpty()) {
	 * plugin.getLogger().info("Stored boat in inventory"); // Object
	 * successfully stored in the player's inventory boat.remove();
	 * e.setCancelled(true);
	 * 
	 * }
	 */
    }

    /**
     * @param e
     *            This function prevents boats from exploding when they hit
     *            something
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onBoatHit(VehicleDestroyEvent e) {
	// plugin.getLogger().info("Vehicle destroyed event called");
	final Entity boat = e.getVehicle();
	if (!boat.getType().equals(EntityType.BOAT)) {
	    // plugin.getLogger().info("Vehicle not a boat - it is a " +
	    // boat.getType().toString());
	    return;
	}
	if (!boat.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    // Not the right world
	    return;
	}

	if (!(e.getAttacker() instanceof Player)) {
	    // plugin.getLogger().info("Attacker is not a player so cancel event");
	    e.setCancelled(true);
	}
    }

    /**
     * @param e
     *            This event aims to put the player in a safe place when they
     *            exit the boat
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBoatExit(VehicleExitEvent e) {
	final Entity boat = e.getVehicle();
	if (!boat.getType().equals(EntityType.BOAT)) {
	    // Not a boat
	    return;
	}
	// LivingEntity entity = e.getExited();
	final Entity entityObj = (Entity) e.getExited();
	if (!(entityObj instanceof Player)) {
	    return;
	}
	final Player player = (Player) entityObj;
	final World playerWorld = player.getWorld();
	if (!playerWorld.getName().equalsIgnoreCase(Settings.worldName)) {
	    // Not the right world
	    return;
	}
	// Okay, so a player is getting out of a boat in the the right world.
	// Now...
	// plugin.getLogger().info("Player just exited a boat");
	// Find a safe place for the player to land
	int radius = 0;
	while (radius++ < 2) {
	    for (int x = player.getLocation().getBlockX() - radius; x < player.getLocation().getBlockX() + radius; x++) {
		for (int z = player.getLocation().getBlockZ() - radius; z < player.getLocation().getBlockZ() + radius; z++) {
		    for (int y = player.getLocation().getBlockY(); y < player.getLocation().getBlockY() + 2; y++) {
			// The safe location to tp to is actually +0.5 to x and
			// z.
			final Location loc = new Location(player.getWorld(), (double) (x + 0.5), (double) y, (double) (z + 0.5));
			// plugin.getLogger().info("XYZ is " + x + " " + y + " "
			// + z);
			// Make sure the location is safe
			if (AcidIsland.isSafeLocation(loc)) {
			    // plugin.getLogger().info("Safe!");
			    Bukkit.getServer().getScheduler().runTask(plugin, new Runnable() {
				@Override
				public void run() {
				    // plugin.getLogger().info("Teleporting to "
				    // + loc.toString());
				    player.teleport(loc);
				    exitedBoat.put(player, false);
				}
			    });
			    // This prevents the player from being acid attacked
			    // during the tick when they may be in the water
			    exitedBoat.put(player, true);
			    return;
			}
		    }
		}
	    }
	}
    }

    /**
     * @param player
     * @return true if the player just exited a boat and false otherwise Resets
     *         the exited boat flag when it is called
     */
    public static boolean exitedBoat(Player player) {
	if (exitedBoat.containsKey(player)) {
	    boolean status = exitedBoat.get(player);
	    // plugin.getLogger().info("Status is " + status);
	    // And reset to false
	    exitedBoat.put(player, false);
	    return status;
	}
	return false;
    }
}
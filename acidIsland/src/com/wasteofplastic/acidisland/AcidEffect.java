package com.wasteofplastic.acidisland;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * @author ben
 * Applies the acid effect to players
 */
public class AcidEffect implements Listener {
    private final AcidIsland plugin;
    private List<Player> burningPlayers = new ArrayList<Player>();

    public AcidEffect(final AcidIsland pluginI) {
	plugin = pluginI;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
	burningPlayers.remove((Player) e.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
	final Player player = e.getPlayer();
	// Fast checks
	if (player.isDead()) {
	    return;
	}
	// Check that they are in the AcidIsland world
	if (!player.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	// If the player is an op in Creative mode, acid does not hurt
	if (player.getGameMode().equals(GameMode.CREATIVE)) {
	    return;
	}

	// Slow checks
	final Location playerLoc = player.getLocation();
	final Block block = playerLoc.getBlock();
	// If they are not in liquid, then return
	if (!block.isLiquid()) {
	    return;
	}
	// Find out if they are at the bottom of the sea and if so bounce them
	// back up
	if (playerLoc.getBlockY() < 1) {
	    final Vector v = new Vector(player.getVelocity().getX(), 1D, player.getVelocity().getZ());
	    player.setVelocity(v);
	}
	// If they are already burning in acid then return
	if (burningPlayers.contains(player)) {
	    return;
	}

	if (block.getType().equals(Material.STATIONARY_WATER) || block.getType().equals(Material.WATER)) {
	    // Check if player has just exited a boat - in which case, they are
	    // immune for 1 tick
	    // This is needed because safeboat.java cannot teleport the player
	    // for 1 tick
	    // Don't remove this!!
	    if (SafeBoat.exitedBoat(player)) {
		return;
	    }
	    // Check if player is in a boat
	    Entity playersVehicle = player.getVehicle();
	    if (playersVehicle != null) {
		// They are in a Vehicle
		if (playersVehicle.getType().equals(EntityType.BOAT)) {
		    // I'M ON A BOAT! I'M ON A BOAT! A %^&&* BOAT!
		    return;
		}
	    }
	    // Check if player has an active water potion or not
	    Collection<PotionEffect> activePotions = player.getActivePotionEffects();
	    for (PotionEffect s : activePotions) {
		// plugin.getLogger().info("Potion is : " +
		// s.getType().toString());
		if (s.getType().equals(PotionEffectType.WATER_BREATHING)) {
		    // Safe!
		    return;
		    // plugin.getLogger().info("Water breathing potion protection!");
		}
	    }
	    // ACID!
	    // Put the player into the acid list 
	    burningPlayers.add(player);
	    // This runnable continuously hurts the player even if they are not
	    // moving but are in acid.
	    new BukkitRunnable() {
		@Override
		public void run() {
		    if (player.isDead()) {
			burningPlayers.remove(player);
			this.cancel();
		    } else if (player.getLocation().getBlock().isLiquid()
			    && player.getLocation().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
			plugin.getLogger().info("Damage setting = " + Settings.acidDamage);
			//plugin.getLogger().info("Damage to player = " + (Settings.general_acidDamage - Settings.general_acidDamage * getDamageReduced(player)));
			plugin.getLogger().info("Player health is " + player.getHealth());
			// Apply additional potion effects
			for (PotionEffectType t: Settings.acidDamageType) {
			    plugin.getLogger().info("Applying " + t.toString());
			    //player.addPotionEffect(new PotionEffect(t, 20, amplifier));
			    if (t.equals(PotionEffectType.BLINDNESS)
				    || t.equals(PotionEffectType.CONFUSION)
				    || t.equals(PotionEffectType.HUNGER)
				    || t.equals(PotionEffectType.SLOW)
				    || t.equals(PotionEffectType.SLOW_DIGGING)
				    || t.equals(PotionEffectType.WEAKNESS)
				    ) {
				player.addPotionEffect(new PotionEffect(t, 600, 1));
			    } else {
				// Poison
				player.addPotionEffect(new PotionEffect(t, 200, 1));
			    }
			}
			//double health = player.getHealth();
			double health = player.getHealth() - (Settings.acidDamage - Settings.acidDamage * getDamageReduced(player));
			if (health < 0D) {
			    health = 0D;
			} else if (health > 20D) {
			    health = 20D;
			}
			player.setHealth(health);
			player.getWorld().playSound(playerLoc, Sound.FIZZ, 2F, 2F);
			
			
		    } else {
			burningPlayers.remove(player);
			// plugin.getLogger().info("Cancelled!");
			this.cancel();
		    }
		}
	    }.runTaskTimer(plugin, 0L, 20L);
	}
    }

    /**
     * Enables changing of obsidian back into lava
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(final PlayerInteractEvent e) {
	if (plugin.playerIsOnIsland(e.getPlayer())) {
	    boolean otherOb = false;
	    if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && e.getPlayer().getItemInHand().getType().equals(Material.BUCKET)
		    && e.getClickedBlock().getType().equals(Material.OBSIDIAN)) {
		// Look around to see if this is a lone obsidian block
		Block b = e.getClickedBlock();
		for (int x = -2; x <= 2; x++) {
		    for (int y = -2; y <= 2; y++) {
			for (int z = -2; z <= 2; z++) {
			    final Block testBlock = b.getWorld().getBlockAt(b.getX() + x, b.getY() + y, b.getZ() + z);
			    if ((x != 0 || y != 0 || z != 0) && testBlock.getType().equals(Material.OBSIDIAN)) {
				otherOb = true;
			    }
			}
		    }
		}
		if (!otherOb) {
		    e.getPlayer().sendMessage(ChatColor.YELLOW + Locale.changingObsidiantoLava);
		    e.getPlayer().getInventory().removeItem(new ItemStack(Material.BUCKET, 1));
		    e.getPlayer().getInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
		    e.getPlayer().updateInventory();
		    e.getClickedBlock().setType(Material.AIR);
		}
	    }
	}
    }

    /**
     * @param player
     * @return A double between 0.0 and 0.80 that reflects how much armor the
     *         player has on. The higher the value, the more protection they
     *         have.
     */
    static public double getDamageReduced(Player player) {
	org.bukkit.inventory.PlayerInventory inv = player.getInventory();
	ItemStack boots = inv.getBoots();
	ItemStack helmet = inv.getHelmet();
	ItemStack chest = inv.getChestplate();
	ItemStack pants = inv.getLeggings();
	double red = 0.0;
	if (helmet != null) {
	    if (helmet.getType() == Material.LEATHER_HELMET)
		red = red + 0.04;
	    else if (helmet.getType() == Material.GOLD_HELMET)
		red = red + 0.08;
	    else if (helmet.getType() == Material.CHAINMAIL_HELMET)
		red = red + 0.08;
	    else if (helmet.getType() == Material.IRON_HELMET)
		red = red + 0.08;
	    else if (helmet.getType() == Material.DIAMOND_HELMET)
		red = red + 0.12;
	}
	if (boots != null) {
	    if (boots.getType() == Material.LEATHER_BOOTS)
		red = red + 0.04;
	    else if (boots.getType() == Material.GOLD_BOOTS)
		red = red + 0.04;
	    else if (boots.getType() == Material.CHAINMAIL_BOOTS)
		red = red + 0.04;
	    else if (boots.getType() == Material.IRON_BOOTS)
		red = red + 0.08;
	    else if (boots.getType() == Material.DIAMOND_BOOTS)
		red = red + 0.12;
	}
	// Pants
	if (pants != null) {
	    if (pants.getType() == Material.LEATHER_LEGGINGS)
		red = red + 0.08;
	    else if (pants.getType() == Material.GOLD_LEGGINGS)
		red = red + 0.12;
	    else if (pants.getType() == Material.CHAINMAIL_LEGGINGS)
		red = red + 0.16;
	    else if (pants.getType() == Material.IRON_LEGGINGS)
		red = red + 0.20;
	    else if (pants.getType() == Material.DIAMOND_LEGGINGS)
		red = red + 0.24;
	}
	// Chest plate
	if (chest != null) {
	    if (chest.getType() == Material.LEATHER_CHESTPLATE)
		red = red + 0.12;
	    else if (chest.getType() == Material.GOLD_CHESTPLATE)
		red = red + 0.20;
	    else if (chest.getType() == Material.CHAINMAIL_CHESTPLATE)
		red = red + 0.20;
	    else if (chest.getType() == Material.IRON_CHESTPLATE)
		red = red + 0.24;
	    else if (chest.getType() == Material.DIAMOND_CHESTPLATE)
		red = red + 0.32;
	}
	return red;
    }
}

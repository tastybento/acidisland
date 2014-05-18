package com.wasteofplastic.acidisland;


import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.potion.Potion;

/**
 * @author ben
 * Provides protection to islands
 */
public class IslandGuard implements Listener {
    private final AcidIsland plugin;

    public IslandGuard(final AcidIsland plugin) {
	this.plugin = plugin;

    }

    // Check all the events and if they are not on the player's island then cancel them
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(final BlockBreakEvent e) {
	if (Settings.worldName.equalsIgnoreCase(e.getPlayer().getWorld().getName())) {
	    if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		e.setCancelled(true);
	    }
	}
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
	// Check world
	if (!Settings.worldName.equalsIgnoreCase(e.getEntity().getWorld().getName())) {
	    return;
	}
	// If the attacked is non-human and not an arrow then everything is okay
	if (!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Projectile)) {
	    return;
	}
	// Only damagers who are players or arrows are left
	// If the damaged entity is a player and PVP is okay then return
	if ((e.getEntity() instanceof Player) && Settings.allowPvP.equalsIgnoreCase("allow")) {
	    return;
	}
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

    @EventHandler(priority = EventPriority.HIGH)
    public void OnBlockBreak(final BlockBreakEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		e.setCancelled(true);
	    }
	}
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		e.setCancelled(true);
	    }
	}
    }

    // Prevent sleeping in other beds
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBedEnter(final PlayerBedEnterEvent e) {
	// Check world
	if (Settings.worldName.equalsIgnoreCase(e.getPlayer().getWorld().getName())) {
	    if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		e.setCancelled(true);
	    }
	}
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBreakHanging(final HangingBreakByEntityEvent e) {
	if (e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (e.getRemover() instanceof Player) {
		Player p = (Player)e.getRemover();
		if (!plugin.playerIsOnIsland(p) && !p.isOp()) {
		    p.sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		}
	    }
	}
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		e.setCancelled(true);
	    }
	}
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketFill(final PlayerBucketFillEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		e.setCancelled(true);
	    }
	}
    }
    // Protect sheep
    @EventHandler(priority = EventPriority.NORMAL)
    public void onShear(final PlayerShearEntityEvent e) {
	if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		e.setCancelled(true);
	    }
	}	    
    }

    // Stop interactions - messy refactor
    // TODO: Refactor!
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(final PlayerInteractEvent e) {
	if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return;
	}
	if (plugin.playerIsOnIsland(e.getPlayer()) || e.getPlayer().isOp()) {
	    // You can do anything on your island or if you are Op
	    return;
	}
	// Player is off island
	//plugin.getLogger().info("DEBUG: clicked block " + e.getClickedBlock());
	//plugin.getLogger().info("DEBUG: Material " + e.getMaterial());
	// Check for disallowed clicked blocks
	if (e.getClickedBlock() != null) {
	    if (e.getClickedBlock().getType().equals(Material.WOODEN_DOOR)
		    || e.getClickedBlock().getType().equals(Material.TRAP_DOOR)
		    || e.getClickedBlock().getType().equals(Material.SOIL)
		    || e.getClickedBlock().getType().equals(Material.CHEST)) {
		e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		e.setCancelled(true);
		return;
	    }
	}
	// Check for disallowed in-hand items
	if (e.getMaterial() != null) {
	    if (e.getMaterial().equals(Material.BOAT) && (e.getClickedBlock() != null && !e.getClickedBlock().isLiquid())) {
		// Trying to put a boat on non-liquid
		e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		e.setCancelled(true);
		return;
	    }
	    if (e.getMaterial().equals(Material.ENDER_PEARL)) {
		e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		e.setCancelled(true);
		return;
	    } else if (e.getMaterial().equals(Material.POTION) && e.getItem().getDurability() != 0) {
		// Potion
		//plugin.getLogger().info("DEBUG: potion");
		try {
		    Potion p = Potion.fromItemStack(e.getItem());
		    if (!p.isSplash()) {
			//plugin.getLogger().info("DEBUG: not a splash potion");
			return;
		    } else {
			e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
			e.setCancelled(true);
		    }
		} catch (Exception ex) {
		}
	    }
	    // Everything else is okay
	}
    }
}
/*
	@EventHandler
	public void onEntityInteract(EntityInteractEvent event) {
		if (!(event.getEntity() instanceof Arrow))
			return;

		Arrow arrow = (Arrow) event.getEntity();

		if (!(arrow.getShooter() instanceof Player))
			return;

		Player damager = (Player) arrow.getShooter();

		if (damager == null)
			return;

		if (damager.hasPermission("usb.mod.bypassprotection"))
			return;

		if (!uSkyBlock.getInstance().playerIsOnIsland(damager))
			event.setCancelled(true);
	}
}
}*/

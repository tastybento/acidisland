package com.wasteofplastic.acidisland;


import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.ItemFrame;
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

    /**
     * Prevents blocks from being broken
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(final BlockBreakEvent e) {
	if (!Settings.allowBreakBlocks) {
	    if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		}
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
	if (!Settings.worldName.equalsIgnoreCase(e.getEntity().getWorld().getName())) {
	    return;
	}
	// Check to see if it's an item frame
	if (e.getEntity() instanceof ItemFrame) {
	    if (!Settings.allowBreakBlocks) {
		if (e.getDamager() instanceof Player) {
		    if (!plugin.playerIsOnIsland((Player)e.getDamager()) && !((Player)e.getDamager()).isOp()) {
			((Player)e.getDamager()).sendMessage(ChatColor.RED + "Island protected.");
			e.setCancelled(true);
		    }
		}
	    }
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
     * Prevents placing of blocks
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
	if (!Settings.allowPlaceBlocks) {
	    if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		}
	    }
	}
    }

    // Prevent sleeping in other beds
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBedEnter(final PlayerBedEnterEvent e) {
	if (!Settings.allowBedUse) {
	    // Check world
	    if (Settings.worldName.equalsIgnoreCase(e.getPlayer().getWorld().getName())) {
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		}
	    }
	}
    }
    /**
     * Prevents the breakage of hanging items
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBreakHanging(final HangingBreakByEntityEvent e) {
	if (!Settings.allowBreakBlocks) {
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
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
	if (!Settings.allowBucketUse) {
	    if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		}
	    }
	}
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketFill(final PlayerBucketFillEvent e) {
	if (!Settings.allowBucketUse) {
	    if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		}
	    }
	}
    }

    // Protect sheep
    @EventHandler(priority = EventPriority.NORMAL)
    public void onShear(final PlayerShearEntityEvent e) {
	if (!Settings.allowShearing) {	
	    if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
		if (!plugin.playerIsOnIsland(e.getPlayer()) && !e.getPlayer().isOp()) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		}
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
	// Check for disallowed clicked blocks
	if (e.getClickedBlock() != null) {
	    //plugin.getLogger().info("DEBUG: clicked block " + e.getClickedBlock());
	    //plugin.getLogger().info("DEBUG: Material " + e.getMaterial());

	    switch (e.getClickedBlock().getType()) {
	    case WOODEN_DOOR:
	    case TRAP_DOOR:
		if (!Settings.allowDoorUse) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case CHEST:
	    case TRAPPED_CHEST:
	    case ENDER_CHEST:
	    case DISPENSER:
	    case DROPPER:
	    case HOPPER:
	    case HOPPER_MINECART:
	    case STORAGE_MINECART:
		if (!Settings.allowChestAccess) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case SOIL:
		if (!Settings.allowCropTrample) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case BREWING_STAND:
	    case CAULDRON:
		if (!Settings.allowBrewing) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case CAKE_BLOCK:
		break;
	    case DIODE:
	    case DIODE_BLOCK_OFF:
	    case DIODE_BLOCK_ON:
	    case REDSTONE_COMPARATOR_ON:
	    case REDSTONE_COMPARATOR_OFF:
		if (!Settings.allowRedStone) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case ENCHANTMENT_TABLE:
		break;
	    case FURNACE:
	    case BURNING_FURNACE:
		if (!Settings.allowFurnaceUse) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case ICE:
		break;
	    case ITEM_FRAME:
		break;
	    case JUKEBOX:
	    case NOTE_BLOCK:
		if (!Settings.allowMusic) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case PACKED_ICE:
		break;
	    case STONE_BUTTON:
	    case WOOD_BUTTON:
	    case LEVER:
		if (!Settings.allowLeverButtonUse) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		    return; 
		}	
		break;
	    case TNT:
		break;
	    case WORKBENCH:
		if (!Settings.allowCrafting) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		    return; 
		}
		break;
	    default:
		break;
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
		if (!Settings.allowEnderPearls) {
		    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
		    e.setCancelled(true);
		}
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
			// Splash potions are allowed only if PVP is allowed
			if (!Settings.allowPvP.equalsIgnoreCase("allow")) {
			    e.getPlayer().sendMessage(ChatColor.RED + "Island protected.");
			    e.setCancelled(true);
			}
		    }
		} catch (Exception ex) {
		}
	    }
	    // Everything else is okay
	}
    }
}


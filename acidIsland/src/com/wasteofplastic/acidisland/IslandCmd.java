package com.wasteofplastic.acidisland;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class IslandCmd implements CommandExecutor {
    public boolean busyFlag = true;
    public Location Islandlocation;
    private AcidIsland plugin;
    // The island reset confirmation
    private HashMap<UUID,Boolean> confirm = new HashMap<UUID,Boolean>();

    /**
     * Invite list - invited player name string (key), inviter name string (value)
     */
    private final HashMap<UUID, UUID> inviteList = new HashMap<UUID, UUID>();
    private PlayerCache players;
    // The time a player has to wait until they can reset their island again
    private HashMap<UUID, Long> resetWaitTime = new HashMap<UUID, Long>();

    /**
     * Constructor
     * 
     * @param acidIsland
     * @param players 
     */
    public IslandCmd(AcidIsland acidIsland, PlayerCache players) {

	// Plugin instance
	this.plugin = acidIsland;
	this.players = players;
    }

    /*
     * PARTY SECTION!
     */

    /**
     * Adds a player to a team. The player and the teamleader MAY be the same
     * 
     * @param playerUUID
     * @param teamLeader
     * @return
     */
    public boolean addPlayertoTeam(final UUID playerUUID, final UUID teamLeader) {
	// Only add online players
	if (!plugin.getServer().getPlayer(playerUUID).isOnline() || !plugin.getServer().getPlayer(teamLeader).isOnline()) {
	    plugin.getLogger().info("Can only add player to a team if both player and leader are online.");
	    return false;
	}
	plugin.getLogger().info("Adding player: " + playerUUID + " to team with leader: " + teamLeader);
	plugin.getLogger().info("The island location is: " + players.getIslandLocation(teamLeader));
	plugin.getLogger().info("The leader's home location is: " + players.getHomeLocation(teamLeader) + " (may be different or null)");

	// Set the player's team giving the team leader's name and the team's island
	// location
	players.setJoinTeam(playerUUID, teamLeader, players.getIslandLocation(teamLeader));
	// If the player's name and the team leader are NOT the same when this
	// method is called then set the player's home location to the leader's
	// home location
	// if it exists, and if not set to the island location
	if (!playerUUID.equals(teamLeader)) {
	    if (players.getHomeLocation(teamLeader) != null) {
		players.setHomeLocation(playerUUID,players.getHomeLocation(teamLeader));
		plugin.getLogger().info("Setting player's home to the leader's home location");		
	    } else {
		//TODO - concerned this may be a bug
		players.setHomeLocation(playerUUID, players.getIslandLocation(teamLeader));
		plugin.getLogger().info("Setting player's home to the team island location");
	    }
	    // If the leader's member list does not contain player then add it
	    if (!players.getMembers(teamLeader).contains(playerUUID)) {
		players.addTeamMember(teamLeader,playerUUID);
	    }
	    // If the leader's member list does not contain their own name then
	    // add it
	    if (!players.getMembers(teamLeader).contains(teamLeader)) {
		players.addTeamMember(teamLeader, teamLeader);
	    }
	}
	return true;
    }

    /**
     * Removes a player from a team run by teamleader
     * 
     * @param player
     * @param teamleader
     */
    public void removePlayerFromTeam(final UUID player, final UUID teamleader) {
	// Remove player from the team
	players.removeMember(teamleader,player);
	// If player is online
	// If player is not the leader of their own team
	if (!player.equals(teamleader)) {
	    players.setLeaveTeam(player);
	    players.setHomeLocation(player, null);
	    players.setIslandLocation(player, null);
	} else {
	    // Ex-Leaders keeps their island, but the rest of the team items are removed
	    players.setLeaveTeam(player);	    
	}
    }

    /**
     * Makes an island
     * 
     * @param sender
     *            player who issued the island command
     * @return true if successful
     */
    private Location newIsland(final CommandSender sender) {
	// Player who is issuing the command
	final Player player = (Player) sender;
	final UUID playerUUID = player.getUniqueId();
	//final Players p = players.get(player.getName());
	// Island building is done in tasks
	// Get the location of the last island generated
	final Location last = new Location(AcidIsland.getIslandWorld(), 0D, Settings.sea_level, 0D);
	// Find the next free spot
	Location next;
	next = nextGridLocation(last);
	while (plugin.islandAtLocation(next)) {
	    next = nextGridLocation(next);
	}
	plugin.setNewIsland(true);
	Location cowSpot = generateIslandBlocks(next.getBlockX(), next.getBlockZ(), player, AcidIsland.getIslandWorld());
	plugin.setNewIsland(false);
	//plugin.getLogger().info("DEBUG: player ID is: " + playerUUID.toString());
	players.setHasIsland(playerUUID,true);
	//plugin.getLogger().info("DEBUG: Set island to true - actually is " + players.hasIsland(playerUUID));

	players.setIslandLocation(playerUUID,next);
	//plugin.getLogger().info("DEBUG: player island location is " + players.getIslandLocation(playerUUID).toString());
	// Teleport the player to a safe place
	//plugin.homeTeleport(player);
	players.save(playerUUID);
	/***************
	 * NOTE: Important section - make sure this is applied any time an
	 * island is reset!
	 */
	resetPlayer(player);
	if (Settings.resetMoney) {
	    resetMoney(player);
	}
	// Remove any mobs if they just so happen to be around in the
	// vicinity
	/*
	final Iterator<Entity> ents = player.getNearbyEntities(50.0D, 250.0D, 50.0D).iterator();
	int numberOfCows = 0;
	while (ents.hasNext()) {
	    final Entity tempent = ents.next();
	    // Remove anything except for the player himself and the cow (!)
	    if (!(tempent instanceof Player) && !tempent.getType().equals(EntityType.COW)) {
		plugin.getLogger().warning("Removed an " + tempent.getType().toString() + " when creating island for " + player.getName());
		tempent.remove();
	    } else if (tempent.getType().equals(EntityType.COW)) {
		numberOfCows++;
		if (numberOfCows > 1) {
		    plugin.getLogger().warning("Removed an extra cow when creating island for " + player.getName());
		    tempent.remove();
		}
	    }
	}*/
	// Done
	return cowSpot;
    }

    private void resetMoney(Player player) {
	// Set player's balance in acid island to the starting balance
	try {
	    // plugin.getLogger().info("DEBUG: " + player.getName() + " " +
	    // Settings.general_worldName);
	    if (VaultHelper.econ == null) {
		//plugin.getLogger().warning("DEBUG: econ is null!");
		VaultHelper.setupEconomy();
	    }
	    Double playerBalance = VaultHelper.econ.getBalance(player, Settings.worldName);
	    // plugin.getLogger().info("DEBUG: playerbalance = " +
	    // playerBalance);
	    // Round the balance to 2 decimal places and slightly down to
	    // avoid issues when withdrawing the amount later
	    BigDecimal bd = new BigDecimal(playerBalance);
	    bd = bd.setScale(2, RoundingMode.HALF_DOWN);
	    playerBalance = bd.doubleValue();
	    // plugin.getLogger().info("DEBUG: playerbalance after rounding = "
	    // + playerBalance);
	    if (playerBalance != Settings.startingMoney)  {
		if (playerBalance > Settings.startingMoney) {
		    Double difference = playerBalance - Settings.startingMoney;
		    EconomyResponse response = VaultHelper.econ.withdrawPlayer(player, Settings.worldName, difference);
		    // plugin.getLogger().info("DEBUG: withdrawn");
		    if (response.transactionSuccess()) {
			plugin.getLogger().info(
				"FYI:" + player.getName() + " had " + VaultHelper.econ.format(playerBalance) + " when they typed /island and it was set to " + Settings.startingMoney);
		    } else {
			plugin.getLogger().warning(
				"Problem trying to withdraw " + playerBalance + " from " + player.getName() + "'s account when they typed /island!");
		    }
		} else {
		    Double difference = Settings.startingMoney - playerBalance;
		    EconomyResponse response = VaultHelper.econ.depositPlayer(player, Settings.worldName, difference);
		    if (response.transactionSuccess()) {
			plugin.getLogger().info(
				"FYI:" + player.getName() + " had " + VaultHelper.econ.format(playerBalance) + " when they typed /island and it was set to " + Settings.startingMoney);
		    } else {
			plugin.getLogger().warning(
				"Problem trying to deposit " + playerBalance + " from " + player.getName() + "'s account when they typed /island!");
		    }

		}
	    }
	} catch (final Exception e) {
	    plugin.getLogger().severe("Error trying to zero " + player.getName() + "'s account when they typed /island!");
	    plugin.getLogger().severe(e.getMessage());
	}

    }
    /**
     * Resets a player's inventory, armor slots, equipment, enderchest and potion effects
     * @param player
     */
    private void resetPlayer(Player player) {
	// Clear their inventory and equipment and set them as survival
	player.getInventory().clear(); // Javadocs are wrong - this does not
	// clear armor slots! So...
	player.getInventory().setHelmet(null);
	player.getInventory().setChestplate(null);
	player.getInventory().setLeggings(null);
	player.getInventory().setBoots(null);
	player.getEquipment().clear();
	player.setGameMode(GameMode.SURVIVAL);
	// Clear any Enderchest contents
	final ItemStack[] items = new ItemStack[player.getEnderChest().getContents().length];
	player.getEnderChest().setContents(items);
	// Clear any potion effects
	for (PotionEffect effect : player.getActivePotionEffects())
	    player.removePotionEffect(effect.getType());	
    }

    /**
     * Creates an island block by block
     * 
     * @param x
     * @param z
     * @param player
     * @param world
     */
    private Location generateIslandBlocks(final int x, final int z, final Player player, final World world) {
	Location cowSpot = null;
	// Check if there is a schematic
	File schematicFile = new File(plugin.getDataFolder(), "island.schematic");
	if (schematicFile.exists()) {    
	    plugin.getLogger().info("Trying to load island schematic...");
	    Schematic island = null;

	    try {
		island = Schematic.loadSchematic(schematicFile);
		Location islandLoc = new Location(world,x,Settings.sea_level,z);
		cowSpot = Schematic.pasteSchematic(world, islandLoc, island, player);
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		plugin.getLogger().severe("Could not load island schematic! Error in file.");
		e.printStackTrace();
	    }
	    return cowSpot;
	}
	// Build island layer by layer
	// Start from the base
	// half sandstone; half sand
	int y = 0;
	for (int x_space = x - 4; x_space <= x + 4; x_space++) {
	    for (int z_space = z - 4; z_space <= z + 4; z_space++) {
		final Block b = world.getBlockAt(x_space, y, z_space);
		b.setType(Material.BEDROCK);
	    }
	}
	for (y = 1; y < Settings.sea_level + 5; y++) {
	    for (int x_space = x - 4; x_space <= x + 4; x_space++) {
		for (int z_space = z - 4; z_space <= z + 4; z_space++) {
		    final Block b = world.getBlockAt(x_space, y, z_space);
		    if (y < (Settings.sea_level / 2)) {
			b.setType(Material.SANDSTONE);
		    } else {
			b.setType(Material.SAND);
		    }
		}
	    }
	}
	// Then cut off the corners to make it round-ish
	for (y = 0; y < Settings.sea_level + 5; y++) {
	    for (int x_space = x - 4; x_space <= x + 4; x_space += 8) {
		for (int z_space = z - 4; z_space <= z + 4; z_space += 8) {
		    final Block b = world.getBlockAt(x_space, y, z_space);
		    b.setType(Material.STATIONARY_WATER);
		}
	    }
	}
	// Add some grass
	for (y = Settings.sea_level + 4; y < Settings.sea_level + 5; y++) {
	    for (int x_space = x - 2; x_space <= x + 2; x_space++) {
		for (int z_space = z - 2; z_space <= z + 2; z_space++) {
		    final Block blockToChange = world.getBlockAt(x_space, y, z_space);
		    blockToChange.setType(Material.GRASS);
		}
	    }
	}
	// Place bedrock - MUST be there (ensures island are not overwritten
	Block b = world.getBlockAt(x, Settings.sea_level, z);
	b.setType(Material.BEDROCK);
	// Then add some more dirt in the classic shape
	y = Settings.sea_level + 3;
	for (int x_space = x - 2; x_space <= x + 2; x_space++) {
	    for (int z_space = z - 2; z_space <= z + 2; z_space++) {
		b = world.getBlockAt(x_space, y, z_space);
		b.setType(Material.DIRT);
	    }
	}
	b = world.getBlockAt(x - 3, y, z);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x + 3, y, z);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x, y, z - 3);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x, y, z + 3);
	b.setType(Material.DIRT);
	y = Settings.sea_level + 2;
	for (int x_space = x - 1; x_space <= x + 1; x_space++) {
	    for (int z_space = z - 1; z_space <= z + 1; z_space++) {
		b = world.getBlockAt(x_space, y, z_space);
		b.setType(Material.DIRT);
	    }
	}
	b = world.getBlockAt(x - 2, y, z);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x + 2, y, z);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x, y, z - 2);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x, y, z + 2);
	b.setType(Material.DIRT);
	y = Settings.sea_level + 1;
	b = world.getBlockAt(x - 1, y, z);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x + 1, y, z);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x, y, z - 1);
	b.setType(Material.DIRT);
	b = world.getBlockAt(x, y, z + 1);
	b.setType(Material.DIRT);

	// Add island items
	y = Settings.sea_level;
	// Add tree (natural)
	final Location treeLoc = new Location(world,x,y + 5D, z);
	world.generateTree(treeLoc, TreeType.ACACIA);
	// Place the cow
	cowSpot = new Location(world, x, (Settings.sea_level+5), z-2);

	// Place a helpful sign in front of player
	Block blockToChange = world.getBlockAt(x, Settings.sea_level + 5, z + 3);
	blockToChange.setType(Material.SIGN_POST);
	Sign sign = (Sign) blockToChange.getState();
	sign.setLine(0, ChatColor.BLUE + "[Acid Island]");
	sign.setLine(1, player.getName());
	String[] lore = Locale.acidLore.split("\n");
	if (lore.length >2) {
	    sign.setLine(2, lore[0] + " " + lore[1]);
	    sign.setLine(3, lore[2]);
	}
	((org.bukkit.material.Sign) sign.getData()).setFacingDirection(BlockFace.NORTH);
	sign.update();
	// Place the chest - no need to use the safe spawn function because we
	// know what this island looks like
	blockToChange = world.getBlockAt(x, Settings.sea_level + 5, z + 1);
	blockToChange.setType(Material.CHEST);
	// Fill the chest
	final Chest chest = (Chest) blockToChange.getState();
	final Inventory inventory = chest.getInventory();
	inventory.clear();
	inventory.setContents(Settings.chestItems);
	return cowSpot;
    }

    /**
     * Finds the next free island spot based off the last known island Uses
     * island_distance setting from the config file Builds up in a grid fashion
     * 
     * @param lastIsland
     * @return
     */
    private Location nextGridLocation(final Location lastIsland) {
	//plugin.getLogger().info("DEBUG nextIslandLocation");
	final int x = lastIsland.getBlockX();
	final int z = lastIsland.getBlockZ();
	final Location nextPos = lastIsland;
	if (x < z) {
	    if (-1 * x < z) {
		nextPos.setX(nextPos.getX() + Settings.islandDistance);
		return nextPos;
	    }
	    nextPos.setZ(nextPos.getZ() + Settings.islandDistance);
	    return nextPos;
	}
	if (x > z) {
	    if (-1 * x >= z) {
		nextPos.setX(nextPos.getX() - Settings.islandDistance);
		return nextPos;
	    }
	    nextPos.setZ(nextPos.getZ() - Settings.islandDistance);
	    return nextPos;
	}
	if (x <= 0) {
	    nextPos.setZ(nextPos.getZ() + Settings.islandDistance);
	    return nextPos;
	}
	nextPos.setZ(nextPos.getZ() - Settings.islandDistance);
	return nextPos;
    }

    /**
     * Calculates the island level
     * @param player - Player object of player who is asking
     * @param islandPlayer - UUID of the player's island that is being requested
     * @return - true if successful.
     */
    public boolean calculateIslandLevel(final Player player, final UUID islandPlayer) {
	if (!busyFlag) {
	    player.sendMessage(ChatColor.RED + Locale.islanderrorLevelNotReady);
	    plugin.getLogger().info(player.getName() + " tried to use /island info but someone else used it first!");
	    return false;
	}
	busyFlag = false;
	if (!players.hasIsland(islandPlayer) && !players.inTeam(islandPlayer)) {
	    player.sendMessage(ChatColor.RED + Locale.islanderrorInvalidPlayer);
	    busyFlag = true;
	    return false;
	}
	plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
	    public void run() {
		plugin.getLogger().info("Calculating island level");
		int oldLevel = players.getIslandLevel(islandPlayer);
		try {
		    Location l;
		    if (players.inTeam(islandPlayer)) {
			l = players.getTeamIslandLocation(islandPlayer);
		    } else {
			l = players.getIslandLocation(islandPlayer);
		    }
		    int blockcount = 0;
		    if (player.getUniqueId().equals(islandPlayer)) {
			int cobblecount = 0;
			final int px = l.getBlockX();
			final int py = l.getBlockY();
			final int pz = l.getBlockZ();
			for (int x = -(Settings.island_protectionRange / 2); x <= (Settings.island_protectionRange / 2); x++) {
			    for (int y = 0; y <= 255; y++) {
				for (int z = -(Settings.island_protectionRange / 2); z <= (Settings.island_protectionRange / 2); z++) {
				    final Block b = new Location(l.getWorld(), px + x, py + y, pz + z).getBlock();
				    final Material blockType = b.getType();
				    switch (blockType) {
				    // Cobblestone
				    case COBBLESTONE:
					cobblecount++;
					if (cobblecount < 10000) {
					    blockcount++;
					}
					// 1 value
				    case ACACIA_STAIRS:
				    case BIRCH_WOOD_STAIRS:
				    case BED_BLOCK:
				    case WOOL:
				    case CARPET:
				    case COBBLESTONE_STAIRS:
				    case COBBLE_WALL:
				    case DARK_OAK_STAIRS:
				    case DOUBLE_STEP:
				    case FENCE:
				    case FENCE_GATE:
				    case GLOWSTONE:
				    case GRAVEL:
				    case HUGE_MUSHROOM_1:
				    case HUGE_MUSHROOM_2:
				    case JACK_O_LANTERN:
				    case JUNGLE_WOOD_STAIRS:
				    case LADDER:
				    case LEVER:
				    case LOG:
				    case LOG_2:
				    case NETHERRACK:
				    case QUARTZ_BLOCK:
				    case QUARTZ_STAIRS:
				    case RAILS:
				    case SANDSTONE_STAIRS:
				    case SIGN_POST:
				    case STAINED_GLASS_PANE:
				    case SPRUCE_WOOD_STAIRS:
				    case STEP:
				    case STONE:
				    case STONE_BUTTON:
				    case THIN_GLASS:
				    case WALL_SIGN:
				    case WOOD:
				    case WOODEN_DOOR:
				    case WOOD_BUTTON:
				    case WOOD_DOUBLE_STEP:
				    case WOOD_PLATE:
				    case WOOD_STAIRS:
				    case WOOD_STEP:
				    case WORKBENCH:
					blockcount++;
					break;
					// 2 Value
				    case BOAT:
				    case CHEST:
				    case CLAY:
				    case DIRT:
				    case ENDER_STONE:
				    case GLASS:
				    case HARD_CLAY:
				    case HAY_BLOCK:
				    case ITEM_FRAME:
				    case MOSSY_COBBLESTONE:
				    case NETHER_BRICK:
				    case NETHER_BRICK_STAIRS:
				    case NETHER_FENCE:
				    case PAINTING:
				    case PISTON_BASE:
				    case PISTON_STICKY_BASE:
				    case SMOOTH_BRICK:
				    case SMOOTH_STAIRS:
				    case SOIL:
				    case SOUL_SAND:
				    case STAINED_CLAY:
				    case STAINED_GLASS:
				    case STONE_PLATE:
					blockcount += 2;
					// 5 Value
				    case BOOKSHELF:
				    case BRICK_STAIRS:
				    case BRICK:
				    case DIODE:
				    case DIODE_BLOCK_OFF:
				    case DIODE_BLOCK_ON:
				    case DISPENSER:
				    case DROPPER:
				    case FLOWER_POT:
				    case GRASS:
				    case ICE:
				    case IRON_DOOR_BLOCK:
				    case IRON_FENCE:
				    case IRON_PLATE:
				    case MYCEL:
				    case PACKED_ICE:
				    case TNT:
				    case TRAP_DOOR:
					blockcount += 5;
					break;
					// 10 value
				    case BURNING_FURNACE:
				    case ACTIVATOR_RAIL:
				    case ANVIL:
				    case CAULDRON:
				    case DAYLIGHT_DETECTOR:
				    case DETECTOR_RAIL:
				    case EXPLOSIVE_MINECART:
				    case FURNACE:
				    case HOPPER:
				    case IRON_BLOCK:
				    case JUKEBOX:
				    case LAPIS_BLOCK:
				    case MINECART:
				    case NOTE_BLOCK:
				    case OBSIDIAN:
				    case POWERED_MINECART:
				    case POWERED_RAIL:
				    case REDSTONE_BLOCK:
				    case SPONGE:
				    case STORAGE_MINECART:
				    case TRAPPED_CHEST:
					blockcount += 10;
					break;
					// 20 value
				    case BREWING_STAND:
				    case HOPPER_MINECART:
					blockcount += 20;
					break;
					// 100 value
				    case BEACON:
					blockcount += 100;
					break;
					// 150 value
				    case DRAGON_EGG:
				    case EMERALD_BLOCK:
				    case ENDER_CHEST:
				    case GOLD_BLOCK:
				    case ENCHANTMENT_TABLE:
					blockcount += 150;
					break;
					// 300 value
				    case DIAMOND_BLOCK:
					blockcount += 300;
					break;
				    default:
					break;
				    }
				}
			    }
			}
		    }

		    if (player.getUniqueId().equals(islandPlayer)) {
			players.setIslandLevel(islandPlayer, blockcount / 100);
			players.save(islandPlayer);
			plugin.updateTopTen();
			// Tell offline team members the island level increased.
			if (players.getIslandLevel(islandPlayer) > oldLevel) {
			    plugin.tellOfflineTeam(islandPlayer, ChatColor.GREEN + Locale.islandislandLevelis + " " + ChatColor.WHITE + players.getIslandLevel(islandPlayer));
			}
		    }
		} catch (final Exception e) {
		    plugin.getLogger().info("Error while calculating Island Level: " + e);
		    busyFlag = true;
		}

		plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
		    public void run() {
			busyFlag = true;
			if (player.isOnline()) {
			    if (player.getUniqueId().equals(islandPlayer)) {
				player.sendMessage(
					ChatColor.GREEN + Locale.islandislandLevelis + " " + ChatColor.WHITE + players.getIslandLevel(islandPlayer));
			    } else {
				if (players.isAKnownPlayer(islandPlayer)) {
				    player.sendMessage(ChatColor.GREEN + Locale.islandislandLevelis + " " + ChatColor.WHITE + players.getIslandLevel(islandPlayer));
				} else {
				    player.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
				}
			    }
			}
		    }
		});
	    }
	});
	return true;
    }

    /**
     * One-to-one relationship, you can return the first matched key
     * @param map
     * @param value
     * @return
     */
    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
	for (Entry<T, E> entry : map.entrySet()) {
	    if (value.equals(entry.getValue())) {
		return entry.getKey();
	    }
	}
	return null;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
	if (!(sender instanceof Player)) {
	    return false;
	}
	final Player player = (Player) sender;
	// Basic permissions check to even use /island
	if (!VaultHelper.checkPerm(player, "acidisland.island.create")) {
	    player.sendMessage(ChatColor.RED + Locale.islanderrorYouDoNotHavePermission);
	    return true;
	}
	/*
	 * Grab data for this player - may be null or empty
	 * playerUUID is the unique ID of the player who issued the command
	 */
	final UUID playerUUID = player.getUniqueId();
	final UUID teamLeader = players.getTeamLeader(playerUUID);
	final List<UUID> teamMembers = players.getMembers(playerUUID);
	// The target player's UUID
	UUID targetPlayer = null;
	// Check if a player has an island or is in a team
	switch (split.length) {
	// /island command by itself
	case 0:
	    // New island
	    if (players.getIslandLocation(playerUUID) == null && !players.inTeam(playerUUID)) {
		// Create new island for player
		player.sendMessage(ChatColor.GREEN + Locale.islandnew);
		final Location cowSpot = newIsland(sender);
		plugin.homeTeleport(player);
		Bukkit.getScheduler().runTaskLater(plugin, new Runnable () {
		    @Override
		    public void run() {
			player.getWorld().spawnEntity(cowSpot, EntityType.COW);

		    }
		}, 40L);		    
		setResetWaitTime(player);
		return true;
	    } else {
		// Teleport home
		// Check if player is in a boat
		if (player.isInsideVehicle()) {
		    Entity boat = player.getVehicle();
		    if (boat instanceof Boat) {
			// Remove the boat so they don't lie around everywhere
			plugin.homeTeleport(player);
			boat.remove();
			player.getInventory().addItem(new ItemStack(Material.BOAT, 1));
			player.updateInventory();
		    }
		} else {
		    plugin.homeTeleport(player);
		}
		return true;
	    }
	case 1:
	    if (split[0].equalsIgnoreCase("minishop") || split[0].equalsIgnoreCase("ms")) {
		if (player.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
		    if (VaultHelper.checkPerm(player, "acidisland.island.minishop")) {
			player.openInventory(ControlPanel.miniShop);
			return true;
		    }
		}
	    }
	    // /island <command>
	    if (split[0].equalsIgnoreCase("warp")) {
		if (VaultHelper.checkPerm(player, "acidisland.island.warp")) {
		    player.sendMessage(ChatColor.YELLOW + "/island warp <player>: " + ChatColor.WHITE + Locale.islandhelpWarp);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("warps")) {
		if (VaultHelper.checkPerm(player, "acidisland.island.warp")) {
		    // Step through warp table
		    Set<UUID> warpList = plugin.listWarps();
		    if (warpList.isEmpty()) {
			player.sendMessage(ChatColor.YELLOW + Locale.warpserrorNoWarpsYet);
			if (VaultHelper.checkPerm(player, "acidisland.island.addwarp")) {
			    player.sendMessage(ChatColor.YELLOW + Locale.warpswarpTip);
			}
			return true;
		    } else {
			Boolean hasWarp = false;
			String wlist = "";
			for (UUID w : warpList) {
			    if (wlist.isEmpty()) {
				wlist = players.getName(w);
			    } else {
				wlist += ", " + players.getName(w);
			    }
			    if (w.equals(playerUUID)) {
				hasWarp = true;
			    }
			}
			player.sendMessage(ChatColor.YELLOW + Locale.warpswarpsAvailable + ": " + ChatColor.WHITE + wlist);
			if (!hasWarp && (VaultHelper.checkPerm(player, "acidisland.island.addwarp"))) {
			    player.sendMessage(ChatColor.YELLOW + Locale.warpswarpTip);
			}
			return true;
		    }
		}
	    } else if (split[0].equalsIgnoreCase("restart") || split[0].equalsIgnoreCase("reset")) {
		if (players.inTeam(playerUUID)) {
		    if (!players.getTeamLeader(playerUUID).equals(playerUUID)) {
			player.sendMessage(ChatColor.RED
				+ Locale.islandresetOnlyOwner);
		    } else {
			player.sendMessage(ChatColor.YELLOW
				+ Locale.islandresetMustRemovePlayers);
		    }
		    return true;
		}
		if (!onRestartWaitTime(player) || Settings.resetWait == 0 || player.isOp()) {
		    // Kick off the confirmation
		    player.sendMessage(ChatColor.RED + Locale.islandresetConfirm);
		    if (!confirm.containsKey(playerUUID) || !confirm.get(playerUUID)) {
			confirm.put(playerUUID, true);
			Bukkit.getScheduler().runTaskLater(plugin, new Runnable () {
			    @Override
			    public void run() {
				confirm.put(playerUUID,false);
			    }
			}, 200L);	
		    }
		    return true;
		} else {
		    player.sendMessage(ChatColor.YELLOW + Locale.islandresetWait.replace("[time]",String.valueOf(getResetWaitTime(player))));
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("confirm")) {
		if (confirm.containsKey(playerUUID) && confirm.get(playerUUID)) {
		    // Actually RESET the island
		    player.sendMessage(ChatColor.YELLOW + Locale.islandresetPleaseWait);
		    //plugin.getLogger().info("DEBUG Reset command issued!");
		    final Location oldIsland = plugin.players.getIslandLocation(playerUUID);
		    plugin.unregisterEvents();		
		    final Location cowSpot = newIsland(sender);
		    players.setHomeLocation(player.getUniqueId(), null);
		    plugin.homeTeleport(player);
		    Bukkit.getScheduler().runTaskLater(plugin, new Runnable () {
			@Override
			public void run() {
			    player.getWorld().spawnEntity(cowSpot, EntityType.COW);

			}
		    }, 40L);		    
		    //player.getWorld().spawnEntity(cowSpot, EntityType.COW);
		    setResetWaitTime(player);
		    plugin.removeWarp(playerUUID);
		    plugin.removeIsland(oldIsland);
		    DeleteIsland deleteIsland = new DeleteIsland(plugin,oldIsland);
		    deleteIsland.runTaskTimer(plugin, 40L, 40L);
		    plugin.restartEvents();
		} else {
		    player.sendMessage(ChatColor.YELLOW + "/island restart: " + ChatColor.WHITE + Locale.islandhelpRestart);
		}
	    } else if (split[0].equalsIgnoreCase("sethome")) {
		if (VaultHelper.checkPerm(player, "acidisland.island.sethome")) {
		    plugin.homeSet(player);
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("help")) { 
		player.sendMessage(ChatColor.GREEN + "AcidIsland " + plugin.getDescription().getVersion() + " help:");

		player.sendMessage(ChatColor.YELLOW + "/island: " + ChatColor.WHITE + Locale.islandhelpIsland);
		player.sendMessage(ChatColor.YELLOW + "/island restart: " + ChatColor.WHITE + Locale.islandhelpRestart);
		player.sendMessage(ChatColor.YELLOW + "/island sethome: " + ChatColor.WHITE + Locale.islandhelpSetHome);
		player.sendMessage(ChatColor.YELLOW + "/island level: " + ChatColor.WHITE + Locale.islandhelpLevel);
		player.sendMessage(ChatColor.YELLOW + "/island level <player>: " + ChatColor.WHITE + Locale.islandhelpLevelPlayer);
		player.sendMessage(ChatColor.YELLOW + "/island top: " + ChatColor.WHITE + Locale.islandhelpTop);
		if (VaultHelper.checkPerm(player, "acidisland.island.minishop")) {
		    player.sendMessage(ChatColor.YELLOW + "/island minishop or ms: " + ChatColor.WHITE + Locale.islandhelpMiniShop);		    
		}
		if (VaultHelper.checkPerm(player, "acidisland.island.warp")) {
		    player.sendMessage(ChatColor.YELLOW + "/island warps: " + ChatColor.WHITE + Locale.islandhelpWarps);
		    player.sendMessage(ChatColor.YELLOW + "/island warp <player>: " + ChatColor.WHITE + Locale.islandhelpWarp);
		}
		if (VaultHelper.checkPerm(player, "acidisland.team.create")) {
		    player.sendMessage(ChatColor.YELLOW + "/island team: " + ChatColor.WHITE + Locale.islandhelpTeam);
		    player.sendMessage(ChatColor.YELLOW + "/island invite <player>: " + ChatColor.WHITE + Locale.islandhelpInvite);
		    player.sendMessage(ChatColor.YELLOW + "/island leave: " + ChatColor.WHITE + Locale.islandhelpLeave);
		}
		if (VaultHelper.checkPerm(player, "acidisland.team.kick")) {
		    player.sendMessage(ChatColor.YELLOW + "/island kick <player>: " + ChatColor.WHITE + Locale.islandhelpKick);
		}
		if (VaultHelper.checkPerm(player, "acidisland.team.join")) {
		    player.sendMessage(ChatColor.YELLOW + "/island <accept/reject>: " + ChatColor.WHITE + Locale.islandhelpAcceptReject);
		}
		if (VaultHelper.checkPerm(player, "acidisland.team.makeleader")) {
		    player.sendMessage(ChatColor.YELLOW + "/island makeleader <player>: " + ChatColor.WHITE + Locale.islandhelpMakeLeader);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("top")) {
		if (VaultHelper.checkPerm(player, "acidisland.island.topten")) {
		    plugin.showTopTen(player);
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("level")) {
		if (plugin.playerIsOnIsland(player)) {
		    if (!players.inTeam(playerUUID) && !players.hasIsland(playerUUID)) {
			player.sendMessage(ChatColor.RED + Locale.errorNoIsland);
		    } else {
			calculateIslandLevel(player, playerUUID);
		    }
		    return true;
		}
		player.sendMessage(ChatColor.RED + Locale.challengeserrorNotOnIsland);
		return true;
	    } else if (split[0].equalsIgnoreCase("invite")) {
		// Invite command with no name, i.e., /island invite - tells the player how many more people they can invite
		if (VaultHelper.checkPerm(player, "acidisland.team.create")) {
		    player.sendMessage(ChatColor.YELLOW + "Use" + ChatColor.WHITE + " /island invite <playername> " + ChatColor.YELLOW
			    + Locale.islandhelpInvite);
		    // If the player who is doing the inviting has a team
		    if (players.inTeam(playerUUID)) {
			// Check to see if the player is the leader
			if (teamLeader.equals(playerUUID)) {
			    // Check to see if the team is already full
			    if (teamMembers.size() < Settings.maxTeamSize) {
				player.sendMessage(ChatColor.GREEN + Locale.inviteyouCanInvite.replace("[number]", String.valueOf(Settings.maxTeamSize - teamMembers.size())));
			    } else {
				player.sendMessage(ChatColor.RED + Locale.inviteerrorYourIslandIsFull);
			    }
			    return true;
			}

			player.sendMessage(ChatColor.RED + Locale.inviteerrorYouMustHaveIslandToInvite);
			return true;
		    }

		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("accept")) {
		// Accept an invite command
		if (VaultHelper.checkPerm(player, "acidisland.team.join")) {
		    // If player is not in a team but has been invited to join one
		    if (!players.inTeam(playerUUID) && inviteList.containsKey(playerUUID)) {
			// If the invitee has an island of their own
			if (players.hasIsland(playerUUID)) {
			    plugin.getLogger().info(player.getName() + "'s island will be deleted because they joined a party.");
			    // Delete the island next tick
			    Bukkit.getScheduler().runTask(plugin, new Runnable() {
				@Override
				public void run() {
				    plugin.deletePlayerIsland(playerUUID);
				    plugin.getLogger().info("Island deleted.");
				}
			    });
			}
			// Add the player to the team
			addPlayertoTeam(playerUUID, inviteList.get(playerUUID));
			// If the leader who did the invite does not yet have a team (leader is not in a team yet)
			if (!players.inTeam(inviteList.get(playerUUID))) {
			    // Add the leader to their own team
			    addPlayertoTeam(inviteList.get(playerUUID), inviteList.get(playerUUID));
			} 
			setResetWaitTime(player);

			plugin.homeTeleport(player);
			resetPlayer(player);
			player.sendMessage(ChatColor.GREEN + Locale.inviteyouHaveJoinedAnIsland);
			if (Bukkit.getPlayer(inviteList.get(playerUUID)) != null) {
			    Bukkit.getPlayer(inviteList.get(playerUUID)).sendMessage(ChatColor.GREEN + Locale.invitehasJoinedYourIsland.replace("[name]", player.getName()));
			}
			// Remove the invite
			inviteList.remove(player.getUniqueId());
			return true;
		    }
		    player.sendMessage(ChatColor.RED + Locale.errorCommandNotReady);
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("reject")) {
		// Reject /island reject
		if (inviteList.containsKey(player.getUniqueId())) {
		    player.sendMessage(ChatColor.YELLOW + Locale.rejectyouHaveRejectedInvitation);
		    // If the player is online still then tell them directly about the rejection
		    if (Bukkit.getPlayer(inviteList.get(player.getUniqueId())) != null) {
			Bukkit.getPlayer(inviteList.get(player.getUniqueId())).sendMessage(
				ChatColor.RED + Locale.rejectnameHasRejectedInvite.replace("[name]", player.getName()));
		    }
		    // Remove this player from the global invite list
		    inviteList.remove(player.getUniqueId());
		} else {
		    // Someone typed /island reject and had not been invited
		    player.sendMessage(ChatColor.RED + Locale.rejectyouHaveNotBeenInvited);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("leave")) {
		// Leave team command
		if (VaultHelper.checkPerm(player, "acidisland.team.join")) {
		    if (player.getWorld().getName().equalsIgnoreCase(AcidIsland.getIslandWorld().getName())) {
			if (players.inTeam(playerUUID)) {
			    if (players.getTeamLeader(playerUUID).equals(playerUUID)) {
				player.sendMessage(ChatColor.YELLOW + Locale.leaveerrorYouAreTheLeader);
				return true;
			    }
			    resetPlayer(player);
			    if (!player.performCommand("spawn")) {
				player.teleport(player.getWorld().getSpawnLocation());
			    }
			    removePlayerFromTeam(playerUUID, teamLeader);
			    // Remove any warps
			    plugin.removeWarp(playerUUID);
			    player.sendMessage(ChatColor.YELLOW + Locale.leaveyouHaveLeftTheIsland);
			    // Tell the leader if they are online
			    if (plugin.getServer().getPlayer(teamLeader) != null) {
				plugin.getServer().getPlayer(teamLeader).sendMessage(ChatColor.RED + Locale.leavenameHasLeftYourIsland.replace("[name]",player.getName()));
			    } else {
				// Leave them a message
				plugin.setMessage(teamLeader, ChatColor.RED + Locale.leavenameHasLeftYourIsland.replace("[name]",player.getName()));
			    }
			    // Check if the size of the team is now 1
			    //teamMembers.remove(playerUUID);
			    if (teamMembers.size() < 3) {
				plugin.getLogger().info("Party is less than 2 - removing leader from team");
				removePlayerFromTeam(teamLeader, teamLeader);
			    }
			    return true;
			} else {
			    player.sendMessage(ChatColor.RED + Locale.leaveerrorYouCannotLeaveIsland);
			    return true;
			}
		    } else {
			player.sendMessage(ChatColor.RED + Locale.leaveerrorYouMustBeInWorld);
		    }
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("team")) {
		if (players.inTeam(playerUUID)) {
		    if (teamLeader.equals(playerUUID)) {
			if (teamMembers.size() < Settings.maxTeamSize) {
			    player.sendMessage(ChatColor.GREEN + Locale.inviteyouCanInvite.replace("[number]", String.valueOf(Settings.maxTeamSize - teamMembers.size())));
			} else {
			    player.sendMessage(ChatColor.RED + Locale.inviteerrorYourIslandIsFull);
			}
		    }

		    player.sendMessage(ChatColor.YELLOW + Locale.teamlistingMembers + ":");
		    // Display members in the list
		    for (UUID m : players.getMembers(teamLeader)) {
			player.sendMessage(ChatColor.WHITE + players.getName(m));
		    }
		} else if (inviteList.containsKey(playerUUID)) {
		    // TODO: Worried about this next line...
		    player.sendMessage(ChatColor.YELLOW + Locale.invitenameHasInvitedYou.replace("[name]", players.getName(inviteList.get(playerUUID))));
		    player.sendMessage(ChatColor.WHITE + "/island [accept/reject]" + ChatColor.YELLOW + Locale.invitetoAcceptOrReject);
		} else {
		    player.sendMessage(ChatColor.RED + Locale.kickerrorNoTeam);
		}
		return true;
	    } else {
		// Incorrect syntax
		return false;
	    }

	case 2:
	    if (split[0].equalsIgnoreCase("warp")) {
		// Warp somewhere command
		if (VaultHelper.checkPerm(player, "acidisland.island.warp")) {
		    final Set<UUID> warpList = plugin.listWarps();
		    if (warpList.isEmpty()) {
			player.sendMessage(ChatColor.YELLOW + Locale.warpserrorNoWarpsYet);
			if (VaultHelper.checkPerm(player, "acidisland.island.addwarp")) {
			    player.sendMessage(ChatColor.YELLOW + Locale.warpswarpTip);
			}
			return true;
		    } else {
			// Check if this is part of a name
			UUID foundWarp = null;
			for (UUID warp : warpList) {
			    if (players.getName(warp).toLowerCase().startsWith(split[1].toLowerCase())) {
				foundWarp = warp;
				break;
			    }
			}
			if (foundWarp == null) {
			    player.sendMessage(ChatColor.RED + Locale.warpserrorDoesNotExist);
			    return true;
			} else {
			    // Warp exists!
			    final Location warpSpot = plugin.getWarp(foundWarp);
			    // Check if the warp spot is safe
			    if (warpSpot == null) {
				player.sendMessage(ChatColor.RED + Locale.warpserrorNotReadyYet);
				plugin.getLogger().warning("Null warp found, owned by " + players.getName(foundWarp));
				return true;
			    }
			    if (!(AcidIsland.isSafeLocation(warpSpot))) {
				player.sendMessage(ChatColor.RED + Locale.warpserrorNotSafe);
				plugin.getLogger().warning("Unsafe warp found at " + warpSpot.toString() + " owned by " + players.getName(foundWarp));
				return true;
			    } else {
				final Location actualWarp = new Location(warpSpot.getWorld(), warpSpot.getBlockX() + 0.5D, warpSpot.getBlockY(),
					warpSpot.getBlockZ() + 0.5D);
				player.teleport(actualWarp);
				player.getWorld().playSound(player.getLocation(), Sound.BAT_TAKEOFF, 1F, 1F);
				return true;
			    }
			}
		    }
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("level")) {
		// island level command
		if (VaultHelper.checkPerm(player, "acidisland.island.info")) {
		    if (!players.inTeam(playerUUID) && !players.hasIsland(playerUUID)) {
			player.sendMessage(ChatColor.RED + Locale.errorNoIsland);
		    } else {
			// May return null if not known
			final UUID invitedPlayerUUID = players.getUUID(split[1]);
			// Invited player must be known
			if (invitedPlayerUUID == null) {
			    player.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
			    return true;
			}
			calculateIslandLevel(player, players.getUUID(split[1]));
		    }
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("invite")) {
		// Team invite a player command
		if (VaultHelper.checkPerm(player, "acidisland.team.create")) {
		    // May return null if not known
		    final UUID invitedPlayerUUID = players.getUUID(split[1]);
		    // Invited player must be known
		    if (invitedPlayerUUID == null) {
			player.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
			return true;
		    }
		    // Player must be online
		    // TODO: enable offline players to be invited
		    if (plugin.getServer().getPlayer(invitedPlayerUUID) == null) {
			player.sendMessage(ChatColor.RED + Locale.errorOfflinePlayer);
			return true;
		    }
		    // Player issuing the command must have an island
		    if (!players.hasIsland(player.getUniqueId())) {
			player.sendMessage(ChatColor.RED + Locale.inviteerrorYouMustHaveIslandToInvite);
			return true;
		    }
		    // Player cannot invite themselves
		    if (player.getName().equalsIgnoreCase(split[1])) {
			player.sendMessage(ChatColor.RED + Locale.inviteerrorYouCannotInviteYourself);
			return true;
		    }
		    // If the player already has a team then check that they are the leader, etc
		    if (players.inTeam(player.getUniqueId())) {
			// Leader?
			if (teamLeader.equals(player.getUniqueId())) {
			    // Invited player is free and not in a team
			    if (!players.inTeam(invitedPlayerUUID)) {
				// Player has space in their team
				if (teamMembers.size() < Settings.maxTeamSize) {
				    // If that player already has an invite out then retract it.
				    // Players can only have one invite out at a time - interesting
				    if (inviteList.containsValue(playerUUID)) {
					inviteList.remove(getKeyByValue(inviteList, player.getUniqueId()));
					player.sendMessage(ChatColor.YELLOW + Locale.inviteremovingInvite);
				    }
				    // Put the invited player (key) onto the list with inviter (value)
				    // If someone else has invited a player, then this invite will overwrite the previous invite!
				    inviteList.put(invitedPlayerUUID, player.getUniqueId());
				    player.sendMessage(ChatColor.GREEN + Locale.inviteinviteSentTo.replace("[name]", split[1]));
				    // Send message to online player
				    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(Locale.invitenameHasInvitedYou.replace("[name]", player.getName()));
				    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(
					    ChatColor.WHITE + "/island [accept/reject]" + ChatColor.YELLOW + " " + Locale.invitetoAcceptOrReject);
				    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(ChatColor.RED + Locale.invitewarningYouWillLoseIsland);
				} else {
				    player.sendMessage(ChatColor.RED + Locale.inviteerrorYourIslandIsFull);
				}
			    } else {
				player.sendMessage(ChatColor.RED + Locale.inviteerrorThatPlayerIsAlreadyInATeam);
			    }
			} else {
			    player.sendMessage(ChatColor.RED + Locale.inviteerrorYouMustHaveIslandToInvite);
			}
		    } else {
			// First-time invite player does not have a team
			// Check if invitee is in a team or not
			if (!players.inTeam(invitedPlayerUUID)) {
			    // If the inviter already has an invite out, remove it
			    if (inviteList.containsValue(playerUUID)) {
				inviteList.remove(getKeyByValue(inviteList, player.getUniqueId()));
				player.sendMessage(ChatColor.YELLOW + Locale.inviteremovingInvite);
			    }
			    // Place the player and invitee on the invite list
			    inviteList.put(invitedPlayerUUID, player.getUniqueId());

			    player.sendMessage(ChatColor.GREEN + Locale.inviteinviteSentTo.replace("[name]", split[1]));
			    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(Locale.invitenameHasInvitedYou.replace("[name]", player.getName()));
			    Bukkit.getPlayer(invitedPlayerUUID).sendMessage(
				    ChatColor.WHITE + "/island [accept/reject]" + ChatColor.YELLOW + " " + Locale.invitetoAcceptOrReject);
			    // Check if the player has an island and warn accordingly
			    if (players.hasIsland(invitedPlayerUUID)) {
				Bukkit.getPlayer(invitedPlayerUUID).sendMessage(ChatColor.RED + Locale.invitewarningYouWillLoseIsland);
			    }
			} else {
			    player.sendMessage(ChatColor.RED + Locale.inviteerrorThatPlayerIsAlreadyInATeam);
			}
		    }
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("kick") || split[0].equalsIgnoreCase("remove")) {
		// Island remove command with a player name, or island kick command
		if (VaultHelper.checkPerm(player, "acidisland.team.kick")) {
		    // The main thing to do is check if the player name to kick is in the list of players in the team.
		    targetPlayer = null;
		    for (UUID member : teamMembers) {
			if (players.getName(member).equalsIgnoreCase(split[1])) {
			    targetPlayer = member;
			}
		    }
		    if (targetPlayer == null) {
			player.sendMessage(ChatColor.RED + Locale.kickerrorNotPartOfTeam);
			return true;
		    }
		    if (players.inTeam(playerUUID)) {		
			// If this player is the leader
			if (teamLeader.equals(playerUUID)) {
			    if (teamMembers.contains(targetPlayer)) {
				// If the player leader tries to kick or remove themselves
				if (player.getUniqueId().equals(targetPlayer)) {
				    player.sendMessage(ChatColor.RED + Locale.leaveerrorLeadersCannotLeave);
				    return true;
				}
				// If target is online
				Player target = plugin.getServer().getPlayer(targetPlayer);
				if (target != null) {
				    target.sendMessage(ChatColor.RED + Locale.kicknameRemovedYou.replace("[name]", player.getName()));

				    // Clear the player out and throw their stuff at the leader
				    if (target.getWorld().getName().equalsIgnoreCase(AcidIsland.getIslandWorld().getName())) {
					for (ItemStack i : target.getInventory().getContents()) {
					    if (i != null) {
						try {
						    player.getWorld().dropItemNaturally(player.getLocation(), i);
						    target.getInventory().remove(i);
						} catch (Exception e) {}
					    }
					}
					for (ItemStack i : target.getEquipment().getArmorContents()) {
					    if (i != null) {
						try {
						    player.getWorld().dropItemNaturally(player.getLocation(), i);
						} catch (Exception e) {}
					    }
					}
					resetPlayer(target);
				    }
				    if (!target.performCommand("spawn")) {
					target.teleport(AcidIsland.getIslandWorld().getSpawnLocation());
				    } 
				} else {
				    // Offline
				    // Tell offline player they were kicked
				    plugin.setMessage(targetPlayer, ChatColor.RED + Locale.kicknameRemovedYou.replace("[name]", player.getName()));
				}
				// Remove any warps
				plugin.removeWarp(target.getUniqueId());
				// Tell leader they removed the player
				sender.sendMessage(ChatColor.RED + Locale.kicknameRemoved.replace("[name]", split[1]));
				removePlayerFromTeam(targetPlayer, teamLeader);
				teamMembers.remove(targetPlayer);
				if (teamMembers.size() < 2) {
				    removePlayerFromTeam(player.getUniqueId(), teamLeader);
				}				
			    } else {
				plugin.getLogger().warning("Player " + player.getName() + " failed to remove " + players.getName(targetPlayer));
				player.sendMessage(ChatColor.RED + Locale.kickerrorNotPartOfTeam);
			    }
			} else {
			    player.sendMessage(ChatColor.RED + Locale.kickerrorOnlyLeaderCan);
			}
		    } else {
			player.sendMessage(ChatColor.RED + Locale.kickerrorNoTeam);
		    }
		    return true;
		}
		return false;
	    } else if (split[0].equalsIgnoreCase("makeleader")) {
		if (VaultHelper.checkPerm(player, "acidisland.team.makeleader")) {
		    targetPlayer = players.getUUID(split[1]);
		    if (targetPlayer == null) {
			player.sendMessage(ChatColor.RED + Locale.makeLeadererrorPlayerMustBeOnline);
			return true;
		    }
		    if (!players.inTeam(player.getUniqueId())) {
			player.sendMessage(ChatColor.RED + Locale.makeLeadererrorYouMustBeInTeam);
			return true;
		    }

		    if (players.getMembers(player.getUniqueId()).size() > 2) {
			player.sendMessage(ChatColor.RED + Locale.makeLeadererrorRemoveAllPlayersFirst);
			plugin.getLogger().info(player.getName() + " tried to transfer his island, but failed because >2 people in a team");
			return true;
		    }

		    if (players.inTeam(player.getUniqueId())) {
			if (teamLeader.equals(player.getUniqueId())) {
			    if (teamMembers.contains(targetPlayer)) {
				if (Bukkit.getPlayer(targetPlayer) != null) {
				    Bukkit.getPlayer(targetPlayer).sendMessage(ChatColor.GREEN + Locale.makeLeaderyouAreNowTheOwner);
				}
				player.sendMessage(ChatColor.GREEN + Locale.makeLeadernameIsNowTheOwner.replace("[name]", Bukkit.getPlayer(targetPlayer).getName()));
				removePlayerFromTeam(targetPlayer, teamLeader);
				removePlayerFromTeam(player.getUniqueId(), teamLeader);
				addPlayertoTeam(player.getUniqueId(), targetPlayer);
				addPlayertoTeam(targetPlayer, targetPlayer);
				plugin.transferIsland(player.getUniqueId(), targetPlayer);
				return true;
			    }
			    player.sendMessage(ChatColor.RED + Locale.makeLeadererrorThatPlayerIsNotInTeam);
			} else {
			    player.sendMessage(ChatColor.RED + Locale.makeLeadererrorNotYourIsland);
			}
		    } else {
			player.sendMessage(ChatColor.RED + Locale.makeLeadererrorGeneralError);
		    }
		    return true;
		}
	    } else {
		return false;
	    }
	}
	return false;
    }

    /**
     * Set time out for island restarting
     * @param player
     * @return
     */
    public boolean onRestartWaitTime(final Player player) {
	if (resetWaitTime.containsKey(player.getUniqueId())) {
	    if (resetWaitTime.get(player.getUniqueId()).longValue() > Calendar.getInstance().getTimeInMillis()) {
		return true;
	    }

	    return false;
	}

	return false;
    }
    /**
     * Sets a timeout for player into the Hashmap resetWaitTime
     * 
     * @param player
     */
    public void setResetWaitTime(final Player player) {
	resetWaitTime.put(player.getUniqueId(), Long.valueOf(Calendar.getInstance().getTimeInMillis() + Settings.resetWait * 1000));
    }

    /**
     * Returns how long the player must wait until they can restart their island in seconds
     * 
     * @param player
     * @return
     */
    public long getResetWaitTime(final Player player) {
	if (resetWaitTime.containsKey(player.getUniqueId())) {
	    if (resetWaitTime.get(player.getUniqueId()).longValue() > Calendar.getInstance().getTimeInMillis()) {
		return (resetWaitTime.get(player.getUniqueId()).longValue() - Calendar.getInstance().getTimeInMillis())/1000;
	    }

	    return 0L;
	}

	return 0L;
    }



}
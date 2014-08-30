package com.wasteofplastic.acidisland;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.v1_7_R3.World;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * This class handles the /acid command for admins
 * 
 */
public class AdminCmd implements CommandExecutor {
    private AcidIsland plugin;
    private List<UUID> removeList = new ArrayList<UUID>();
    private PlayerCache players; 
    private boolean purgeFlag = false;
    private boolean confirmReq = false;
    private boolean confirmOK = false;
    private int confirmTimer = 0;

    public AdminCmd(AcidIsland acidIsland, PlayerCache players) {
	this.plugin = acidIsland;
	this.players = players;
    }

    private void help(CommandSender sender) {
	if (!(sender instanceof Player)) {
	    sender.sendMessage(ChatColor.YELLOW + "/acid reload:" + ChatColor.WHITE + " " + Locale.adminHelpreload);
	    sender.sendMessage(ChatColor.YELLOW + "/acid topten:" + ChatColor.WHITE + " " + Locale.adminHelptopTen);
	    sender.sendMessage(ChatColor.YELLOW + "/acid delete <player>:" + ChatColor.WHITE + " " + Locale.adminHelpdelete);
	    sender.sendMessage(ChatColor.YELLOW + "/acid completechallenge <challengename> <player>:" + ChatColor.WHITE
		    + " " + Locale.adminHelpcompleteChallenge);
	    sender.sendMessage(ChatColor.YELLOW + "/acid resetchallenge <challengename> <player>:" + ChatColor.WHITE
		    + " " + Locale.adminHelpresetChallenge);
	    sender.sendMessage(ChatColor.YELLOW + "/acid resetallchallenges <player>:" + ChatColor.WHITE + " " + Locale.adminHelpresetAllChallenges);
	    sender.sendMessage(ChatColor.YELLOW + "/acid purge [TimeInDays]:" + ChatColor.WHITE + " " + Locale.adminHelppurge);
	    sender.sendMessage(ChatColor.YELLOW + "/acid info <player>:" + ChatColor.WHITE + " " + Locale.adminHelpinfo);
	    sender.sendMessage(ChatColor.YELLOW + "/acid info:" + ChatColor.WHITE + " " + Locale.adminHelpinfoIsland);
	} else {
	    // Only give help if the player has permissions
	    // Permissions are split into admin permissions and mod permissions
	    Player player = (Player)sender;
	    player.sendMessage(Locale.adminHelpHelp);
	    if (VaultHelper.checkPerm(player, "acidisland.admin.reload") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid reload:" + ChatColor.WHITE + " " + Locale.adminHelpreload);
	    }
	    if (VaultHelper.checkPerm(player, "acidisland.admin.register") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid register <player>:" + ChatColor.WHITE + " " + Locale.adminHelpregister);
	    }
	    if (VaultHelper.checkPerm(player, "acidisland.admin.delete") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid delete <player>:" + ChatColor.WHITE + " " + Locale.adminHelpdelete);
	    }
	    if (VaultHelper.checkPerm(player, "acidisland.admin.purge") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid purge [TimeInDays]:" + ChatColor.WHITE + " " + Locale.adminHelppurge);
	    }
	    if (VaultHelper.checkPerm(player, "acidisland.mod.topten") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid topten:" + ChatColor.WHITE + " " + Locale.adminHelptopTen);
	    }
	    if (VaultHelper.checkPerm(player, "acidisland.mod.challenges") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid completechallenge <challengename> <player>:" + ChatColor.WHITE
			+ " " + Locale.adminHelpcompleteChallenge);
	    }
	    if (VaultHelper.checkPerm(player, "acidisland.mod.challenges") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid resetchallenge <challengename> <player>:" + ChatColor.WHITE
			+ " " + Locale.adminHelpresetChallenge);
	    }
	    if (VaultHelper.checkPerm(player, "acidisland.mod.challenges") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid resetallchallenges <player>:" + ChatColor.WHITE + " " + Locale.adminHelpresetAllChallenges);
	    }
	    if (VaultHelper.checkPerm(player, "acidisland.mod.info") || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid info <player>:" + ChatColor.WHITE + " " + Locale.adminHelpinfo);
	    }

	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
	// Console commands
	Player player;
	if (sender instanceof Player) {
	    player = (Player)sender;
	    if (split.length > 0) {
		// Admin : reload, register, delete and purge
		if (split[0].equalsIgnoreCase("reload") || split[0].equalsIgnoreCase("register")
			|| split[0].equalsIgnoreCase("delete") || split[0].equalsIgnoreCase("purge")
			|| split[0].equalsIgnoreCase("confirm")) {
		    if (!checkAdminPerms(player, split)) {
			player.sendMessage(ChatColor.RED + Locale.errorNoPermission);
			return true;
		    }
		} else {
		    // Mod commands
		    if (!checkModPerms(player, split)) {
			player.sendMessage(ChatColor.RED + Locale.errorNoPermission);
			return true;
		    }		    
		}
	    }
	}
	// Check for zero parameters e.g., /acid
	switch (split.length) {
	case 0:
	    help(sender);
	    return true;
	case 1:
	    if (split[0].equalsIgnoreCase("info")) {
		// Find the closest island
		if (!(sender instanceof Player)) {
		    sender.sendMessage(ChatColor.RED + "This command must be used in-game.");
		    return true;
		}
		Player p = (Player)sender;
		Location closestBedRock = null;
		double distance = 0;
		for (int x = -Settings.islandDistance; x< Settings.islandDistance; x++) {
		    for (int z = -Settings.islandDistance; z< Settings.islandDistance; z++) {
			
			Location blockLoc = new Location(p.getWorld(),x + p.getLocation().getBlockX(),Settings.island_level,z + p.getLocation().getBlockZ());
			if (blockLoc.getBlock().getType().equals(Material.BEDROCK)) {
			    if (closestBedRock == null) {
				closestBedRock = blockLoc.clone();
				distance = closestBedRock.distanceSquared(p.getLocation());
			    } else {
				// Find out if this is closer to the player
				
				double newDist = blockLoc.distanceSquared(p.getLocation());
				if (distance > newDist) {
				    closestBedRock = blockLoc.clone();
				    distance = newDist;
				    }
			    }
			}
		    }
		}
		if (closestBedRock == null) {
		    sender.sendMessage(ChatColor.RED + "Sorry, could not find an island. Move closer?");
		    return true;
		}
		// Find out whose island this is
		UUID target = plugin.players.getPlayerFromIslandLocation(closestBedRock);
		if (target == null) {
		    sender.sendMessage(ChatColor.RED + "This island is not owned by anyone right now.");
		    return true;
		}
		showInfo(target, sender);
		return true;
	    } else if (split[0].equalsIgnoreCase("reload")) {
		plugin.reloadConfig();
		plugin.loadPluginConfig();
		plugin.reloadChallengeConfig();
		ControlPanel.loadShop();
		sender.sendMessage(ChatColor.YELLOW + Locale.reloadconfigReloaded);
		return true;
	    } else if (split[0].equalsIgnoreCase("topten")) {
		sender.sendMessage(ChatColor.YELLOW + Locale.adminTopTengenerating);
		plugin.updateTopTen();
		sender.sendMessage(ChatColor.YELLOW + Locale.adminTopTenfinished);
		return true;
	    } else if (split[0].equalsIgnoreCase("purge")) {
		if (purgeFlag) {
		    sender.sendMessage(ChatColor.RED + Locale.purgealreadyRunning);
		    return true;
		}
		sender.sendMessage(ChatColor.YELLOW + Locale.purgeusage);
		return true;
	    } else if (split[0].equalsIgnoreCase("confirm")) { 
		if (!confirmReq) {
		    sender.sendMessage(ChatColor.RED + Locale.confirmerrorTimeLimitExpired);
		    return true;
		} else {
		    // Tell purge routine to go
		    confirmOK = true;
		    confirmReq = false;
		}
		return true;
	    } else {
		sender.sendMessage(ChatColor.RED + Locale.errorUnknownCommand);
		return false;
	    }
	case 2:
	    if (split[0].equalsIgnoreCase("purge")) {
		// PURGE Command
		// Purge runs in the background so if one is already running this flag stops a repeat
		if (purgeFlag) {
		    sender.sendMessage(ChatColor.RED + Locale.purgealreadyRunning);
		    return true;
		}
		// Set the flag
		purgeFlag = true;
		// Convert days to hours - no other limit checking?
		final int time = Integer.parseInt(split[1]) * 24;

		sender.sendMessage(ChatColor.YELLOW + Locale.purgecalculating.replace("[time]", split[1]));
		// Kick off task
		plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
		    public void run() {
			final File directoryPlayers = new File(plugin.getDataFolder() + File.separator + "players");

			long offlineTime = 0L;
			// Go through the player directory and build the purge list of filenames
			for (final File playerFile : directoryPlayers.listFiles()) {
			    if (playerFile.getName().endsWith(".yml")) {
				final UUID playerUUID = UUID.fromString(playerFile.getName().substring(0, playerFile.getName().length()-4));
				// Only bother if the layer is offline (by definition)
				if (Bukkit.getOfflinePlayer(playerUUID) != null && Bukkit.getPlayer(playerUUID) == null) {
				    final OfflinePlayer oplayer = Bukkit.getOfflinePlayer(playerUUID);
				    offlineTime = oplayer.getLastPlayed();
				    // Calculate the number of hours the player has
				    // been offline
				    offlineTime = (System.currentTimeMillis() - offlineTime) / 3600000L;
				    if (offlineTime > time) {
					if (players.hasIsland(playerUUID)) {
					    // If the player is in a team then ignore
					    if (!players.inTeam(playerUUID)) {
						if (players.getIslandLevel(playerUUID) < Settings.abandonedIslandLevel) {
						    //player.sendMessage("Island level for " + players.getName(playerUUID) + " is " + players.getIslandLevel(playerUUID));
						    removeList.add(playerUUID);
						}
					    }
					}
				    }
				}
			    }
			}
			if (removeList.isEmpty()) {
			    sender.sendMessage(ChatColor.YELLOW + Locale.purgenoneFound);
			    purgeFlag = false;
			    return;
			}
			sender.sendMessage(ChatColor.YELLOW + Locale.purgethisWillRemove.replace("[number]",String.valueOf(removeList.size())));
			sender.sendMessage(ChatColor.RED + Locale.purgewarning);
			sender.sendMessage(ChatColor.RED + Locale.purgetypeConfirm);
			confirmReq = true;
			confirmOK = false;
			confirmTimer = 0;
			new BukkitRunnable() {
			    @Override
			    public void run() {
				// This waits for 10 seconds and if no confirmation received, then it
				// cancels
				if (confirmTimer++ > 10) {
				    // Ten seconds is up!
				    confirmReq = false;
				    confirmOK = false;
				    purgeFlag = false;
				    removeList.clear();
				    sender.sendMessage(ChatColor.YELLOW + Locale.purgepurgeCancelled);
				    this.cancel();
				} else if (confirmOK) {
				    // Set up a repeating task to run every 5 seconds to remove
				    // islands one by one and then cancel when done
				    new BukkitRunnable() {
					@Override
					public void run() {
					    if (removeList.isEmpty() && purgeFlag) {
						purgeFlag = false;
						sender.sendMessage(ChatColor.YELLOW + Locale.purgefinished);
						this.cancel();
					    } 

					    if (removeList.size() > 0 && purgeFlag) {
						plugin.deletePlayerIsland(removeList.get(0));
						sender.sendMessage(ChatColor.YELLOW + Locale.purgeremovingName.replace("[name]", players.getName(removeList.get(0))));
						removeList.remove(0);
					    }

					}
				    }.runTaskTimer(plugin, 0L, 100L);
				    confirmReq = false;
				    confirmOK = false;
				    this.cancel();
				}
			    }
			}.runTaskTimer(plugin, 0L,20L);
		    }
		});
		return true;
	    } else if (split[0].equalsIgnoreCase("delete")) {
		// Convert name to a UUID
		final UUID playerUUID = players.getUUID(split[1]);
		if (!players.isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		} else {
		    if (players.getIslandLocation(playerUUID) != null) {
			sender.sendMessage(ChatColor.YELLOW + Locale.deleteremoving.replace("[name]", split[1]));
			plugin.deletePlayerIsland(playerUUID);
			// If they are online and in AcidIsland then delete their stuff too
			Player target = plugin.getServer().getPlayer(playerUUID);
			if (target != null) {
			    plugin.resetPlayer(target);
			}
			return true;
		    }
		    sender.sendMessage(Locale.errorNoIslandOther);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("register")) {
		if (sender instanceof Player) {
		    // Convert name to a UUID
		    final UUID playerUUID = players.getUUID(split[1]);
		    if (!players.isAKnownPlayer(playerUUID)) {
			sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
			return true;
		    } else {
			if (adminSetPlayerIsland(sender, ((Player) sender).getLocation(), playerUUID)) {
			    sender.sendMessage(ChatColor.GREEN + Locale.registersettingIsland.replace("[name]",split[1]));
			} else {
			    sender.sendMessage(ChatColor.RED + Locale.registererrorBedrockNotFound);
			}
			return true;
		    }
		} else {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownCommand);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("info")) {
		// Convert name to a UUID
		final UUID playerUUID = players.getUUID(split[1]);
		//plugin.getLogger().info("DEBUG: console player info UUID = " + playerUUID);
		if (!players.isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		} else {
		    showInfo(playerUUID, sender);
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("resetallchallenges")) {
		// Convert name to a UUID
		final UUID playerUUID = players.getUUID(split[1]);
		if (!players.isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		}
		players.resetAllChallenges(playerUUID);
		sender.sendMessage(ChatColor.YELLOW + Locale.resetChallengessuccess.replace("[name]", split[1]));
		return true;
	    }  else {
		return false;
	    }
	case 3:
	    if (split[0].equalsIgnoreCase("completechallenge")) {
		// Convert name to a UUID
		final UUID playerUUID = players.getUUID(split[2]);
		if (!players.isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		}
		if (players.checkChallenge(playerUUID,split[1].toLowerCase()) || !players.get(playerUUID).challengeExists(split[1].toLowerCase())) {
		    sender.sendMessage(ChatColor.RED + Locale.completeChallengeerrorChallengeDoesNotExist);
		    return true;
		}
		players.get(playerUUID).completeChallenge(split[1].toLowerCase());
		sender.sendMessage(ChatColor.YELLOW + Locale.completeChallengechallangeCompleted.replace("[challengename]", split[1].toLowerCase()).replace("[name]", split[2]));
		return true;
	    } else if (split[0].equalsIgnoreCase("resetchallenge")) {
		// Convert name to a UUID
		final UUID playerUUID = players.getUUID(split[2]);
		if (!players.isAKnownPlayer(playerUUID)) {
		    sender.sendMessage(ChatColor.RED + Locale.errorUnknownPlayer);
		    return true;
		}
		if (!players.checkChallenge(playerUUID,split[1].toLowerCase())
			|| !players.get(playerUUID).challengeExists(split[1].toLowerCase())) {
		    sender.sendMessage(ChatColor.RED + Locale.resetChallengeerrorChallengeDoesNotExist);
		    return true;
		}
		players.resetChallenge(playerUUID,split[1].toLowerCase());
		sender.sendMessage(ChatColor.YELLOW +  Locale.resetChallengechallengeReset.replace("[challengename]", split[1].toLowerCase()).replace("[name]",split[2]));
		return true;
	    } else {
		return false;
	    }
	default:
	    return false;
	}
    }

    private void showInfo(UUID playerUUID, CommandSender sender) {
	    sender.sendMessage(ChatColor.GREEN + players.getName(playerUUID));
	    sender.sendMessage(ChatColor.WHITE + "UUID: " + playerUUID.toString());
	    // Display island level
	    sender.sendMessage(ChatColor.GREEN + Locale.levelislandLevel + ": " + players.getIslandLevel(playerUUID));
	    // Last login
	    try {
		Date d = new Date(plugin.getServer().getOfflinePlayer(playerUUID).getLastPlayed());
		sender.sendMessage(ChatColor.GOLD + "Last login: " + d.toString());
	    } catch (Exception e) {}

	    // Completed challenges
	    sender.sendMessage(ChatColor.WHITE + "Challenges:");
	    HashMap<String,Boolean> challenges = players.getChallengeStatus(playerUUID);
	    for (String c: challenges.keySet()) {
		sender.sendMessage(c + ": " + ((challenges.get(c)) ? ChatColor.GREEN + Locale.challengescomplete :ChatColor.AQUA + Locale.challengesincomplete));
	    }
	    // Teams
	    if (players.inTeam(playerUUID)) {
		final UUID leader = players.getTeamLeader(playerUUID);
		final List<UUID> pList = players.getMembers(leader);
		sender.sendMessage(ChatColor.GREEN + players.getName(leader) );
		for (UUID member: pList) {
		    sender.sendMessage(ChatColor.WHITE + " - " + players.getName(member));
		}
		sender.sendMessage(ChatColor.YELLOW + Locale.adminInfoislandLocation + ":" + ChatColor.WHITE + " (" + players.getTeamIslandLocation(playerUUID).getBlockX() + ","
			+ players.getTeamIslandLocation(playerUUID).getBlockY() + "," + players.getTeamIslandLocation(playerUUID).getBlockZ() + ")");
	    } else {
		sender.sendMessage(ChatColor.YELLOW + Locale.errorNoTeam);
		if (players.hasIsland(playerUUID)) {
		    sender.sendMessage(ChatColor.YELLOW + Locale.adminInfoislandLocation + ":" + ChatColor.WHITE + " (" + players.getIslandLocation(playerUUID).getBlockX() + ","
			    + players.getIslandLocation(playerUUID).getBlockY() + "," + players.getIslandLocation(playerUUID).getBlockZ() + ")");
		}
		if (!(players.getTeamLeader(playerUUID) == null)) {
		    sender.sendMessage(ChatColor.RED + Locale.adminInfoerrorNullTeamLeader);
		}
		if (!players.getMembers(playerUUID).isEmpty()) {
		    sender.sendMessage(ChatColor.RED + Locale.adminInfoerrorTeamMembersExist);
		}
	    }
	
    }

    private boolean checkAdminPerms(Player player2, String[] split) {
	// Check perms quickly for this command
	if (player2.isOp()) {
	    return true;
	}
	String check = split[0];
	if (check.equalsIgnoreCase("confirm"))
	    check = "purge";
	if (VaultHelper.checkPerm(player2, "acidisland.admin." + split[0].toLowerCase())) {
	    return true;
	}
	return false;
    }
    private boolean checkModPerms(Player player2, String[] split) {
	// Check perms quickly for this command
	if (player2.isOp()) {
	    return true;
	}
	String check = split[0];
	// Check special cases
	if (check.contains("challenge".toLowerCase())) {
	    check = "challenges";
	}
	if (VaultHelper.checkPerm(player2, "acidisland.mod." + split[0].toLowerCase())) {
	    return true;
	}
	return false;
    }


    /**
     * Searches for bedrock around a location (20x20x20) and then assigns the
     * player to that island and applies a WorldGuard protection TODO: Does not
     * remove the player from any islands they already own. This should probably
     * be done because you can only have one island per player
     * 
     * @param sender
     *            - the player requesting the assignment
     * @param l
     *            - the location of sender
     * @param player
     *            - the assignee
     * @return - true if successful, false if not
     */
    public boolean adminSetPlayerIsland(final CommandSender sender, final Location l, final UUID player) {
	// If the player is not online
	final int px = l.getBlockX();
	final int py = l.getBlockY();
	final int pz = l.getBlockZ();
	for (int x = -10; x <= 10; x++) {
	    for (int y = -10; y <= 10; y++) {
		for (int z = -10; z <= 10; z++) {
		    final Block b = new Location(l.getWorld(), px + x, py + y, pz + z).getBlock();
		    if (b.getType().equals(Material.BEDROCK)) {
			players.setHomeLocation(player,new Location(l.getWorld(), px + x, py + y + 3, pz + z));
			players.setHasIsland(player,true);
			players.setIslandLocation(player, b.getLocation());
			return true;
		    }
		}
	    }
	}
	return false;
    }
}
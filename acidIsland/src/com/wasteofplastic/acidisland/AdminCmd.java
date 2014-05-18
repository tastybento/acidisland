package com.wasteofplastic.acidisland;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
	// Do not allow commands from the console
	if (!(sender instanceof Player)) {
	    return false;
	}
	final Player player = (Player) sender;
	// Check for zero parameters e.g., /acid
	switch (split.length) {
	case 0:
	    // Only give help if the player has permissions
	    // Permissions are split into admin permissions and mod permissions
	    player.sendMessage("/acid is a command for mods and ops only");
	    if (VaultHelper.checkPerm(player.getName(), "acidisland.admin.reload", player.getWorld()) || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid reload:" + ChatColor.WHITE + " reload configuration from file.");
	    }
	    if (VaultHelper.checkPerm(player.getName(), "acidisland.mod.topten", player.getWorld()) || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid topten:" + ChatColor.WHITE + " manually update the top 10 list");
	    }
	    if (VaultHelper.checkPerm(player.getName(), "acidisland.admin.register", player.getWorld()) || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid register <player>:" + ChatColor.WHITE + " set a player's island to your location");
	    }
	    if (VaultHelper.checkPerm(player.getName(), "acidisland.admin.delete", player.getWorld()) || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid delete <player>:" + ChatColor.WHITE + " delete an island (removes blocks).");
	    }
	    if (VaultHelper.checkPerm(player.getName(), "acidisland.mod.challenges", player.getWorld()) || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid completechallenge <challengename> <player>:" + ChatColor.WHITE
			+ " marks a challenge as complete");
	    }
	    if (VaultHelper.checkPerm(player.getName(), "acidisland.mod.challenges", player.getWorld()) || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid resetchallenge <challengename> <player>:" + ChatColor.WHITE
			+ " marks a challenge as incomplete");
	    }
	    if (VaultHelper.checkPerm(player.getName(), "acidisland.mod.challenges", player.getWorld()) || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid resetallchallenges <player>:" + ChatColor.WHITE + " resets all of the player's challenges");
	    }
	    if (VaultHelper.checkPerm(player.getName(), "acidisland.admin.purge", player.getWorld()) || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid purge [TimeInDays]:" + ChatColor.WHITE + " delete inactive islands older than [TimeInDays].");
	    }
	    if (VaultHelper.checkPerm(player.getName(), "acidisland.mod.team", player.getWorld()) || player.isOp()) {
		player.sendMessage(ChatColor.YELLOW + "/acid info <player>:" + ChatColor.WHITE + " check the team information for the given player.");
	    }
	    return true;
	case 1:
	    switch (split[0].toLowerCase()) {
	    case "reload":
		if ((VaultHelper.checkPerm(player.getName(), "acidisland.admin.reload", player.getWorld()) || player.isOp())) {
		    plugin.reloadConfig();
		    plugin.loadPluginConfig();
		    player.sendMessage(ChatColor.YELLOW + "Configuration reloaded from file.");
		    return true;
		}
		break;

	    case "topten":
		if (VaultHelper.checkPerm(player.getName(), "acidisland.mod.topten", player.getWorld()) || player.isOp()) {
		    player.sendMessage(ChatColor.YELLOW + "Generating the Top Ten list");
		    plugin.updateTopTen();
		    player.sendMessage(ChatColor.YELLOW + "Finished generation of the Top Ten list");
		    return true;
		}
		break;
	    case "purge":
		if (VaultHelper.checkPerm(player.getName(), "acidisland.admin.purge", player.getWorld()) || player.isOp()) {
		    if (purgeFlag) {
			player.sendMessage(ChatColor.RED + "Purge is already running, please wait for it to finish!");
			return true;
		    }
		    player.sendMessage(ChatColor.YELLOW + "Usage: /acid purge [TimeInDays]");
		    return true;
		}
	    case "confirm":
		if (VaultHelper.checkPerm(player.getName(), "acidisland.admin.purge", player.getWorld()) || player.isOp()) {
		    if (!confirmReq) {
			player.sendMessage(ChatColor.RED + "Time limit expired. Please issue command again!");
			return true;
		    } else {
			// Tell purge routine to go
			confirmOK = true;
			confirmReq = false;
		    }
		    return true;
		}


	    default:
		player.sendMessage(ChatColor.RED + "Unknown command.");
		return false;
	    }
	    break;
	case 2:
	    switch (split[0].toLowerCase()) {
	    case "purge":
		// PURGE Command
		if (VaultHelper.checkPerm(player.getName(), "acidisland.admin.purge", player.getWorld()) || player.isOp()) {
		    // Purge runs in the background so if one is already running this flag stops a repeat
		    if (purgeFlag) {
			player.sendMessage(ChatColor.RED + "A purge is already running, please wait for it to finish!");
			return true;
		    }
		    // Set the flag
		    purgeFlag = true;
		    // Convert days to hours - no other limit checking?
		    final int time = Integer.parseInt(split[1]) * 24;
		    player.sendMessage(ChatColor.YELLOW + "Calculating which islands have been inactive for more than " + split[1] + " days.");
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
				player.sendMessage(ChatColor.YELLOW + "No inactive islands to remove.");
				return;
			    }
			    player.sendMessage(ChatColor.YELLOW + "This will remove " + removeList.size() + " inactive islands!");
			    player.sendMessage(ChatColor.RED + "DANGER! Do not run this with players on the server! MAKE BACKUP OF WORLD!");
			    player.sendMessage(ChatColor.RED + "Type /acid confirm to proceed within 10 seconds");
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
					player.sendMessage(ChatColor.YELLOW + "Purge cancelled.");
					this.cancel();
				    } else if (confirmOK) {
					// Set up a repeating task to run every 5 seconds to remove
					// islands one by one and then cancel when done
					new BukkitRunnable() {
					    @Override
					    public void run() {
						if (removeList.isEmpty() && purgeFlag) {
						    purgeFlag = false;
						    player.sendMessage(ChatColor.YELLOW + "Finished purging of inactive islands.");
						    this.cancel();
						} 

						if (removeList.size() > 0 && purgeFlag) {
						    plugin.deletePlayerIsland(removeList.get(0));
						    player.sendMessage(ChatColor.YELLOW + "Purge: Removing " + players.getName(removeList.get(0)) + "'s island");
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
		} else {
		    player.sendMessage(ChatColor.RED + "You can't access that command!");
		}
		return true;
	    case "delete":
		if (VaultHelper.checkPerm(player.getName(), "acidisland.admin.delete", player.getWorld()) || player.isOp()) {
		    // Convert name to a UUID
		    final UUID playerUUID = players.getUUID(split[1]);
		    if (!players.isAKnownPlayer(playerUUID)) {
			player.sendMessage(ChatColor.RED + "Error: Invalid Player (check spelling)");
			return true;
		    } else {
			if (players.getIslandLocation(playerUUID) != null) {
			    player.sendMessage(ChatColor.YELLOW + "Removing " + split[1] + "'s island.");
			    plugin.deletePlayerIsland(playerUUID);
			    return true;
			}
			player.sendMessage("Error: That player does not have an island!");
			return true;
		    }
		} else {
		    player.sendMessage(ChatColor.RED + "You can't access that command!");
		}
		return true;
	    case "register":
		if (VaultHelper.checkPerm(player.getName(), "acidisland.admin.register", player.getWorld()) || player.isOp()) {
		    // Convert name to a UUID
		    final UUID playerUUID = players.getUUID(split[1]);
		    if (!players.isAKnownPlayer(playerUUID)) {
			player.sendMessage(ChatColor.RED + "Error: Invalid Player (check spelling)");
			return true;
		    } else {
			if (adminSetPlayerIsland(sender, player.getLocation(), playerUUID)) {
			    player.sendMessage(ChatColor.GREEN + "Set " + split[1] + "'s island to the bedrock nearest you.");
			} else {
			    player.sendMessage(ChatColor.RED + "Bedrock not found: unable to set the island!");
			}
			return true;
		    }
		} else {
		    player.sendMessage(ChatColor.RED + "You can't access that command!");
		}
		return true;
	    case "info":
		if (VaultHelper.checkPerm(player.getName(), "acidisland.mod.team", player.getWorld()) || player.isOp()) {
		    // Convert name to a UUID
		    final UUID playerUUID = players.getUUID(split[1]);
		    if (!players.isAKnownPlayer(playerUUID)) {
			player.sendMessage(ChatColor.RED + "Error: Invalid Player (check spelling)");
			return true;
		    } else {
			if (players.inTeam(playerUUID)) {
			    final UUID leader = players.getTeamLeader(playerUUID);
			    final List<UUID> pList = players.getMembers(leader);
			    if (pList.contains(split[1])) {
				if (playerUUID.equals(leader)) {
				    pList.remove(playerUUID);
				} else {
				    pList.remove(leader);
				}
			    }
			    player.sendMessage(ChatColor.GREEN + players.getName(leader) + " " + ChatColor.WHITE + pList.toString());
			    player.sendMessage(ChatColor.YELLOW + "Island Location:" + ChatColor.WHITE + " (" + players.getTeamIslandLocation(playerUUID).getBlockX() + ","
				    + players.getTeamIslandLocation(playerUUID).getBlockY() + "," + players.getTeamIslandLocation(playerUUID).getBlockZ() + ")");
			} else {
			    player.sendMessage(ChatColor.YELLOW + "That player is not a member of an island team.");
			    if (players.hasIsland(playerUUID)) {
				player.sendMessage(ChatColor.YELLOW + "Island Location:" + ChatColor.WHITE + " (" + players.getIslandLocation(playerUUID).getBlockX() + ","
					+ players.getIslandLocation(playerUUID).getBlockY() + "," + players.getIslandLocation(playerUUID).getBlockZ() + ")");
			    }
			    if (!(players.getTeamLeader(playerUUID) == null)) {
				player.sendMessage(ChatColor.RED + "Team leader should be null!");
			    }
			    if (!players.getMembers(playerUUID).isEmpty()) {
				player.sendMessage(ChatColor.RED + "Player has team members, but shouldn't!");
			    }
			}
			return true;
		    }
		} else {
		    player.sendMessage(ChatColor.RED + "You can't access that command!");
		}
		return true;
	    case "resetallchallenges":
		if (VaultHelper.checkPerm(player.getName(), "acidisland.mod.challenges", player.getWorld()) || player.isOp()) {
		    // Convert name to a UUID
		    final UUID playerUUID = players.getUUID(split[1]);
		    if (!players.isAKnownPlayer(playerUUID)) {
			player.sendMessage(ChatColor.RED + "Error: Invalid Player (check spelling)");
			return true;
		    }
		    players.resetAllChallenges(playerUUID);
		    player.sendMessage(ChatColor.YELLOW + split[1] + " has had all challenges reset.");
		} else {
		    player.sendMessage(ChatColor.RED + "You can't access that command!");
		}
		return true;
	    case "checkteam":
		if (VaultHelper.checkPerm(player.getName(), "acidisland.mod.team", player.getWorld()) || player.isOp()) {
		    player.sendMessage(ChatColor.YELLOW + "Checking Team of " + split[1]);
		    // Convert name to a UUID
		    final UUID playerUUID = players.getUUID(split[1]);
		    if (!players.isAKnownPlayer(playerUUID)) {
			player.sendMessage(ChatColor.RED + "That player doesn't exist!");
			return true;
		    }
		    if (players.inTeam(playerUUID)) {
			if (players.getTeamLeader(playerUUID).equals(playerUUID)) {
			    // List member names
			    for (UUID member : players.getMembers(playerUUID)) {
				player.sendMessage(players.getName(member));
			    }
			} else {
			    final UUID leader = players.getTeamLeader(playerUUID);
			    // List member names
			    for (UUID member : players.getMembers(leader)) {
				player.sendMessage(players.getName(member));
			    }
			}
		    } else {
			player.sendMessage(ChatColor.RED + "That player is not in an island team!");
		    }
		} else {
		    player.sendMessage(ChatColor.RED + "You can't access that command!");
		}
		return true;
	    default:
		return false;
	    }
	case 3:
	    switch (split[0].toLowerCase()) {
	    case "completechallenge":
		if (VaultHelper.checkPerm(player.getName(), "acidisland.mod.challenges", player.getWorld()) || player.isOp()) {
		    // Convert name to a UUID
		    final UUID playerUUID = players.getUUID(split[2]);
		    if (!players.isAKnownPlayer(playerUUID)) {
			player.sendMessage(ChatColor.RED + "Error: Invalid Player (check spelling)");
			return true;
		    }
		    if (players.checkChallenge(playerUUID,split[1].toLowerCase()) || !players.get(playerUUID).challengeExists(split[1].toLowerCase())) {
			player.sendMessage(ChatColor.RED + "Challenge doesn't exist or is already completed");
			return true;
		    }
		    players.get(playerUUID).completeChallenge(split[1].toLowerCase());
		    player.sendMessage(ChatColor.YELLOW + "challange: " + split[1].toLowerCase() + " has been completed for " + split[2]);
		    return true;
		} else {
		    player.sendMessage(ChatColor.RED + "You can't access that command!");
		}
		return true;
	    case "resetchallenge":
		if (VaultHelper.checkPerm(player.getName(), "acidisland.mod.challenges", player.getWorld()) || player.isOp()) {
		    // Convert name to a UUID
		    final UUID playerUUID = players.getUUID(split[2]);
		    if (!players.isAKnownPlayer(playerUUID)) {
			player.sendMessage(ChatColor.RED + "Error: Invalid Player (check spelling)");
			return true;
		    }
		    if (!players.checkChallenge(playerUUID,split[1].toLowerCase())
			    || !players.get(playerUUID).challengeExists(split[1].toLowerCase())) {
			player.sendMessage(ChatColor.RED + "Challenge doesn't exist or isn't yet completed");
			return true;
		    }
		    players.resetChallenge(playerUUID,split[1].toLowerCase());
		    player.sendMessage(ChatColor.YELLOW + "challange: " + split[1].toLowerCase() + " has been reset for " + split[2]);
		    return true;
		} else {
		    player.sendMessage(ChatColor.RED + "You can't access that command!");
		}
		return true;
	    default:
		return false;
	    }
	default:
	    return false;
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
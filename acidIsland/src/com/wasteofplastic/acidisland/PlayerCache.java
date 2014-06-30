/**
 * 
 */
package com.wasteofplastic.acidisland;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * @author ben
 * Provides a memory cache of online player information
 * This is the one-stop-shop of player info
 * If the player is not cached, then a request is made to Players to obtain it
 */
public class PlayerCache {
    private HashMap<UUID, Players> playerCache = new HashMap<UUID, Players>();
    private final AcidIsland plugin;

    public PlayerCache(AcidIsland plugin) {
	this.plugin = plugin;
	final Player[] serverPlayers = Bukkit.getServer().getOnlinePlayers();
	for (Player p : serverPlayers) {
	    if (p.isOnline()) {
		final Players playerInf = new Players(plugin, p.getUniqueId());
		// Make sure parties are working correctly
		if (playerInf.inTeam() && playerInf.getTeamIslandLocation() == null) {
		    final Players leaderInf = new Players(plugin, playerInf.getTeamLeader());
		    playerInf.setTeamIslandLocation(leaderInf.getIslandLocation());
		    playerInf.save();
		}
		// Add this player to the online cache
		playerCache.put(p.getUniqueId(), playerInf);
	    }
	}
    }
    
    /*
     * Cache control methods
     */
        
    public void addPlayer(final UUID playerUUID) {
	if (!playerCache.containsKey(playerUUID)) {
	    final Players player = new Players(plugin, playerUUID);
	    playerCache.put(playerUUID,player);
	}
    }
    
    /**
     * Stores the player's info to a file and removes the player from the list
     * of currently online players
     * 
     * @param player
     *            - name of player
     */
    public void removeOnlinePlayer(final UUID player) {
	if (playerCache.containsKey(player)) {
	    playerCache.get(player).save();
	    playerCache.remove(player);
	    plugin.getLogger().info("Removing player from cache: " + player);
	}
    }
    
    /**
     * Removes all players on the server now from cache and saves their info
     */
    public void removeAllPlayers() {
	for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
	    if (playerCache.containsKey(onlinePlayer.getUniqueId())) {
		playerCache.get(onlinePlayer.getUniqueId()).save();
		playerCache.remove(onlinePlayer.getUniqueId());
	    }
	}
    }

    /*
     * Player info query methods
     */
    /**
     * Returns location of player's island from cache if available
     * @param playerUUID
     * @return Location of player's island
     */
    public Location getPlayerIsland(final UUID playerUUID) {
	if (playerCache.containsKey(playerUUID)) {
	    return playerCache.get(playerUUID).getIslandLocation();
	}
	final Players player = new Players(plugin, playerUUID);
	return player.getIslandLocation();
    }

    /**
     * Checks if the player is known or not by looking through the filesystem
     * 
     * @param uniqueID
     * @return true if player is know, otherwise false
     */
    public boolean isAKnownPlayer(final UUID uniqueID) {
	if (uniqueID == null) {
	    return false;
	}
	if (playerCache.containsKey(uniqueID)) {
	    return true;
	} else {
	    // Get the file system
	    final File folder = plugin.playersFolder;
	    final File[] files = folder.listFiles();
	    // Go through the native YAML files
	    for (final File f : files) {
		// Need to remove the .yml suffix
		if (f.getName().endsWith(".yml")) {
		    if (UUID.fromString(f.getName().substring(0, f.getName().length() - 4)).equals(uniqueID)) {
			return true;
		    }
		}
	    }
	}
	// Not found, sorry.
	return false;
    }
    
    /**
     * Returns the player object for the named player
     * @param playerUUID - String name of player
     * @return - player object
     */
    public Players get(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID);
    }

    /**
     * Checks if player has island from cache if available
     * @param playerUUID - string name of player
     * @return true if player has island
     */
    public boolean hasIsland(final UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).hasIsland();
    }

     /**
      * Checks if player is in a Team from cache if available
     * @param playerUUID
     * @return
     */
    public boolean inTeam(final UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).inTeam();
    }

    public void removeIsland(UUID playerUUID) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setHasIsland(false);
	playerCache.get(playerUUID).setHomeLocation(null);
	playerCache.get(playerUUID).setIslandLocation(null);
	playerCache.get(playerUUID).save(); // Needed?
    }

    public void setHomeLocation(UUID playerUUID, Location location) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setHomeLocation(location);
    }

    /**
     * Returns the home location, or null if none
     * @param playerUUID
     * @return
     */
    public Location getHomeLocation(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getHomeLocation();
   }

    public Location getIslandLocation(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getIslandLocation();
    }

    public void setHasIsland(UUID playerUUID, boolean b) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setHasIsland(b);
   }

    public void setIslandLocation(UUID playerUUID, Location islandLocation) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setIslandLocation(islandLocation);	
    }

    public Integer getIslandLevel(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getIslandLevel();
    }

    public void setIslandLevel(UUID playerUUID, Integer islandLevel) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setIslandLevel(islandLevel);
    }

    public void setTeamIslandLocation(UUID playerUUID, Location islandLocation) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setTeamIslandLocation(islandLocation);	
    }

    public boolean checkChallenge(UUID playerUUID, String challenge) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).checkChallenge(challenge);
    }

    public void resetChallenge(UUID playerUUID, String challenge) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).resetChallenge(challenge);	
    }

    public void resetAllChallenges(UUID playerUUID) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).resetAllChallenges();	
    }

    public void setJoinTeam(UUID playerUUID, UUID teamLeader, Location islandLocation) {
	addPlayer(playerUUID);
	addPlayer(teamLeader);
	playerCache.get(playerUUID).setJoinTeam(teamLeader, islandLocation);
    }
    /**
     * Called when a player leaves a team Resets inTeam, teamLeader,
     * islandLevel, teamIslandLocation, homeLocation, islandLocation and members array
     */
    public void setLeaveTeam(UUID playerUUID) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setLeaveTeam();
    }

    /**
     * Returns a list of team member UUID's 
     * @param playerUUID
     * @return
     */
    public List<UUID> getMembers(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getMembers();
    }

    public void addTeamMember(UUID teamLeader, UUID playerUUID) {
	addPlayer(teamLeader);
	addPlayer(playerUUID);
	playerCache.get(teamLeader).addTeamMember(playerUUID);
    }

    public void removeMember(UUID teamLeader, UUID playerUUID) {
	addPlayer(teamLeader);
	addPlayer(playerUUID);
	playerCache.get(teamLeader).removeMember(playerUUID);
    }

    public UUID getTeamLeader(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getTeamLeader();
   }

    /**
     * Saves the player's info to the file system
     * @param playerUUID
     */
    public void save(UUID playerUUID) {
	playerCache.get(playerUUID).save();
    }

    public void completeChallenge(UUID playerUUID, String challenge) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).completeChallenge(challenge);	
    }

    public boolean challengeExists(UUID playerUUID, String challenge) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).challengeExists(challenge);	
   }

    /**
     * Attempts to return a UUID for a given player's name
     * @param string
     * @return
     */
    public UUID getUUID(String string) {
	for (UUID id : playerCache.keySet()) {
	    String name = playerCache.get(id).getPlayerName();
	    //plugin.getLogger().info("DEBUG: Testing name " + name);
	    if (name != null && name.equalsIgnoreCase(string)) {
		return id;
	    }
	}
	// Look in the file system
	for (final File f : plugin.playersFolder.listFiles()) {
	    // Need to remove the .yml suffix
	    String fileName = f.getName();
	    if (fileName.endsWith(".yml")) {
		try {
		    final UUID playerUUID = UUID.fromString(fileName.substring(0, fileName.length() - 4));
		    if (plugin.getServer().getOfflinePlayer(playerUUID).getName().equalsIgnoreCase(string)) {
			return playerUUID;
		    }
		} catch (Exception e) {
		}
	    }
	}
	return null;
    }

    public void setPlayerName(UUID uniqueId, String name) {
	addPlayer(uniqueId);
	playerCache.get(uniqueId).setPlayerN(name);
    }

    /**
     * Obtains the name of the player from their UUID
     * Player must have logged into the game before
     * @param playerUUID
     * @return String - playerName
     */
    public String getName(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getPlayerName();
    }

    public Location getTeamIslandLocation(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getTeamIslandLocation();
    }

}


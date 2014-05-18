package com.wasteofplastic.acidisland;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveEvents implements Listener {
    private AcidIsland plugin;
    private PlayerCache players;

    public JoinLeaveEvents(AcidIsland acidIsland, PlayerCache onlinePlayers) {
	this.plugin = acidIsland;
	this.players = onlinePlayers;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(final PlayerJoinEvent event) {
	final UUID playerUUID = event.getPlayer().getUniqueId();
	if (players.inTeam(playerUUID) && players.getTeamIslandLocation(playerUUID) == null) {
	    final UUID leader = players.getTeamLeader(playerUUID);
	    players.setTeamIslandLocation(playerUUID, players.getIslandLocation(leader));
	}
	players.addPlayer(playerUUID);
	// Set the player's name (it may have changed)
	players.setPlayerName(playerUUID, event.getPlayer().getName());
	players.save(playerUUID);
	plugin.getLogger().info("Cached " + event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
	players.removeOnlinePlayer(event.getPlayer().getUniqueId());
    }
}
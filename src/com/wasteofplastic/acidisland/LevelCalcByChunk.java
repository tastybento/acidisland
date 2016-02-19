/*******************************************************************************
 * This file is part of ASkyBlock.
 *
 *     ASkyBlock is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ASkyBlock is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.wasteofplastic.acidisland;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

import com.wasteofplastic.acidisland.events.IslandLevelEvent;

/**
 * A class that calculates the level of an island very quickly by copying island
 * chunks to a list and then processing asynchronously.
 * 
 * @author tastybento
 * 
 */
public class LevelCalcByChunk {

    public LevelCalcByChunk(ASkyBlock plugin, UUID targetPlayer, Player asker) {
	this(plugin, targetPlayer, asker, false);
    }

    /**
     * Calculates the level of an island
     * 
     * @param plugin
     * @param targetPlayer
     * @param asker
     */
    public LevelCalcByChunk(final ASkyBlock plugin, final UUID targetPlayer, final Player asker, final boolean silent) {
	//plugin.getLogger().info("DEBUG: running level calc " + silent);
	// Get player's island
	final Island island = plugin.getGrid().getIsland(targetPlayer);
	if (island != null) {
	    // Check if player's island world is the nether or overworld and adjust accordingly
	    World world = plugin.getPlayers().getIslandLocation(targetPlayer).getWorld();
	    // Get the chunks
	    //long nano = System.nanoTime();
	    Set<ChunkSnapshot> chunkSnapshot = new HashSet<ChunkSnapshot>();
	    for (int x = island.getMinProtectedX(); x < (island.getMinProtectedX() + island.getProtectionSize() + 16); x += 16) {
		for (int z = island.getMinProtectedZ(); z < (island.getMinProtectedZ() + island.getProtectionSize() + 16); z += 16) {
		    chunkSnapshot.add(world.getBlockAt(x, 0, z).getChunk().getChunkSnapshot());
		    //plugin.getLogger().info("DEBUG: getting chunk at " + x + ", " + z);
		}
	    }
	    //plugin.getLogger().info("DEBUG: time = " + (System.nanoTime() - nano) / 1000000 + " ms");
	    //plugin.getLogger().info("DEBUG: size of chunk ss = " + chunkSnapshot.size());
	    final Set<ChunkSnapshot> finalChunk = chunkSnapshot;
	    final int worldHeight = world.getMaxHeight();
	    //plugin.getLogger().info("DEBUG:world height = " +worldHeight);
	    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

		@SuppressWarnings("deprecation")
		@Override
		public void run() {
		    // Copy the limits hashmap
		    HashMap<MaterialData, Integer> limitCount = new HashMap<MaterialData, Integer>();
		    for (MaterialData m : Settings.blockLimits.keySet()) {
			limitCount.put(m, Settings.blockLimits.get(m));
		    }
		    // Calculate the island score
		    int blockCount = 0;
		    int underWaterBlockCount = 0;
		    for (ChunkSnapshot chunk: finalChunk) {
			for (int x = 0; x< 16; x++) { 
			    // Check if the block coord is inside the protection zone and if not, don't count it
			    if (chunk.getX() * 16 + x < island.getMinProtectedX() || chunk.getX() * 16 + x >= island.getMinProtectedX() + island.getProtectionSize()) {
				//plugin.getLogger().info("Block is outside protected area - x = " + (chunk.getX() * 16 + x));
				continue;
			    }
			    for (int z = 0; z < 16; z++) {
				// Check if the block coord is inside the protection zone and if not, don't count it
				if (chunk.getZ() * 16 + z < island.getMinProtectedZ() || chunk.getZ() * 16 + z >= island.getMinProtectedZ() + island.getProtectionSize()) {
				    //plugin.getLogger().info("Block is outside protected area - z = " + (chunk.getZ() * 16 + z));
				    continue;
				}
				for (int y = 0; y < worldHeight; y++) {
				    int type = chunk.getBlockTypeId(x, y, z);
				    int data = chunk.getBlockData(x, y, z);
				    MaterialData md = new MaterialData(type,(byte) data);
				    MaterialData generic = new MaterialData(type);
				    if (type != 0) { // AIR
					if (limitCount.containsKey(md) && Settings.blockValues.containsKey(md)) {
					    int count = limitCount.get(md);
					    //plugin.getLogger().info("DEBUG: Count for non-generic " + md + " is " + count);
					    if (count > 0) {
						limitCount.put(md, --count);
						if (y<Settings.sea_level) {
						    underWaterBlockCount += Settings.blockValues.get(md);
						} else {
						    blockCount += Settings.blockValues.get(md);
						}
					    }
					} else if (limitCount.containsKey(generic) && Settings.blockValues.containsKey(generic)) {
					    int count = limitCount.get(generic);
					    //plugin.getLogger().info("DEBUG: Count for generic " + generic + " is " + count);
					    if (count > 0) {  
						limitCount.put(md, --count);
						if (y<Settings.sea_level) {
						    underWaterBlockCount += Settings.blockValues.get(generic);
						} else {
						    blockCount += Settings.blockValues.get(generic);
						}
					    }
					} else if (Settings.blockValues.containsKey(md)) {
					    //plugin.getLogger().info("DEBUG: Adding " + md + " = " + Settings.blockValues.get(md));
					    if (y<Settings.sea_level) {
						underWaterBlockCount += Settings.blockValues.get(md);
					    } else {
						blockCount += Settings.blockValues.get(md);
					    }
					} else if (Settings.blockValues.containsKey(generic)) {
					    //plugin.getLogger().info("DEBUG: Adding " + generic + " = " + Settings.blockValues.get(generic));
					    if (y<Settings.sea_level) {
						underWaterBlockCount += Settings.blockValues.get(generic);
					    } else {
						blockCount += Settings.blockValues.get(generic);
					    }
					}
				    }
				}
			    }
			}
		    }
		    blockCount += (int)((double)underWaterBlockCount * Math.max(Settings.underWaterMultiplier,1D));
		    //System.out.println("block count = "+blockCount);
		    final int score = blockCount / Settings.levelCost;

		    // Return to main thread
		    plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

			@Override
			public void run() {
			    //plugin.getLogger().info("DEBUG: updating player");
			    int oldLevel = plugin.getPlayers().getIslandLevel(targetPlayer);
			    // Update player and team mates
			    plugin.getPlayers().setIslandLevel(targetPlayer, score);
			    //plugin.getLogger().info("DEBUG: set island level, now trying to save player");
			    plugin.getPlayers().save(targetPlayer);
			    //plugin.getLogger().info("DEBUG: save player, now looking at team members");
			    // Update any team members too
			    if (plugin.getPlayers().inTeam(targetPlayer)) {
				//plugin.getLogger().info("DEBUG: player is in team");
				for (UUID member : plugin.getPlayers().getMembers(targetPlayer)) {
				    //plugin.getLogger().info("DEBUG: updating team member level too");
				    plugin.getPlayers().setIslandLevel(member, score);
				    plugin.getPlayers().save(member);
				}
			    }
			    //plugin.getLogger().info("DEBUG: finished team member saving");
			    if (!silent) {
				// Tell offline team members the island level increased.
				if (plugin.getPlayers().getIslandLevel(targetPlayer) > oldLevel) {
				    //plugin.getLogger().info("DEBUG: telling offline players");
				    plugin.getMessages().tellOfflineTeam(targetPlayer, ChatColor.GREEN + plugin.myLocale(targetPlayer).islandislandLevelis + " " + ChatColor.WHITE
					    + plugin.getPlayers().getIslandLevel(targetPlayer));
				}
				if (asker.isOnline()) {
				    //plugin.getLogger().info("DEBUG: updating player GUI");
				    asker.sendMessage(ChatColor.GREEN + plugin.myLocale(asker.getUniqueId()).islandislandLevelis + " " + ChatColor.WHITE + plugin.getPlayers().getIslandLevel(targetPlayer));
				}
			    }
			    //plugin.getLogger().info("DEBUG: updating top ten");
			    // Only update top ten if the asker doesn't have this permission
			    if (!(asker.getUniqueId().equals(targetPlayer) && asker.hasPermission(Settings.PERMPREFIX + "excludetopten"))) {
				if (plugin.getPlayers().inTeam(targetPlayer)) {
				    UUID leader = plugin.getPlayers().getTeamLeader(targetPlayer);
				    if (leader != null) {
					TopTen.topTenAddEntry(leader, score);
				    }
				} else {
				    TopTen.topTenAddEntry(targetPlayer, score);
				}
			    }
			    // Fire the level event
			    Island island = plugin.getGrid().getIsland(targetPlayer);
			    final IslandLevelEvent event = new IslandLevelEvent(targetPlayer, island, score);
			    plugin.getServer().getPluginManager().callEvent(event);
			}});
		}});
	}
    }
}
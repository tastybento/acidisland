/**
 * 
 */
package com.wasteofplastic.acidisland;

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Location;

/**
 * @author tastybento
 * This class manages the island grid. It knows where every island is, and where new
 * ones should go. It can handle any size of island or protection size
 * The grid is stored in a YML file.
 */
public class GridManager {
    // 2D grid of islands, x,z
    private TreeMap<Integer,TreeMap<Integer,Island>> grid = new TreeMap<Integer,TreeMap<Integer,Island>>();
    
    private int maxX = 0;
    private int maxZ = 0;
    private int minX = 0;
    private int minZ = 0;
    
    protected void loadGrid() {
	
    }
    
    /**
     * Returns the island at the location or null if there is none
     * @param location
     * @return Island object
     */
    protected Island getIslandAt(Location location) {
	int x = location.getBlockX();
	Entry<Integer, TreeMap<Integer,Island>> en = grid.lowerEntry(x);
	if (en != null) {
	    int z = location.getBlockZ();
	    Entry<Integer, Island> ent = en.getValue().lowerEntry(z);
	    if (ent != null) {
		return ent.getValue();
	    }
	}
	return null;
    }
    
    /**
     * Returns the owner of the island at location
     * @param location
     * @return UUID of owner
     */
    protected UUID getOwnerOfIslandAt(Location location) {
	Island island = getIslandAt(location);
	if (island != null) {
	    return island.getOwner();
	}
	return null;
    }
}

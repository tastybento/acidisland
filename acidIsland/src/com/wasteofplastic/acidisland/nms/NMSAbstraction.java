package com.wasteofplastic.acidisland.nms;

import org.bukkit.Material;
import org.bukkit.block.Block;

public interface NMSAbstraction {

    /**
     * Update the low-level chunk information for the given block to the new block ID and data.  This
     * change will not be propagated to clients until the chunk is refreshed to them (e.g. by the
     * Bukkit world.refreshChunk() method).  The block's light level will also not be recalculated,
     * nor will the light level of any nearby blocks which might be affected by the change in this
     * block.
     * @param b - block to be changed
     * @param mat - material to change it into
     */
    public void setBlockSuperFast(Block b, Material mat);
}

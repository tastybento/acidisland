/**
 * 
 */
package com.wasteofplastic.acidisland.fallback;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.wasteofplastic.acidisland.nms.NMSAbstraction;

/**
 * @author ben
 *
 */
public class NMSHandler implements NMSAbstraction {

    /* (non-Javadoc)
     * @see com.wasteofplastic.acidisland.nms.NMSAbstraction#setBlockSuperFast(org.bukkit.block.Block, org.bukkit.Material)
     */
    @Override
    public void setBlockSuperFast(Block b, Material mat) {
	b.setType(mat);
    }

}

/**
 * 
 */
package com.wasteofplastic.acidisland.v1_7_R4;

import java.lang.reflect.Field;

import net.minecraft.server.v1_7_R4.ChunkSection;

import org.bukkit.Chunk;
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
    @SuppressWarnings("deprecation")
    @Override
    public void setBlockSuperFast(Block b, Material mat) {
	int typeId = mat.getId();
	Chunk c = b.getChunk();
	try {

	    net.minecraft.server.v1_7_R4.Chunk chunk = ((org.bukkit.craftbukkit.v1_7_R4.CraftChunk) c).getHandle();

	    Field f = chunk.getClass().getDeclaredField("sections");
	    f.setAccessible(true);
	    ChunkSection[] sections = (ChunkSection[]) f.get(chunk);
	    ChunkSection chunksection = sections[b.getY() >> 4];

	    if (chunksection == null) {
		if (typeId == 0)
		    return;
		chunksection = sections[b.getY() >> 4] = new ChunkSection(b.getY() >> 4 << 4, !chunk.world.worldProvider.f);
	    }

	    net.minecraft.server.v1_7_R4.Block mb = net.minecraft.server.v1_7_R4.Block.getById(typeId);
	    chunksection.setTypeId(b.getX() & 15, b.getY() & 15, b.getZ() & 15, mb);
	    //chunksection.setData(b.getX() & 15, b.getY() & 15, b.getZ() & 15, data);

	} catch (Exception e) {
	    b.setType(mat);
	    //e.printStackTrace();
	}

    }
}

package com.wasteofplastic.acidisland;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

/**
 * @author ben
 * Creates the world
 */
public class AcidChunkGenerator extends ChunkGenerator {
    //@SuppressWarnings("deprecation")
    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid)
    {
	byte[][] result = new byte[world.getMaxHeight() / 16][];
	//Bukkit.getLogger().info("DEBUG: sea_level" + Settings.sea_level);
	if (Settings.sea_level == 0) {
	    return result;
	} else {
	    for (int x = 0; x < 16; x++) {
		for (int z = 0; z < 16; z++) {
		    for (int y = 0; y < Settings.sea_level; y++) {
			setBlock(result,x,y,z, (byte) Material.STATIONARY_WATER.getId()); // Stationary Water
			// Allows stuff to fall through into oblivion, thus keeping lag to a minimum
		    }
		}
	    }
	    return result;
	}
    }
    /*
    @Override
    public byte[] generate(final World world, final Random rand, final int chunkx, final int chunkz) {
	final byte[] result = new byte[(world.getMaxHeight() / 16)*4096];
	// This generator creates water world with no base
	for (int x = 0; x < 16; x++) {
	    for (int z = 0; z < 16; z++) {
		for (int y = 0; y < 50; y++) {
		    result[(x * 16 + z) * 128 + y] = 9; // Stationary Water
		    // Allows stuff to fall through into oblivion, thus keeping lag to a minimum
		}
	    }
	}
	return result;
    }*/

    void setBlock(byte[][] result, int x, int y, int z, byte blkid) {
	// is this chunk part already initialized?
	if (result[y >> 4] == null) {
	    // Initialize the chunk part
	    result[y >> 4] = new byte[4096];
	}
	// set the block (look above, how this is done)
	result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
    }

    // This needs to be set to return true to override minecraft's default
    // behavior
    @Override
    public boolean canSpawn(World world, int x, int z) {
	return true;
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(final World world) {
	return Arrays.asList(new BlockPopulator[0]);
    }
}
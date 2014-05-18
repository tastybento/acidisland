package com.wasteofplastic.acidisland;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

/**
 * @author ben
 * Creates the world
 */
public class AcidChunkGenerator extends ChunkGenerator {
    @Override
    public byte[] generate(final World world, final Random rand, final int chunkx, final int chunkz) {
	final byte[] result = new byte[32768];
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
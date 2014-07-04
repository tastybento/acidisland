package com.wasteofplastic.acidisland;

import java.util.List;
import java.util.Set;

import org.bukkit.inventory.ItemStack;

/**
 * @author ben
 * Where all the settings are
 */
public class Settings {
    public static Set<String> challengeList;
    public static int waiverAmount;
    public static List<String> challengeLevels;
    public static double acidDamage;
    public static int resetWait;
    public static int maxTeamSize;
    public static String worldName;
    public static int monsterSpawnLimit;
    public static int animalSpawnLimit;
    public static int waterAnimalSpawnLimit;
    // IslandGuard settings
    public static String allowPvP;
    public static boolean allowBreakBlocks;
    public static boolean allowPlaceBlocks;
    public static boolean allowBedUse;
    public static boolean allowBucketUse;
    public static boolean allowShearing;
    public static boolean allowEnderPearls;
    public static boolean allowDoorUse;
    public static boolean allowLeverButtonUse;
    public static boolean allowCropTrample;
    public static boolean allowChestAccess;
    public static boolean allowFurnaceUse;
    public static boolean allowRedStone;
    public static boolean allowMusic;
    public static boolean allowCrafting;
    public static boolean allowBrewing;
    public static boolean allowGateUse;
    
    public static ItemStack[] chestItems;
    public static int islandDistance;
    public static int sea_level;
    public static int island_protectionRange;
    public static int abandonedIslandLevel;
    public static boolean absorbLava;
    public static boolean absorbFire;
    public static boolean restoreWater;
    public static boolean canPlaceWater;
    public static int spongeRadius;
    public static boolean threadedSpongeSave;
    public static int flowTimeMult;
    public static boolean attackFire;
    public static Object excludedWorlds;
    public static boolean pistonMove;
    public static boolean spongeSaturation;
    public static Double startingMoney;
}
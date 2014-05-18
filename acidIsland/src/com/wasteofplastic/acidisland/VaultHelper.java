package com.wasteofplastic.acidisland;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Helper class for Vault Economy and Permissions
 */
public class VaultHelper {
    public static Economy econ = null;
    public static Permission permission = null;

    /**
     * Sets up the economy instance
     * @return
     */
    public static boolean setupEconomy() {
	RegisteredServiceProvider<Economy> economyProvider = AcidIsland.getPlugin().getServer().getServicesManager()
		.getRegistration(net.milkbowl.vault.economy.Economy.class);
	if (economyProvider != null) {
	    econ = economyProvider.getProvider();
	}
	return econ != null;
    }

    /**
     * Sets up the permissions instance
     * @return
     */
    public static boolean setupPermissions() {
	RegisteredServiceProvider<Permission> permissionProvider = AcidIsland.getPlugin().getServer().getServicesManager()
		.getRegistration(net.milkbowl.vault.permission.Permission.class);
	if (permissionProvider != null) {
	    permission = permissionProvider.getProvider();
	}
	return (permission != null);
    }
    

    /**
     * Checks permission of player in world or in any world
     * @param player
     * @param perm
     * @param world
     * @return
     */
    public static boolean checkPerm(final String player, final String perm, final World world) {
	if (permission.has((String) null, player, perm)) {
	    return true;
	}
	return permission.has(world, player, perm);
   }
    
    /**
     * Adds permission to player
     * @param player
     * @param perm
     */
    public static void addPerm(final Player player, final String perm) {
	permission.playerAdd((String) null, player.getName(), perm);
    }

    /**
     * Removes a player's permission
     * @param player
     * @param perm
     */
    public static void removePerm(final Player player, final String perm) {
	permission.playerRemove((String) null, player.getName(), perm);
    }


}
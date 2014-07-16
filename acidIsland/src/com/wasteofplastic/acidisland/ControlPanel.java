package com.wasteofplastic.acidisland;

import java.util.HashMap;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author ben
 * Provides a handy control panel and minishop
 */
public class ControlPanel implements Listener {

    private static YamlConfiguration miniShopFile;
    private static HashMap<Integer, MiniShopItem> store = new HashMap<Integer,MiniShopItem>();

    //public static final Inventory challenges = Bukkit.createInventory(null, 9, ChatColor.YELLOW + "Challenges");


    public static final Inventory miniShop = Bukkit.createInventory(null, 9, ChatColor.YELLOW + Locale.islandMiniShopTitle);
    // The first parameter, is the inventory owner. I make it null to let everyone use it.
    //The second parameter, is the slots in a inventory. Must be a multiple of 9. Can be up to 54.
    //The third parameter, is the inventory name. This will accept chat colors.

    static {
	loadShop();
    }

    public static void loadShop() {
	//The first parameter is the Material, then the durability (if wanted), slot, descriptions
	// Minishop
	store.clear();
	miniShopFile = AcidIsland.loadYamlFile("minishop.yml");
	ConfigurationSection items = miniShopFile.getConfigurationSection("items");
	AcidIsland plugin = AcidIsland.getPlugin();
	//plugin.getLogger().info("DEBUG: loading the shop. items = " + items.toString());
	if (items != null) {
	    // Run through items
	    int slot = 0;
	    for (String item : items.getKeys(false)) {
		try {
		    String m = items.getString(item + ".material");
		    //plugin.getLogger().info("Material = " + m);
		    Material material = Material.matchMaterial(m);
		    int quantity = items.getInt(item + ".quantity", 0);
		    String extra = items.getString(item + ".extra", "");
		    double price = items.getDouble(item + ".price");
		    String description = items.getString(item + ".description");
		    MiniShopItem shopItem = new MiniShopItem(material,extra,slot,description,quantity,price);
		    store.put(slot, shopItem);
		    miniShop.setItem(slot, shopItem.getItem());
		    slot++;
		    if (slot > 8) {
			break;
		    }
		} catch (Exception e) {
		    plugin.getLogger().warning("Problem loading minishop item #" + slot);
		    plugin.getLogger().warning(e.getMessage());
		    e.printStackTrace();
		}
	    }

	}	
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
	Player player = (Player) event.getWhoClicked(); // The player that clicked the item
	ItemStack clicked = event.getCurrentItem(); // The item that was clicked
	Inventory inventory = event.getInventory(); // The inventory that was clicked in
	//AcidIsland plugin = AcidIsland.getPlugin();
	int slot = event.getSlot();
	if (inventory.getName().equals(miniShop.getName())) { // The inventory is our custom Inventory
	    String message = "";
	    //plugin.getLogger().info("You clicked on slot " + slot);
	    event.setCancelled(true); // Don't let them pick it up
	    if (store.containsKey(slot)) {
		// We have a winner!
		MiniShopItem item = store.get(slot);
		if (clicked.equals(item.getItem())) {
		    // Check they can afford it
		    if (!VaultHelper.econ.has(player, item.getPrice())) {
			message = "You cannot afford that item!";
		    } else {
			EconomyResponse r = VaultHelper.econ.withdrawPlayer(player, item.getPrice());
			if (r.transactionSuccess()) {
			    message = "You bought " + item.getQuantity() + " " + item.getDescription() + " for " + VaultHelper.econ.format(item.getPrice());			
			    player.getInventory().addItem(item.getItemClean());
			} else {
			    message = "There was a problem puchasing that item: " + r.errorMessage;
			}
		    }
		    player.closeInventory(); // Closes the inventory
		    player.sendMessage(message);		
		}
	    }
	}
    }
}
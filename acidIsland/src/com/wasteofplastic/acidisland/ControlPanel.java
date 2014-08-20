package com.wasteofplastic.acidisland;

import java.util.HashMap;
import java.util.List;

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
    private static YamlConfiguration cpFile;
    private AcidIsland plugin;



    /**
     * @param plugin
     */
    public ControlPanel(AcidIsland plugin) {
	this.plugin = plugin;
	loadShop();
	loadControlPanel();
    }


    /**
     * Map of panel contents by name
     */
    private static HashMap<String, HashMap<Integer,CPItem>> panels = new HashMap<String, HashMap<Integer,CPItem>>();
    //public static final Inventory challenges = Bukkit.createInventory(null, 9, ChatColor.YELLOW + "Challenges");

    /**
     * Map of CP inventories by name
     */
    public static HashMap<String,Inventory> controlPanel = new HashMap<String,Inventory>();

    public static final Inventory miniShop = Bukkit.createInventory(null, 9, ChatColor.YELLOW + Locale.islandMiniShopTitle);
    // The first parameter, is the inventory owner. I make it null to let everyone use it.
    //The second parameter, is the slots in a inventory. Must be a multiple of 9. Can be up to 54.
    //The third parameter, is the inventory name. This will accept chat colors.


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

    private static void loadControlPanel() {
	AcidIsland plugin = AcidIsland.getPlugin();
	// Map of known panel contents by name
	panels.clear();
	// Map of panel inventories by name
	controlPanel.clear();


	cpFile = AcidIsland.loadYamlFile("controlpanel.yml");
	ConfigurationSection controlPanels = cpFile.getRoot();
	if (controlPanels == null) {
	    plugin.getLogger().severe("Controlpanel.yml is corrupted! Delete so it can be regenerated or fix!");
	    return;
	}	
	// Go through the yml file and create inventories and panel maps
	for (String panel : controlPanels.getKeys(false)) {
	    ConfigurationSection panelConf = cpFile.getConfigurationSection(panel);
	    // New panel map
	    HashMap<Integer,CPItem> cp = new HashMap<Integer,CPItem>();
	    String panelName = panelConf.getString("panelname", "Commands");
	    //plugin.getLogger().info("DEBUG: Panel section " + panelName);
	    // New inventory
	    Inventory newPanel = Bukkit.createInventory(null, 9, panelName);
	    if (newPanel == null) {
		plugin.getLogger().info("DEBUG: new panel is null!");
	    }
	    // Add inventory to map of inventories
	    controlPanel.put(newPanel.getName(),newPanel);
	    ConfigurationSection buttons = cpFile.getConfigurationSection(panel + ".buttons");
	    if (buttons != null) {
		// Run through buttons
		int slot = 0;
		for (String item : buttons.getKeys(false)) {
		    try {
			String m = buttons.getString(item + ".material","AIR");
			//plugin.getLogger().info("Material = " + m);
			Material material = Material.matchMaterial(m);
			String description = buttons.getString(item + ".description","");
			String command = buttons.getString(item + ".command","");
			String nextSection = buttons.getString(item + ".nextSection","");
			CPItem cpItem = new CPItem(material,description,command,nextSection);
			cp.put(slot, cpItem);
			newPanel.setItem(slot, cpItem.getItem());
			slot++;
			if (slot > 8) {
			    break;
			}
		    } catch (Exception e) {
			plugin.getLogger().warning("Problem loading control panel " + panel + " item #" + slot);
			plugin.getLogger().warning(e.getMessage());
			e.printStackTrace();
		    }
		}
		// Add overall control panel
		panels.put(panelName, cp);
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
	// Check control panels
	for (String panelName : controlPanel.keySet()) {
	    if (inventory.getName().equals(panelName)) {
		//plugin.getLogger().info("DEBUG: panels length " + panels.size());
		//plugin.getLogger().info("DEBUG: panel name " + panelName);
		if (slot == -999) {
		    player.closeInventory();
		    event.setCancelled(true);
		    return;
		}
		HashMap<Integer, CPItem> thisPanel = panels.get(panelName);
		if (slot >= 0 && slot < thisPanel.size()) {
		    //plugin.getLogger().info("DEBUG: slot is " + slot);
		    // Do something
		    String command = thisPanel.get(slot).getCommand();
		    String nextSection = thisPanel.get(slot).getNextSection();
		    if (!command.isEmpty()) {
			player.closeInventory(); // Closes the inventory
			event.setCancelled(true);
			//plugin.getLogger().info("DEBUG: performing command " + command);
			player.performCommand(command);
			return;
		    }
		    if (!nextSection.isEmpty()) {
			player.closeInventory(); // Closes the inventory
			Inventory next = controlPanel.get(nextSection);
			//plugin.getLogger().info("DEBUG: opening next cp "+nextSection);
			player.openInventory(next);
			event.setCancelled(true);
			return;
		    }
		}
	    }
	}
	if (inventory.getName().equals(Locale.challengesguiTitle)) {
	    event.setCancelled(true);
	    if (slot == -999) {
		player.closeInventory();
		return;
	    }

	    // Get the list of items in this inventory
	    //plugin.getLogger().info("You clicked on slot " + slot);
	    List<CPItem> challenges = plugin.challenges.getCP(player);
	    if (slot >=0 && slot < challenges.size()) {
		CPItem item = challenges.get(slot);
		// Check that it is the top items that are bing clicked on
		if (clicked.equals(item.getItem())) {
		    //plugin.getLogger().info("You clicked on a challenge item");
		    //plugin.getLogger().info("performing  /" + item.getCommand());
		    if (item.getCommand() != null) {
			player.performCommand(item.getCommand());
			player.closeInventory();
		    }
		}
	    }
	}
	if (inventory.getName().equals(miniShop.getName())) { // The inventory is our custom Inventory
	    String message = "";
	    //plugin.getLogger().info("You clicked on slot " + slot);
	    event.setCancelled(true); // Don't let them pick it up
	    if (slot == -999) {
		player.closeInventory();
		return;
	    }
	    if (store.containsKey(slot)) {
		// We have a winner!
		MiniShopItem item = store.get(slot);
		if (clicked.equals(item.getItem())) { 
		    // Check they can afford it
		    if (!VaultHelper.econ.has(player, player.getWorld().getName(), item.getPrice())) {
			message = "You cannot afford that item!";
		    } else {
			EconomyResponse r = VaultHelper.econ.withdrawPlayer(player, player.getWorld().getName(), item.getPrice());
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
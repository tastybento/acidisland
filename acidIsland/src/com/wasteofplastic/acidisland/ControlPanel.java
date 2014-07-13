package com.wasteofplastic.acidisland;

import java.util.ArrayList;
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
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author ben
 * Provides a handy control panel and minishop
 */
public class ControlPanel implements Listener {

    private static YamlConfiguration miniShopFile;
    private static HashMap<Integer, MiniShopItem> store = new HashMap<Integer,MiniShopItem>();

    public static final Inventory challenges = Bukkit.createInventory(null, 9, ChatColor.YELLOW + "Challenges");


    public static final Inventory miniShop = Bukkit.createInventory(null, 9, ChatColor.YELLOW + "MiniShop");
    // The first parameter, is the inventory owner. I make it null to let everyone use it.
    //The second parameter, is the slots in a inventory. Must be a multiple of 9. Can be up to 54.
    //The third parameter, is the inventory name. This will accept chat colors.

    static {
	//The first parameter is the Material, then the durability (if wanted), slot, descriptions
	// Minishop
	miniShopFile = AcidIsland.loadYamlFile("minishop.yml");
	ConfigurationSection items = miniShopFile.getConfigurationSection("items");
	AcidIsland plugin = AcidIsland.getPlugin();
	if (items != null) {
	    // Run through items
	    int slot = 0;
	    for (String item : items.getKeys(false)) {
		try {
		    String m = items.getString(item + ".material");
		    plugin.getLogger().info("Material = " + m);
		    Material material = Material.matchMaterial(m);
		    int quantity = items.getInt(item + ".quantity", 0);
		    int durability = items.getInt(item + ".durability", 0);
		    double price = items.getDouble(item + ".price");
		    String description = items.getString(item + ".description");
		    AcidIsland.getPlugin().getLogger().info("Loading minishop item #" + slot + ":" + description);
		    store.put(slot, new MiniShopItem(material,durability,slot,description,quantity,price));
		    createDisplay(material,durability,miniShop,slot,description,quantity + ":" + VaultHelper.econ.format(price));
		    slot++;
		} catch (Exception e) {
		    AcidIsland.getPlugin().getLogger().warning("Problem loading minishop item #" + slot);
		    AcidIsland.getPlugin().getLogger().warning(e.getMessage());
		    e.printStackTrace();
		}
	    }

	}
	//createDisplay(Material.LAVA_BUCKET, miniShop, 0, "Buy Lava Bucket", "$100");
	//createDisplay(Material.DIRT,1, miniShop, 1, "Buy Dirt", "$10 a block");
	//createDisplay(Material.IRON_INGOT, miniShop, 2, "Buy Iron", "$50");
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
	Player player = (Player) event.getWhoClicked(); // The player that clicked the item
	ItemStack clicked = event.getCurrentItem(); // The item that was clicked
	Inventory inventory = event.getInventory(); // The inventory that was clicked in
	AcidIsland plugin = AcidIsland.getPlugin();
	int slot = event.getSlot();
	if (inventory.getName().equals(miniShop.getName())) { // The inventory is our custom Inventory
	    String message = "";
	    plugin.getLogger().info("You clicked on slot " + slot);
	    event.setCancelled(true); // Don't let them pick it up
	    if (store.containsKey(slot)) {
		// We have a winner!
		MiniShopItem item = store.get(slot);
		if (clicked.getType().equals(item.getMaterial()) && (clicked.getDurability() == item.getDurability())) {

		    // Check they can afford it
		    if (!VaultHelper.econ.has(player, item.getPrice())) {
			message = "You cannot afford that item!";
		    } else {
			EconomyResponse r = VaultHelper.econ.withdrawPlayer(player, item.getPrice());
			if (r.transactionSuccess()) {
			    message = "You bought " + item.getQuantity() + " " + item.getDescription() + " for " + VaultHelper.econ.format(item.getPrice());
			    ItemStack purchased = new ItemStack(item.getMaterial(),item.getQuantity());
			    if (item.getDurability() > 0) {
				purchased.setDurability((short) item.getDurability());
			    }
			    player.getInventory().addItem(purchased);
			} else {
			    message = "There was a problem puchasing that item: " + r.errorMessage;
			}
		    }
		    player.closeInventory(); // Closes the inventory
		    player.sendMessage(message);		
		}
	    }
	    /*
	    if (clicked.getType() == Material.DIRT && clicked.getDurability() == 3) { // The item that the player clicked it dirt
		event.setCancelled(true); // Make it so the dirt is back in its original spot
		player.closeInventory(); // Closes there inventory
		player.getInventory().addItem(new ItemStack(Material.DIRT, 1)); // Adds dirt
		player.sendMessage("You bought dirt!");
	    }*/
	}
    }

    public static void createDisplay(Material material, Inventory inv, int Slot, String name, String lore) {
	createDisplay(material, 0, inv, Slot, name, lore);
    }

    public static void createDisplay(Material material, int durability, Inventory inv, int Slot, String name, String lore) {
	ItemStack item = new ItemStack(material);
	item.setDurability((short)durability);
	ItemMeta meta = item.getItemMeta();
	meta.setDisplayName(name);
	ArrayList<String> Lore = new ArrayList<String>();
	Lore.add(lore);
	meta.setLore(Lore);
	item.setItemMeta(meta);

	inv.setItem(Slot, item); 

    }
}
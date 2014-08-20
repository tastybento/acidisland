package com.wasteofplastic.acidisland;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

/**
 * Handles challenge commands and related methods
 */
public class Challenges implements CommandExecutor {
    private AcidIsland plugin;
    // Database of challenges
    private LinkedHashMap<String, List<String>> challengeList = new LinkedHashMap<String, List<String>>();
    private PlayerCache players;
    private HashMap<UUID, List<CPItem>> playerChallengeGUI = new HashMap<UUID, List<CPItem>>();

    public Challenges(AcidIsland acidIsland, PlayerCache players) {
	this.plugin = acidIsland;
	this.players = players;
	populateChallengeList();
    }

    /* (non-Javadoc)
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] cmd) {
	if (!(sender instanceof Player)) {
	    return false;
	}
	final Player player = (Player)sender;
	if (!player.getWorld().getName().equalsIgnoreCase(Settings.worldName)) {
	    return true;
	}
	switch (cmd.length) {
	case 0:
	    // User typed /c or /challenge
	    // Display panel
	    player.openInventory(challengePanel(player));
	    /*
	    int levelDone = 0;
	    sender.sendMessage(ChatColor.GOLD + Settings.challengeLevels.get(0) + ": " + getChallengesByLevel(player, Settings.challengeLevels.get(0)));
	    for (int i = 1; i < Settings.challengeLevels.size(); i++) {
		levelDone = checkLevelCompletion(player, Settings.challengeLevels.get(i-1));
		if (levelDone <= 0) {
		    sender.sendMessage(ChatColor.GOLD + Settings.challengeLevels.get(i) + ": "
			    + getChallengesByLevel(player, Settings.challengeLevels.get(i)));
		} else {
		    sender.sendMessage(ChatColor.GOLD + Settings.challengeLevels.get(i) + ChatColor.GRAY + ": "
			    + Locale.challengestoComplete.replace("[challengesToDo]", String.valueOf(levelDone)).replace("[thisLevel]",Settings.challengeLevels.get(i-1)));
		}
	    }
	    sender.sendMessage(ChatColor.GOLD + Locale.challengeshelp1);
	    sender.sendMessage(ChatColor.GOLD + Locale.challengeshelp2);
	     */
	    return true;
	case 1:
	    if (cmd[0].equalsIgnoreCase("help") || cmd[0].equalsIgnoreCase("complete") || cmd[0].equalsIgnoreCase("c")) {
		sender.sendMessage(ChatColor.GOLD + Locale.challengeshelp1);
		sender.sendMessage(ChatColor.GOLD + Locale.challengeshelp2);
		sender.sendMessage(ChatColor.GOLD + Locale.challengescolors);
		sender.sendMessage(ChatColor.GREEN + Locale.challengesincomplete + ChatColor.DARK_GREEN + Locale.challengescompleteNotRepeatable
			+ ChatColor.AQUA + Locale.challengescompleteRepeatable);
	    } else if (isLevelAvailable(player, plugin.getChallengeConfig().getString("challenges.challengeList." + cmd[0].toLowerCase() + ".level"))) {
		// Provide info on the challenge
		// Challenge Name
		// Description
		// Type
		// Items taken or not
		// island or not
		final String challenge = cmd[0].toLowerCase();
		sender.sendMessage(ChatColor.GOLD + Locale.challengesname + ": " + ChatColor.WHITE + challenge);
		sender.sendMessage(ChatColor.WHITE + Locale.challengeslevel +": " + ChatColor.GOLD + plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".level",""));
		sender.sendMessage(ChatColor.GOLD + plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".description",""));
		final String type = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".type","").toLowerCase();
		if (type.equals("inventory")) {
		    if (plugin.getChallengeConfig().getBoolean("challenges.challengeList." + cmd[0].toLowerCase() + ".takeItems")) {
			sender.sendMessage(ChatColor.RED + Locale.challengesitemTakeWarning);
		    }
		} else if (type.equals("island")) {
		    sender.sendMessage(ChatColor.RED + Locale.challengeserrorItemsNotThere);
		}
		if (players.checkChallenge(player.getUniqueId(),challenge)
			&& (!type.equals("inventory") || !plugin.getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".repeatable", false))) {
		    sender.sendMessage(ChatColor.RED + Locale.challengesnotRepeatable);
		    return true;
		}
		int moneyReward = 0;
		int expReward = 0;
		String rewardText = "";

		if (!players.checkChallenge(player.getUniqueId(),challenge)) {
		    // First time
		    moneyReward = plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge.toLowerCase() + ".moneyReward", 0);
		    rewardText = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".rewardText", "Goodies!").replace('&', '§');
		    expReward = plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge + ".xpReward", 0);
		    sender.sendMessage(ChatColor.GOLD + Locale.challengesfirstTimeRewards);
		} else {
		    // Repeat challenge
		    moneyReward = plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge.toLowerCase() + ".repeatMoneyReward", 0);
		    rewardText = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".repeatRewardText", "Goodies!").replace('&', '§');
		    expReward = plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge + ".repeatExpReward", 0);
		    sender.sendMessage(ChatColor.GOLD + Locale.challengesrepeatRewards);

		}	
		sender.sendMessage(ChatColor.WHITE + rewardText);
		if (expReward > 0) {
		    sender.sendMessage(ChatColor.GOLD + Locale.challengesexpReward + ": " + ChatColor.WHITE + expReward);
		}
		if (moneyReward > 0) { 
		    sender.sendMessage(ChatColor.GOLD + Locale.challengesmoneyReward + ": " + ChatColor.WHITE + VaultHelper.econ.format(moneyReward));
		}
		sender.sendMessage(ChatColor.GOLD + Locale.challengestoCompleteUse + ChatColor.WHITE + " /" + label + " c " + challenge);
	    } else {
		sender.sendMessage(ChatColor.RED + Locale.challengesinvalidChallengeName);
	    }
	    return true;
	case 2:
	    if (cmd[0].equalsIgnoreCase("complete") || cmd[0].equalsIgnoreCase("c")) {
		if (checkIfCanCompleteChallenge(player, cmd[1].toLowerCase())) {
		    giveReward(player, cmd[1].toLowerCase());
		}
		return true;
	    }
	default:
	    return false;
	}
    }

    /**
     * Gives the reward for completing the challenge
     * Uses the same format as uSkyblock config.yml
     * @param player
     * @param challenge
     * @return
     */
    private boolean giveReward(final Player player, final String challenge) {
	// Grab the rewards from the config.yml file
	String[] permList;
	String[] itemRewards;
	int moneyReward = 0;
	int expReward = 0;
	String rewardText = "";

	// Gather the rewards due
	// If player has done a challenge already, the rewards are different
	if (!players.checkChallenge(player.getUniqueId(),challenge)) {
	    // First time
	    player.sendMessage(ChatColor.GREEN + Locale.challengesyouHaveCompleted.replace("[challenge]", challenge));
	    plugin.getServer().broadcastMessage(ChatColor.GOLD + Locale.challengesnameHasCompleted.replace("[name]", player.getName()).replace("[challenge]", challenge ));
	    plugin.tellOfflineTeam(player.getUniqueId(), ChatColor.GOLD + Locale.challengesnameHasCompleted.replace("[name]", player.getName()).replace("[challenge]", challenge ));
	    itemRewards = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".itemReward", "").split(" ");
	    moneyReward = plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge.toLowerCase() + ".moneyReward", 0);
	    rewardText = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".rewardText", "Goodies!").replace('&', '§');
	    expReward = plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge + ".expReward", 0);
	} else {
	    // Repeat challenge
	    player.sendMessage(ChatColor.GREEN + Locale.challengesyouRepeated.replace("[challenge]", challenge));
	    itemRewards = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".repeatItemReward", "").split(" ");
	    moneyReward = plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge.toLowerCase() + ".repeatMoneyReward", 0);
	    rewardText = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".repeatRewardText", "Goodies!").replace('&', '§');
	    expReward = plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge + ".repeatExpReward", 0);	    
	}	
	// Report the rewards and give out exp, money and permissions if appropriate
	player.sendMessage(ChatColor.GOLD + Locale.challengesrewards + ": " + ChatColor.WHITE +  rewardText);
	if (expReward > 0) {
	    player.sendMessage(ChatColor.GOLD + Locale.challengesexpReward + ": " + ChatColor.WHITE + expReward);
	    player.giveExp(expReward);
	}
	if (moneyReward > 0 && (VaultHelper.econ != null)) {
	    EconomyResponse e = VaultHelper.econ.depositPlayer(player, Settings.worldName, moneyReward);
	    if (e.transactionSuccess()) {
		player.sendMessage(ChatColor.GOLD + Locale.challengesmoneyReward + ": " + ChatColor.WHITE + VaultHelper.econ.format(moneyReward));
	    } else {
		plugin.getLogger().severe("Error giving player " + player.getUniqueId() + " challenge money:" + e.errorMessage);
		plugin.getLogger().severe("Reward was $" + moneyReward);
	    }
	}
	// Dole out permissions
	permList = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".permissionReward", "").split(" ");
	for (final String s : permList) {
	    if (!s.isEmpty()) {
		if (!VaultHelper.checkPerm(player, s)) {
		    VaultHelper.addPerm(player, s);
		    plugin.getLogger().info("Added permission " + s + " to " + player.getName() + "");
		}
	    }
	}
	// Give items
	Material rewardItem;
	int rewardQty;
	// Build the item stack of rewards to give the player
	for (final String s : itemRewards) {
	    final String[] element = s.split(":");
	    if (element.length == 2) {
		try {
		    rewardItem = Material.getMaterial(element[0]);
		    rewardQty = Integer.parseInt(element[1]);
		    final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(new ItemStack[] { new ItemStack(rewardItem, rewardQty) });
		    if (!leftOvers.isEmpty()) {
			player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
		    }
		    player.getWorld().playSound(player.getLocation(), Sound.ITEM_PICKUP, 1F, 1F);
		} catch (Exception e) {
		    plugin.getLogger().severe("Could not give " + element + " to "+ player.getUniqueId() + " for challenge reward!");
		}
	    } else if (element.length == 3) {
		try {
		    rewardItem = Material.getMaterial(element[0]);
		    rewardQty = Integer.parseInt(element[2]);
		    // Check for POTION
		    if (rewardItem.equals(Material.POTION)) {
			// Add the effect of the potion
			final PotionEffectType potionType = PotionEffectType.getByName(element[1]);
			if (potionType == null) {
			    plugin.getLogger().severe("Reward potion effect type in config.yml challenges is unknown - skipping!");
			} else {
			    final Potion rewPotion = new Potion(PotionType.getByEffect(potionType));
			    final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(new ItemStack[] { rewPotion.toItemStack(rewardQty) });
			    if (!leftOvers.isEmpty()) {
				player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
			    }
			}
		    } else {
			// Normal item, not a potion
			int rewMod = Integer.parseInt(element[1]);
			final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(new ItemStack[] { new ItemStack(rewardItem, rewardQty, (short) rewMod) });
			if (!leftOvers.isEmpty()) {
			    player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
			}
		    }
		    player.getWorld().playSound(player.getLocation(), Sound.ITEM_PICKUP, 1F, 1F);
		} catch (Exception e) {
		    plugin.getLogger().severe("Could not give " + element + " to "+ player.getUniqueId() + " for challenge reward!");
		}
	    }
	}
	// Mark the challenge as complete
	if (!players.checkChallenge(player.getUniqueId(),challenge)) {
	    players.completeChallenge(player.getUniqueId(),challenge);
	}	
	return true;
    }


    /**
     * Returns a color formatted string for all the challenges of a particular level for a player
     * Repeatable challenges are AQUA, completed are Dark Green and yet to be done are green.
     * @param player
     * @param level
     * @return string of challenges
     */
    private String getChallengesByLevel(final Player player, final String level) {
	List<String> levelChallengeList = challengeList.get(level);
	String response = "";
	for (String challenge : levelChallengeList) {
	    if (players.checkChallenge(player.getUniqueId(), challenge)) {
		if (plugin.getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".repeatable",false)) {
		    response += ChatColor.AQUA + challenge + ", ";
		} else {
		    response += ChatColor.DARK_GREEN + challenge + ", ";
		}
	    } else {
		response += ChatColor.GREEN + challenge + ", ";

	    }
	}
	// Trim the final dash
	if (response.length() > 3) {
	    response = response.substring(0, response.length() - 2);
	}
	return response;
    }

    /**
     * Returns the number of challenges that must still be completed to finish a level
     * Based on how many challenges there are in a level, how many have been done and 
     * how many are okay to leave undone.
     * @param player
     * @param level
     * @return
     */
    public int checkLevelCompletion(final Player player, final String level) {
	int challengesCompleted = 0;
	List<String> levelChallengeList = challengeList.get(level);
	for (String challenge : levelChallengeList) {
	    if (players.checkChallenge(player.getUniqueId(), challenge)) {
		challengesCompleted++;
	    }
	}
	return levelChallengeList.size() - Settings.waiverAmount - challengesCompleted;
    }

    /**
     * Checks if player can complete challenge
     * @param player
     * @param challenge
     * @return true if player can complete otherwise false
     */
    public boolean checkIfCanCompleteChallenge(final Player player, final String challenge) {
	if (!isLevelAvailable(player, plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".level"))) {
	    player.sendMessage(ChatColor.RED + Locale.challengesyouHaveNotUnlocked);
	    return false;
	}
	if (!players.challengeExists(player.getUniqueId(),challenge)) {
	    player.sendMessage(ChatColor.RED + Locale.challengesunknownChallenge);
	    return false;
	}
	if (players.checkChallenge(player.getUniqueId(),challenge)
		&& !plugin.getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".repeatable")) {
	    player.sendMessage(ChatColor.RED + Locale.challengesnotRepeatable);
	    return false;
	}
	if (players.checkChallenge(player.getUniqueId(),challenge)
		&& plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".type").equalsIgnoreCase("island")) {
	    player.sendMessage(ChatColor.RED + Locale.challengesnotRepeatable);
	    return false;
	}
	if (plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".type").equalsIgnoreCase("inventory")) {
	    if (!hasRequired(player, challenge, "inventory")) {
		player.sendMessage(ChatColor.RED + Locale.challengeserrorNotEnoughItems);
		player.sendMessage(ChatColor.RED + plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".description"));
		return false;
	    }
	    return true;
	}
	if (plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".type").equalsIgnoreCase("island")) {
	    if (!plugin.playerIsOnIsland(player)) {
		player.sendMessage(ChatColor.RED + Locale.challengeserrorNotOnIsland);
	    }
	    if (!hasRequired(player, challenge, "island")) {
		player.sendMessage(ChatColor.RED + Locale.challengeserrorNotCloseEnough);
		player.sendMessage(ChatColor.RED
			+ plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".description"));
		return false;
	    }
	    return true;
	}
	if (plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".type").equalsIgnoreCase("level")) {
	    if (players.getIslandLevel(player.getUniqueId()) >= plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge + ".requiredItems")) {
		return true;
	    }

	    player.sendMessage(ChatColor.RED + Locale.challengeserrorIslandLevel.replace("[level]",
		    String.valueOf(plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge + ".requiredItems"))));
	    return false;
	}
	return false;
    }
    /**
     * Goes through all the challenges in the config.yml file and puts them into
     * the challenges list
     */
    public void populateChallengeList() {
	for (String s : Settings.challengeList) {
	    String level = plugin.getChallengeConfig().getString("challenges.challengeList." + s + ".level","");
	    // Verify that this challenge's level is in the list of levels
	    if (Settings.challengeLevels.contains(level)) {	
		if (challengeList.containsKey(level)) {
		    challengeList.get(level).add(s);
		} else {
		    List<String> t = new ArrayList<String>();
		    t.add(s);
		    challengeList.put(level, t);		    
		}
	    } else {
		plugin.getServer().getLogger().severe("Level ("+ level + ") for challenge " + s + " does not exist. Check challenges.yml.");
	    }
	}
    }

    /**
     * Checks if a player has enough for a challenge. Supports two types of checks, inventory and island
     * @param player
     * @param challenge
     * @param type
     * @return true if the player has everything required
     */
    @SuppressWarnings("deprecation")
    public boolean hasRequired(final Player player, final String challenge, final String type) {
	final String[] reqList = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".requiredItems").split(" ");
	// The format of the requiredItems is as follows:
	// Material:Qty
	// or
	// Material:DamageModifier:Qty
	// This second one is so that items such as potions or variations on standard items can be collected
	if (type.equalsIgnoreCase("inventory")) {
	    List<ItemStack> toBeRemoved = new ArrayList<ItemStack>();
	    Material reqItem;
	    int reqAmount = 0;
	    for (final String s : reqList) {
		final String[] part = s.split(":");
		if (part.length == 2) {
		    try {
			
			reqItem = Material.getMaterial(part[0]);
			reqAmount = Integer.parseInt(part[1]);
			ItemStack item = new ItemStack(reqItem);
			//plugin.getLogger().info("DEBUG: required item = " + reqItem.toString());
			//plugin.getLogger().info("DEBUG: item amount = " + reqAmount);
			
			if (!player.getInventory().contains(reqItem)) {
			    return false;
			} else {
			    // check amount
			    int amount = 0;
			    for (ItemStack i : player.getInventory().all(reqItem).values()) {
				// #1 item stack qty + amount is less than required items - take all i
				// #2 item stack qty + amount = required item - take all
				// #3 item stack qty + amount > req items - take portion of i
				amount += i.getAmount();
				if (amount <= reqAmount) {
				    // Remove all of this item stack - clone otherwise it will keep a reference to the original
				    toBeRemoved.add(i.clone());
				    //plugin.getLogger().info("DEBUG: amount is <= req Remove " + i.toString() + ":" + i.getDurability() + " x " + i.getAmount());

				} else {
				    // Remove a portion of this item
				    //plugin.getLogger().info("DEBUG: amount is > req Remove " + i.toString() + ":" + i.getDurability() + " x " + i.getAmount());

				    item.setAmount(reqAmount);
				    item.setDurability(i.getDurability());
				    toBeRemoved.add(item);  
				}
			    }
			    //plugin.getLogger().info("DEBUG: amount "+ amount);
			    if (amount < reqAmount) {
				return false;
			    }
			}
			
			/*
			if (!player.getInventory().containsAtLeast(item,reqAmount)){
			    // MAP is a special case - the durability increments with every one
			    plugin.getLogger().info("DEBUG: not enough in inventory");
			    
			    for (ItemStack i : player.getInventory().getContents()) {
				if (i != null) {
				    plugin.getLogger().info("DEBUG: material "+ i.getType());
				    plugin.getLogger().info("DEBUG: amount "+ i.getAmount());
				    plugin.getLogger().info("DEBUG: durability "+ i.getDurability());
				}
			    }
			    return false;
			}*/
			//item.setAmount(reqAmount);
			//toBeRemoved.add(item);
		    } catch (Exception e) {
			plugin.getLogger().severe("Problem with " + s + " in challenges.yml!");
			return false;
		    }
		} else if (part.length == 3) {
		    try {
			reqItem = Material.getMaterial(part[0]);
			int reqDurability = Integer.parseInt(part[1]);
			reqAmount = Integer.parseInt(part[2]);
			int count = reqAmount;
			//plugin.getLogger().info("DEBUG: 3 part " + reqItem.toString() + ":" + reqDurability + " x " + reqAmount);
			ItemStack item = new ItemStack(reqItem);
			// Check for potions
			if (reqItem.equals(Material.POTION)) {
			    // Contains at least does not work for potions
			    ItemStack[] playerInv = player.getInventory().getContents();
			    for (ItemStack i : playerInv) {
				if (i != null && i.getType().equals(Material.POTION)) {
				    //plugin.getLogger().info("Potion found, durability = "+ i.getDurability());
				    if (i.getDurability() == reqDurability) {
					//plugin.getLogger().info("Matched! ");
					count--;
					item = new ItemStack(i);
					//plugin.getLogger().info("DEBUG: Found " + item.toString() + ":" + item.getDurability() + " x " + item.getAmount());
				    }
				}
			    }
			    if (count >0) {
				return false;
			    }
			    // They have enough
			} else {
			    item.setDurability((short)reqDurability);
			    //item.setAmount(reqAmount);
			    if (!player.getInventory().containsAtLeast(item, reqAmount)) {
				return false;
			    }
			}
			//plugin.getLogger().info("DEBUG: before set amount " + item.toString() + ":" + item.getDurability() + " x " + item.getAmount());
			item.setAmount(reqAmount);
			//plugin.getLogger().info("DEBUG: after set amount " + item.toString() + ":" + item.getDurability() + " x " + item.getAmount());
			toBeRemoved.add(item);
		    } catch (Exception e) {
			plugin.getLogger().severe("Problem with " + s + " in challenges.yml!");
			if (part[0].equalsIgnoreCase("POTION")) {
			    plugin.getLogger().severe("Format POTION:TYPE:QTY where TYPE is the number of the following:");
			    for (PotionType p : PotionType.values()) {
				plugin.getLogger().info(p.toString() + ":" + p.getDamageValue());
			    }
			}
			e.printStackTrace();
			return false;
		    }
		}
	    }
	    // TODO: Build up the items in the inventory and remove them if they are all there.
	    // REPLACE THIS FUNCTION BELOW
	    if (plugin.getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".takeItems")) {
		//checkChallengeItems(player, challenge);
		int qty = 0;
		for (ItemStack i : toBeRemoved) {
		    qty += i.getAmount();
		    //plugin.getLogger().info("DEBUG: Remove " + i.toString() + ":" + i.getDurability() + " x " + i.getAmount());
		    player.getInventory().removeItem(i);
		}
		//plugin.getLogger().info("DEBUG: total = " + qty);
	    }
	    return true;
	}
	if (type.equalsIgnoreCase("island")) {
	    final HashMap<Material, Integer> neededItem = new HashMap<Material, Integer>();
	    for (int i = 0; i < reqList.length; i++) {
		final String[] sPart = reqList[i].split(":");
		plugin.getLogger().info("Needed item is " + Integer.parseInt(sPart[1]) + " x " + Material.getMaterial(sPart[0]).toString());
	    }
	    final Location l = player.getLocation();
	    final int px = l.getBlockX();
	    final int py = l.getBlockY();
	    final int pz = l.getBlockZ();
	    for (int x = -10; x <= 10; x++) {
		for (int y = -3; y <= 10; y++) {
		    for (int z = -10; z <= 10; z++) {
			final Material b = new Location(l.getWorld(), px + x, py + y, pz + z).getBlock().getType();
			if (neededItem.containsKey(b)) {
			    if (neededItem.get(b) == 1) {
				neededItem.remove(b);
			    } else {
				// Reduce the require amount by 1
				neededItem.put(b, neededItem.get(b) - 1);
			    }
			}
		    }
		}
	    }
	    // Check if all the needed items have been amassed
	    if (neededItem.isEmpty()) {
		// getLogger().info("All done!");
		return true;
	    } else {
		return false;
	    }
	}

	return true;
    }

    /**
     * Checks if a player has the required items and removes them if they meet the challenge requirements
     * @param player
     * @param challenge
     * @param type
     * @return true if the challenge is successful, false if not
     */
    /*
    public boolean checkChallengeItems(final Player player, final String challenge) {
	final String type = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".type");
	if (type.equalsIgnoreCase("inventory")) {
	    final String[] reqList = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".requiredItems").split(" ");

	    Material reqItem;
	    int reqAmount = 0;
	    int reqMod = -1;
	    for (final String required : reqList) {
		final String[] item = required.split(":");
		if (item.length == 2) {
		    reqItem = Material.getMaterial(item[0]);
		    reqAmount = Integer.parseInt(item[1]);
		    if (!player.getInventory().contains(reqItem, reqAmount)) {
			return false;
		    }

		    player.getInventory().removeItem(new ItemStack[] { new ItemStack(reqItem, reqAmount) });
		} else if (item.length == 3) {
		    reqItem = Material.getMaterial(item[0]);
		    reqAmount = Integer.parseInt(item[2]);
		    reqMod = Integer.parseInt(item[1]);
		    if (!player.getInventory().containsAtLeast(new ItemStack(reqItem, reqAmount, (short) reqMod), reqAmount)) {
			return false;
		    }
		    player.getInventory().removeItem(new ItemStack[] { new ItemStack(reqItem, reqAmount, (short) reqMod) });
		}
	    }
	    return true;
	}
	return false;
    }
     */
    public boolean isLevelAvailable(final Player player, final String level) {
	if (challengeList.size() < 2) {
	    return true;
	}
	for (int i = 0; i < Settings.challengeLevels.size(); i++) {
	    if (Settings.challengeLevels.get(i).equalsIgnoreCase(level)) {
		if (i == 0) {
		    return true;
		}

		if (checkLevelCompletion(player, Settings.challengeLevels.get(i-1)) <= 0) {
		    return true;
		}
	    }

	}

	return false;
    }

    public Inventory challengePanel(Player player) {
	// Create the challenges control panel
	// New panel map
	List<CPItem> cp = new ArrayList<CPItem>();
	int levelDone = 0;
	//sender.sendMessage(ChatColor.GOLD + Settings.challengeLevels.get(0) + ": " + getChallengesByLevel(player, Settings.challengeLevels.get(0)));
	// Loop through challenges for this player for first level
	for (String challengeName : challengeList.get(Settings.challengeLevels.get(0))) {
	    // Get the icon
	    ItemStack icon = null;
	    String iconName = plugin.getChallengeConfig().getString("challenges.challengeList." + challengeName + ".icon", "");
	    if (!iconName.isEmpty()) {
		try {
		    // Split if required
		    String[] split = iconName.split(":");
		    if (split.length == 1) {
			icon = new ItemStack(Material.valueOf(iconName));
		    } else if (split.length == 2) {
			icon = new ItemStack(Material.valueOf(split[0]));
			icon.setDurability(Integer.valueOf(split[1]).shortValue());
		    }
		} catch (Exception e) {
		    // Icon was not well formatted
		    plugin.getLogger().warning("Error in challenges.yml - icon format is incorrect for " + challengeName + ":" + iconName);
		    plugin.getLogger().warning("Format should be 'icon: MaterialType:Damage' where Damage is optional");
		}
	    }
	    if (icon == null) {
		icon = new ItemStack(Material.PAPER);
	    }
	    CPItem item = new CPItem(icon, ChatColor.GREEN + challengeName.substring(0, 1).toUpperCase() + challengeName.substring(1), "aichallenge c " + challengeName, null);
	    List<String> lore = challengeDescription(challengeName, player);
	    item.setLore(lore);
	    cp.add(item);
	}

	// Loop through other levels
	for (int i = 1; i < Settings.challengeLevels.size(); i++) {
	    levelDone = checkLevelCompletion(player, Settings.challengeLevels.get(i-1));
	    if (levelDone <= 0) {
		// Loop through challenges for this player
		for (String challengeName : challengeList.get(Settings.challengeLevels.get(i))) {
		    // Get the icon
		    ItemStack icon = null;
		    String iconName = plugin.getChallengeConfig().getString("challenges.challengeList." + challengeName + ".icon", "");
		    if (!iconName.isEmpty()) {
			try {
			    // Split if required
			    String[] split = iconName.split(":");
			    if (split.length == 1) {
				icon = new ItemStack(Material.valueOf(iconName));
			    } else if (split.length == 2) {
				icon = new ItemStack(Material.valueOf(split[0]));
				icon.setDurability(Integer.valueOf(split[1]).shortValue());
			    }
			} catch (Exception e) {
			    // Icon was not well formatted
			    plugin.getLogger().warning("Error in challenges.yml - icon format is incorrect for " + challengeName + ":" + iconName);
			    plugin.getLogger().warning("Format should be 'icon: MaterialType:Damage' where Damage is optional");
			}
		    }
		    if (icon == null) {
			icon = new ItemStack(Material.PAPER);
		    }
		    CPItem item = new CPItem(icon, ChatColor.GREEN + challengeName.substring(0, 1).toUpperCase() + challengeName.substring(1), "aichallenge c " + challengeName, null);
		    List<String> lore = challengeDescription(challengeName, player);
		    item.setLore(lore);
		    cp.add(item);
		}
	    } else {
		// Hint at what is to come
		CPItem item = new CPItem(Material.WATER, ChatColor.GOLD + Settings.challengeLevels.get(i), null, null);
		List<String> lore = new ArrayList<String>();
		// Add the level
		lore = chop(ChatColor.WHITE,Locale.challengestoComplete.replace("[challengesToDo]", String.valueOf(levelDone)).replace("[thisLevel]",Settings.challengeLevels.get(i-1)),25);
		// TODO Add other info here..
		item.setLore(lore);
		cp.add(item);
	    }
	}
	if (cp.size() > 0) {
	    // Make sure size is a multiple of 9
	    int size = cp.size() +8;
	    size -= (size % 9);
	    Inventory newPanel = Bukkit.createInventory(null, size, Locale.challengesguiTitle);
	    // Store the panel details for retrieval later
	    playerChallengeGUI.put(player.getUniqueId(), cp);
	    // Fill the inventory and return
	    for (CPItem i : cp) {
		newPanel.addItem(i.getItem());
	    }
	    return newPanel;
	}
	return null;
    }

    public List<CPItem> getCP(Player player) {
	return playerChallengeGUI.get(player.getUniqueId());
    }

    private List<String> chop(ChatColor color, String longLine, int length) {
	List<String> result = new ArrayList<String>();
	//int multiples = longLine.length() / length;
	int i = 0;
	for (i = 0; i< longLine.length(); i += length) {
	    //for (int i = 0; i< (multiples*length); i += length) {
	    int endIndex = Math.min(i + length, longLine.length());
	    String line = longLine.substring(i, endIndex);
	    // Do the following only if i+length is not the end of the string
	    if (endIndex < longLine.length()) {
		// Check if last character in this string is not a space
		if (!line.substring(line.length()-1).equals(" ")) {
		    // If it is not a space, check to see if the next character in long line is a space.
		    if (!longLine.substring(endIndex,endIndex+1).equals(" ")) {
			// If it is not, then we are cutting a word in two and need to backtrack to the last space if possible
			int lastSpace = line.lastIndexOf(" ");
			if (lastSpace < line.length()) {
			    line = line.substring(0, lastSpace);
			    i -= (length - lastSpace -1);
			}
		    }
		} 
	    }
	    //}
	    result.add(color + line);
	}
	//result.add(color + longLine.substring(i, longLine.length()));
	return result;
    }

    private List<String> challengeDescription(String challenge, Player player) {
	List<String> result = new ArrayList<String>();
	final int length = 25;
	//plugin.getLogger().info("DEBUG: challenge is '"+challenge+"'");
	//plugin.getLogger().info("challenges.challengeList." + challenge + ".level");
	//plugin.getLogger().info(plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".level"));
	result.addAll(chop(ChatColor.WHITE, Locale.challengeslevel +": " + plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".level",""),length));
	result.addAll(chop(ChatColor.GOLD, plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".description",""),length));
	final String type = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge + ".type","").toLowerCase();
	if (type.equals("inventory")) {
	    if (plugin.getChallengeConfig().getBoolean("challenges.challengeList." + challenge.toLowerCase() + ".takeItems")) {
		result.addAll(chop(ChatColor.RED, Locale.challengesitemTakeWarning,length));
	    }
	} else if (type.equals("island")) {
	    result.addAll(chop(ChatColor.RED, Locale.challengeserrorItemsNotThere,length));
	}
	if (players.checkChallenge(player.getUniqueId(),challenge)
		&& (!type.equals("inventory") || !plugin.getChallengeConfig().getBoolean("challenges.challengeList." + challenge + ".repeatable", false))) {
	    result.addAll(chop(ChatColor.RED, Locale.challengesnotRepeatable,length));
	    return result;
	}
	int moneyReward = 0;
	int expReward = 0;
	String rewardText = "";

	if (!players.checkChallenge(player.getUniqueId(),challenge)) {
	    // First time
	    moneyReward = plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge.toLowerCase() + ".moneyReward", 0);
	    rewardText = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".rewardText", "Goodies!").replace('&', '§');
	    expReward = plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge + ".xpReward", 0);
	    result.addAll(chop(ChatColor.GOLD, Locale.challengesfirstTimeRewards,length));
	} else {
	    // Repeat challenge
	    moneyReward = plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge.toLowerCase() + ".repeatMoneyReward", 0);
	    rewardText = plugin.getChallengeConfig().getString("challenges.challengeList." + challenge.toLowerCase() + ".repeatRewardText", "Goodies!").replace('&', '§');
	    expReward = plugin.getChallengeConfig().getInt("challenges.challengeList." + challenge + ".repeatExpReward", 0);
	    result.addAll(chop(ChatColor.GOLD,  Locale.challengesrepeatRewards,length));

	}	
	result.addAll(chop(ChatColor.WHITE, rewardText,length));
	if (expReward > 0) {
	    result.addAll(chop(ChatColor.GOLD, Locale.challengesexpReward + ": " + ChatColor.WHITE + expReward,length));
	}
	if (moneyReward > 0) { 
	    result.addAll(chop(ChatColor.GOLD, Locale.challengesmoneyReward + ": " + ChatColor.WHITE + VaultHelper.econ.format(moneyReward),length));
	}
	return result;	
    }


}
/**
 * 
 */
package com.wasteofplastic.acidisland;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author ben
 * This class is for a control panel button that has an icon, a command to run if pressed or a link to
 * another control panel.
 */
public class CPItem {

    private ItemStack item;
    private String command;
    private String nextSection;


    /**
     * @param item
     * @param material
     * @param description
     * @param command
     * @param nextSection
     */
    public CPItem(Material material, String description, String command, String nextSection) {
	this.command = command;
	this.nextSection = nextSection;
	item = new ItemStack(material);
	ItemMeta meta = item.getItemMeta();
	meta.setDisplayName(description);
	/*
	ArrayList<String> Lore = new ArrayList<String>();
	// Split up description into lines
	int endIndex = 0;
	for (int beginIndex = 0; endIndex <= description.length(); beginIndex+=20) {
	    endIndex = Math.min(beginIndex+20, description.length());
	    Lore.add(description.substring(beginIndex, endIndex));
	}
	meta.setLore(Lore);
	*/
	item.setItemMeta(meta);
    }


    /**
     * @return the command
     */
    public String getCommand() {
	return command;
    }


    /**
     * @return the nextSection
     */
    public String getNextSection() {
	return nextSection;
    }


    public ItemStack getItem() {
	return item;
    }

}

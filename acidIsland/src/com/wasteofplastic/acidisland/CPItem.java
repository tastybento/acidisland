/**
 * 
 */
package com.wasteofplastic.acidisland;

import java.util.List;

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
     * @param name
     * @param command
     * @param nextSection
     */
    public CPItem(Material material, String name, String command, String nextSection) {
	this.command = command;
	this.nextSection = nextSection;
	item = new ItemStack(material);
	ItemMeta meta = item.getItemMeta();
	meta.setDisplayName(name);
	item.setItemMeta(meta);
    }

    public void setLore(List<String> lore) {
	ItemMeta meta = item.getItemMeta();
	meta.setLore(lore);
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

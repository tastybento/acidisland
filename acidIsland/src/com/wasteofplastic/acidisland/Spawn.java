package com.wasteofplastic.acidisland;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class Spawn {
    private AcidIsland plugin;
    private YamlConfiguration spawnConfig;
    private Location spawnLoc;  
    private Location bedrock;
    private ConfigurationSection spawn;
    private int range;

    /**
     * @param plugin
     * @param players
     */
    public Spawn(AcidIsland plugin) {
	this.plugin = plugin;
	reload();
    }

    public void reload() {
	spawnConfig = AcidIsland.loadYamlFile("spawn.yml");
	spawn = spawnConfig.getConfigurationSection("spawn");
	// load the config items
	Settings.allowSpawnDoorUse = spawn.getBoolean("allowdooruse", true);
	Settings.allowSpawnLeverButtonUse = spawn.getBoolean("allowleverbuttonuse", true);
	Settings.allowSpawnChestAccess = spawn.getBoolean("allowchestaccess", true);
	Settings.allowSpawnFurnaceUse = spawn.getBoolean("allowfurnaceuse", true);
	Settings.allowSpawnRedStone = spawn.getBoolean("allowredstone", false);
	Settings.allowSpawnMusic = spawn.getBoolean("allowmusic", true);
	Settings.allowSpawnCrafting = spawn.getBoolean("allowcrafting", true);
	Settings.allowSpawnBrewing = spawn.getBoolean("allowbrewing", true);
	Settings.allowSpawnGateUse = spawn.getBoolean("allowgateuse", true);	
	Settings.allowSpawnMobSpawn = spawn.getBoolean("allowmobspawn", false);
	Settings.allowSpawnNoAcidWater = spawn.getBoolean("allowspawnnoacidwater", false);
	this.spawnLoc = AcidIsland.getLocationString(spawn.getString("location",""));
	this.bedrock = AcidIsland.getLocationString(spawn.getString("bedrock",""));
	this.range = spawn.getInt("range",100);
    }
    public void save() {
	// Save the spawn location
	plugin.getLogger().info("Saving spawn.yml file");
	String spawnPlace = AcidIsland.getStringLocation(spawnLoc);
	spawn.set("location", spawnPlace);
	spawn.set("bedrock", AcidIsland.getStringLocation(bedrock));
	//plugin.getLogger().info("Spawn = " + spawnPlace);
	AcidIsland.saveYamlFile(spawnConfig,"spawn.yml");
    }
    /**
     * @return the spawnLoc
     */
    public Location getSpawnLoc() {
	return spawnLoc;
    }

    /**
     * @param spawnLoc the spawnLoc to set
     * @param bedrock 
     */
    public void setSpawnLoc(Location bedrock, Location spawnLoc) {
	this.spawnLoc = spawnLoc;
	this.bedrock = bedrock;
    }

    /**
     * @return the range
     */
    public int getRange() {
	return range;
    }

    /**
     * @param range the range to set
     */
    public void setRange(int range) {
	this.range = range;
    }

    /**
     * @return the bedrock
     */
    public Location getBedrock() {
        return bedrock;
    }

    /**
     * Returns true if this location is within the spawn area
     * @param loc
     * @return
     */
    public boolean isAtSpawn(Location loc) {
	//plugin.getLogger().info("DEBUG: location is " + loc.toString());
	//plugin.getLogger().info("DEBUG spawnLoc is " + spawnLoc.toString());
	//plugin.getLogger().info("DEBUG: range = " + range);
	if (loc.distanceSquared(spawnLoc) < range * range) {
	    //plugin.getLogger().info("DEBUG: within range");
	    return true;
	}
	return false;
    }


}

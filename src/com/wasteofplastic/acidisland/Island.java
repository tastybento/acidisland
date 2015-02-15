package com.wasteofplastic.acidisland;

import java.util.Date;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Stores all the info about an island
 * Managed by GridManager
 * @author tastybento
 *
 */
public class Island {

    // Coordinates of the island area
    private int x;
    private int z;
    // Protection size
    private int protectionRange;
    // Height of island
    private int y;
    // The actual center of the island itself
    private Location center;
    // World the island is in
    private World world;
    // The owner of the island
    private UUID owner;
    // Time parameters
    private Date createdDate;
    private Date updatedDate;
    // A password associated with the island
    private String password;
    // Votes for how awesome the island is
    private int votes;
    /**
     * @param x
     * @param z
     * @param protectionRange
     * @param center
     * @param owner
     * @param createdDate
     * @param updatedDate
     * @param password
     * @param votes
     */
    public Island(int x, int z, int protectionRange, Location center, UUID owner, Date createdDate, Date updatedDate, String password, int votes) {
	this.x = x;
	this.z = z;
	this.protectionRange = protectionRange;
	this.center = center;
	this.world = center.getWorld();
	this.y = center.getBlockY();
	this.owner = owner;
	this.createdDate = createdDate;
	this.updatedDate = updatedDate;
	this.password = password;
	this.votes = votes;
    }

    /**
     * Checks if a location is within this island's protected area
     * @param loc
     * @return
     */
    public boolean onIsland(Location target) {
	if (target.getWorld().equals(world)) {
	    if (target.getX() > center.getBlockX() - protectionRange / 2
		    && target.getX() < center.getBlockX() + protectionRange / 2
		    && target.getZ() > center.getBlockZ() - protectionRange / 2
		    && target.getZ() < center.getBlockZ() + protectionRange / 2) {
		return true;
	    }
	}
	return false;	
    }

    /**
     * @return the x
     */
    public int getRow() {
	return x;
    }
    /**
     * @param x the x to set
     */
    public void setRow(int row) {
	this.x = row;
    }
    /**
     * @return the z
     */
    public int getColumn() {
	return z;
    }
    /**
     * @param z the z to set
     */
    public void setColumn(int column) {
	this.z = column;
    }
    /**
     * @return the protectionRange
     */
    public int getProtectionSize() {
	return protectionRange;
    }
    /**
     * @param protectionRange the protectionRange to set
     */
    public void setProtectionSize(int protectionSize) {
	this.protectionRange = protectionSize;
    }
    /**
     * @return the center
     */
    public Location getCenter() {
	return center;
    }
    /**
     * @param center the center to set
     */
    public void setCenter(Location center) {
	this.center = center;
    }
    /**
     * @return the owner
     */
    public UUID getOwner() {
	return owner;
    }
    /**
     * @param owner the owner to set
     */
    public void setOwner(UUID owner) {
	this.owner = owner;
    }
    /**
     * @return the createdDate
     */
    public Date getCreatedDate() {
	return createdDate;
    }
    /**
     * @param createdDate the createdDate to set
     */
    public void setCreatedDate(Date createdDate) {
	this.createdDate = createdDate;
    }
    /**
     * @return the updatedDate
     */
    public Date getUpdatedDate() {
	return updatedDate;
    }
    /**
     * @param updatedDate the updatedDate to set
     */
    public void setUpdatedDate(Date updatedDate) {
	this.updatedDate = updatedDate;
    }
    /**
     * @return the password
     */
    public String getPassword() {
	return password;
    }
    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
	this.password = password;
    }
    /**
     * @return the votes
     */
    public int getVotes() {
	return votes;
    }
    /**
     * @param votes the votes to set
     */
    public void setVotes(int votes) {
	this.votes = votes;
    }


}

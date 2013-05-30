package me.FurH.Core.packets;

import me.FurH.Core.CorePlugin;
import org.bukkit.entity.Player;

/**
 *
 * @author FurmigaHumana
 * All Rights Reserved unless otherwise explicitly stated.
 */
public abstract class IPacketQueue {
    
    private String owner;

    public IPacketQueue(CorePlugin plugin) {
        this.owner = plugin.getDescription().getName();
    }
    
    /**
     * Receive a Custom Payload (Packet250CustomPayload)
     *
     * @param player the player
     * @param channel the packet channel
     * @param length the packet data length
     * @param data the packet data
     * @return true if the packet is ment to be procesed by the server, false otherwise 
     */
    public abstract boolean handleCustomPayload(Player player, String channel, int length, byte[] data);
    
    /**
     * Receive and set a custom Payload (Packet250CustomPayload)
     *
     * @param player the player
     * @param object the packet object
     * @return the modified packet object
     */
    public abstract Object handleAndSetCustomPayload(Player player, Object object);
    
    /**
     * Receive the Client Settings (Packet204LocaleAndViewDistance)
     *
     * @param player the player
     * @return true if the packet is ment to be processe by the server, false otherwise
     */
    public abstract boolean handleClientSettings(Player player);

    /**
     * Receive a chunk packet (Packet56MapChunkBulk)
     *
     * @param player the player
     * @param object the packet object
     * @return the modified (or not) chunk packet
     */
    public abstract Object handleMapChunkBulk(Player player, Object object);
    
    /**
     * Receive a chunk packet (Packet51MapChunk)
     *
     * @param player the player
     * @param object the packet object
     * @return the modified (or not) chunk packet
     */
    public abstract Object handleMapChunk(Player player, Object object);

    /**
     * Receive a block place packet (Packet15Place)
     *
     * @param player the player
     * @param id the block id
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    public abstract void handleBlockPlace(Player player, int id, int x, int y, int z);

    /**
     * Receive a block 'dig' packet (Packet14BlockDig)
     *
     * @param player the player
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    public abstract void handleBlockBreak(Player player, int x, int y, int z);

    @Override
    public int hashCode() {
        int hash = 7;

        hash = 89 * hash + (this.owner != null ? this.owner.hashCode() : 0);

        return hash;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final IPacketQueue other = (IPacketQueue) obj;
        if ((this.owner == null) ? (other.owner != null) : !this.owner.equals(other.owner)) {
            return false;
        }

        return true;
    }
    
    
}

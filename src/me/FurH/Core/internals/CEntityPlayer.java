package me.FurH.Core.internals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import me.FurH.Core.exceptions.CoreException;
import me.FurH.Core.packets.PacketCustomPayload;
import me.FurH.Core.packets.PacketManager;
import me.FurH.Core.reflection.ReflectionUtils;
import net.minecraft.server.v1_5_R3.EntityPlayer;
import net.minecraft.server.v1_5_R3.ItemStack;
import net.minecraft.server.v1_5_R3.Packet;
import net.minecraft.server.v1_5_R3.Packet250CustomPayload;
import net.minecraft.server.v1_5_R3.Packet51MapChunk;
import net.minecraft.server.v1_5_R3.Packet56MapChunkBulk;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_5_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_5_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;

/**
 *
 * @author FurmigaHumana All Rights Reserved unless otherwise explicitly stated.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class CEntityPlayer implements IEntityPlayer {

    private boolean inventory_hidden = false;
    private EntityPlayer entity;
    private Player player;

    @Override
    public IEntityPlayer setEntityPlayer(Player player) {
        this.player = player;

        this.entity =
                ((CraftPlayer) player).getHandle();

        return this;
    }

    @Override
    public int ping() {
        return entity.ping;
    }

    @Override
    public void setInboundQueue() throws CoreException {
            
        Queue<Packet> newSyncPackets = new ConcurrentLinkedQueue<Packet>() {
            
            private static final long serialVersionUID = 7299839519835756010L;

            @Override
            public boolean add(Packet packet) {

                if (packet.n() == 250) {

                    Packet250CustomPayload p250 = (Packet250CustomPayload) packet;

                    if (!PacketManager.callCustomPayload(player, p250.data, p250.length, p250.tag)) {
                        return false;
                    }

                } else
                if (packet.n() == 204) {
                    if (!PacketManager.callClientSettings(player)) {
                        return false;
                    }
                }

                return super.add(packet);
            }
        };

        if (isNettyEnabled()) {

            Queue<Packet> syncPackets = (Queue<Packet>) ReflectionUtils.getPrivateField(entity.playerConnection.networkManager, "syncPackets");
            newSyncPackets.addAll(syncPackets);
            ReflectionUtils.setFinalField(entity.playerConnection.networkManager, "syncPackets", newSyncPackets);

        } else {
            
            Queue syncPackets = (Queue) ReflectionUtils.getPrivateField(entity.playerConnection.networkManager, "inboundQueue");
            newSyncPackets.addAll(syncPackets);
            ReflectionUtils.setFinalField(entity.playerConnection.networkManager, "inboundQueue", newSyncPackets);
            
        }
    }

    @Override
    public void sendCustomPayload(PacketCustomPayload packet) {
        Packet250CustomPayload payload = new Packet250CustomPayload();

        payload.tag = packet.getChannel();
        payload.data = packet.getData();
        payload.length = packet.getLength();

        entity.playerConnection.networkManager.queue(payload);
    }

    @Override
    public void hideInventory() {
        inventory_hidden = false;
        
        ItemStack stack = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.AIR, 1));

        List stacks = new ArrayList();
        for (int j1 = 0; j1 < entity.activeContainer.a().size(); j1++) {
            stacks.add(stack);
        }

        entity.a(entity.activeContainer, stacks);
        inventory_hidden = true;
    }

    @Override
    public void unHideInventory() {
        inventory_hidden = false;
        player.updateInventory();
    }

    @Override
    public boolean isInventoryHidden() {
        return this.inventory_hidden;
    }

    @Override
    public void setOutboundQueue() throws CoreException {

        if (!isNettyEnabled()) {
            
            List newhighPriorityQueue = Collections.synchronizedList(new PriorityQueue().getArrayList());
            List newlowPriorityQueue = Collections.synchronizedList(new PriorityQueue().getArrayList());
            
            List highPriorityQueue = (List) ReflectionUtils.getPrivateField(entity.playerConnection.networkManager, "highPriorityQueue");
            List lowPriorityQueue = (List) ReflectionUtils.getPrivateField(entity.playerConnection.networkManager, "lowPriorityQueue");

            if (highPriorityQueue != null) {
                newhighPriorityQueue.addAll(highPriorityQueue);
                highPriorityQueue.clear();
            }
            
            if (lowPriorityQueue != null) {
                newlowPriorityQueue.addAll(lowPriorityQueue);
                lowPriorityQueue.clear();
            }

            ReflectionUtils.setFinalField(entity.playerConnection.networkManager, "highPriorityQueue", newhighPriorityQueue);
            ReflectionUtils.setFinalField(entity.playerConnection.networkManager, "lowPriorityQueue", newlowPriorityQueue);
        } else {

            HookSpigot hook = new HookSpigot();
            hook.hook();
            
        }
    }
    
    private static boolean isNettyEnabled() {
        try {
            Class.forName("org.spigotmc.netty.NettyNetworkManager");
            return true;
        } catch (NoClassDefFoundError ex) {
            return false;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
    
    private class PriorityQueue {
        private List queue = new ArrayList<Packet>() {
            
            private static final long serialVersionUID = 927895363924203624L;

            @Override
            public boolean add(Packet packet) {

                if (isInventoryHidden() && (packet.n() == 103 || packet.n() == 104)) {
                    return false;
                }

                return super.add(packet);
            }
            
            @Override
            public Packet remove(int index) {

                Packet packet = super.remove(index);
                if (packet != null) {
                    if (packet instanceof Packet56MapChunkBulk) {
                        packet = (Packet56MapChunkBulk) PacketManager.callMapChunkBulk(player, (Packet56MapChunkBulk) packet);
                    } else
                    if (packet instanceof Packet51MapChunk) {
                        packet = (Packet51MapChunk) PacketManager.callMapChunk(player, (Packet51MapChunk) packet);
                    }
                }

                return packet;
            }
        };
        
        public List<Packet> getArrayList() {
            return queue;
        }
    }
    
    private class HookSpigot {
        private void hook() throws CoreException {
            io.netty.channel.Channel channel = (io.netty.channel.Channel) ReflectionUtils.getPrivateField(entity.playerConnection.networkManager, "channel");

            channel.pipeline().replace("encoder", "encoder", new FPacketEncoder());

            ReflectionUtils.setFinalField(entity.playerConnection.networkManager, "channel", channel);
        }
    }
    
    private class FPacketEncoder extends org.spigotmc.netty.PacketEncoder {

        private FPacketEncoder() {
            super((org.spigotmc.netty.NettyNetworkManager) entity.playerConnection.networkManager);
        }

        @Override
        public void encode(io.netty.channel.ChannelHandlerContext ctx, Packet packet, io.netty.buffer.ByteBuf out) throws Exception {

            if (packet != null) {
                
                if (isInventoryHidden() && (packet.n() == 103 || packet.n() == 104)) {
                    return;
                }

                if (packet instanceof Packet56MapChunkBulk) {
                    packet = (Packet56MapChunkBulk) PacketManager.callMapChunkBulk(player, (Packet56MapChunkBulk) packet);
                } else
                if (packet instanceof Packet51MapChunk) {
                    packet = (Packet51MapChunk) PacketManager.callMapChunk(player, (Packet51MapChunk) packet);
                }
            }

            super.encode(ctx, packet, out);
        }
    }
}
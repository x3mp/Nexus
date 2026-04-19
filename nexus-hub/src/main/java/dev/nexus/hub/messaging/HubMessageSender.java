package dev.nexus.hub.messaging;

import dev.nexus.api.messaging.NexusChannel;
import dev.nexus.api.messaging.NexusPacket;
import dev.nexus.hub.redis.RedisManager;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Sends {@link NexusPacket} instances to other Nexus components.
 *
 * <p>Two transport paths:
 * <ul>
 *   <li><b>Minion</b> — via Bukkit plugin messaging on channel {@link NexusChannel#MAIN}.
 *       The player must be currently connected to the target server.
 *   <li><b>Velocity</b> — via Redis pub/sub on channel {@link NexusChannel#VELOCITY}.
 *       This path works regardless of player count on the hub.
 * </ul>
 *
 * @since 1.0.0
 */
public class HubMessageSender {

    private final RedisManager redis;
    private final Logger logger;

    /**
     * @param redis  shared Redis manager
     * @param logger plugin logger
     */
    public HubMessageSender(RedisManager redis, Logger logger) {
        this.redis = redis;
        this.logger = logger;
    }

    /**
     * Sends a packet to a specific player via Bukkit plugin messaging.
     *
     * <p>Used to deliver packets that target a particular minion server through
     * a player who is currently connected to that server.
     *
     * @param player target player (must be online and on the target server)
     * @param packet packet to send
     */
    public void send(Player player, NexusPacket packet) {
        byte[] data = packet.toJson().getBytes(StandardCharsets.UTF_8);
        player.sendPluginMessage(
                org.bukkit.Bukkit.getPluginManager().getPlugin("NexusHub"),
                NexusChannel.MAIN, data);
    }

    /**
     * Sends a packet to Velocity via Redis pub/sub on {@link NexusChannel#VELOCITY}.
     *
     * <p>This is the preferred path for hub-to-proxy communication because it does not
     * require an online player as a courier.
     *
     * @param packet packet to publish
     */
    public void sendToVelocity(NexusPacket packet) {
        try (Jedis jedis = redis.getResource()) {
            jedis.publish(NexusChannel.VELOCITY, packet.toJson());
        } catch (Exception e) {
            logger.warning("Failed to publish packet to Velocity: " + e.getMessage());
        }
    }

    /**
     * Sends a packet to a minion by publishing to Redis pub/sub on a per-minion channel.
     *
     * <p>The minion must subscribe to {@code nexus:minion:{serverName}} to receive it.
     *
     * @param serverName Velocity server name of the target minion
     * @param packet     packet to send
     */
    public void sendToMinion(String serverName, NexusPacket packet) {
        try (Jedis jedis = redis.getResource()) {
            jedis.publish("nexus:minion:" + serverName, packet.toJson());
        } catch (Exception e) {
            logger.warning("Failed to send packet to minion " + serverName + ": " + e.getMessage());
        }
    }
}

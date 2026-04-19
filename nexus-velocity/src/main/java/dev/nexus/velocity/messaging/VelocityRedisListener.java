package dev.nexus.velocity.messaging;

import dev.nexus.api.messaging.NexusChannel;
import dev.nexus.api.messaging.NexusPacket;
import dev.nexus.api.messaging.PacketType;
import dev.nexus.api.messaging.packets.MinionRegisterPacket;
import dev.nexus.api.messaging.packets.MinionUnregisterPacket;
import dev.nexus.api.messaging.packets.PlayerTransferPacket;
import dev.nexus.velocity.PlayerRouter;
import dev.nexus.velocity.ServerRegistry;
import dev.nexus.velocity.redis.RedisManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Subscribes to the Redis pub/sub channel {@link NexusChannel#VELOCITY} and dispatches
 * incoming packets to the appropriate Velocity component.
 *
 * <p>Runs the subscriber on a dedicated background thread. On connection loss it
 * automatically attempts to reconnect every 5 seconds.
 *
 * @since 1.0.0
 */
public class VelocityRedisListener {

    private final RedisManager redis;
    private final ServerRegistry serverRegistry;
    private final PlayerRouter playerRouter;
    private final Logger logger;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "nexus-velocity-redis-listener");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean running = true;

    /**
     * @param redis          shared Redis manager; a dedicated connection is borrowed for subscribe
     * @param serverRegistry handles MINION_REGISTER and MINION_UNREGISTER packets
     * @param playerRouter   handles PLAYER_TRANSFER packets
     * @param logger         plugin logger
     */
    public VelocityRedisListener(RedisManager redis, ServerRegistry serverRegistry,
                                  PlayerRouter playerRouter, Logger logger) {
        this.redis = redis;
        this.serverRegistry = serverRegistry;
        this.playerRouter = playerRouter;
        this.logger = logger;
    }

    /**
     * Starts the background subscriber thread.
     */
    public void start() {
        executor.submit(this::subscribeLoop);
        logger.info("VelocityRedisListener started on channel " + NexusChannel.VELOCITY);
    }

    /**
     * Stops the subscriber and shuts down the background thread.
     */
    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    private void subscribeLoop() {
        while (running) {
            try (Jedis jedis = redis.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        dispatch(message);
                    }
                }, NexusChannel.VELOCITY);
            } catch (Exception e) {
                if (!running) return;
                logger.warning("Redis subscriber lost connection, reconnecting in 5s: " + e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private void dispatch(String json) {
        PacketType type = NexusPacket.typeOf(json);
        if (type == null) {
            logger.warning("Received unrecognised packet on " + NexusChannel.VELOCITY + ": " + json);
            return;
        }

        switch (type) {
            case MINION_REGISTER -> {
                MinionRegisterPacket pkt = NexusPacket.fromJson(json, MinionRegisterPacket.class);
                serverRegistry.register(pkt.getServerName(), pkt.getHost(), pkt.getPort());
            }
            case MINION_UNREGISTER -> {
                MinionUnregisterPacket pkt = NexusPacket.fromJson(json, MinionUnregisterPacket.class);
                serverRegistry.unregister(pkt.getServerName());
            }
            case PLAYER_TRANSFER -> {
                PlayerTransferPacket pkt = NexusPacket.fromJson(json, PlayerTransferPacket.class);
                playerRouter.transfer(pkt.getPlayerUUID(), pkt.getTargetServerName());
            }
            default -> logger.fine("Ignored packet type " + type + " on Velocity listener");
        }
    }
}

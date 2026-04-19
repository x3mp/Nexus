package dev.nexus.hub.queue;

import dev.nexus.api.game.GameMeta;
import dev.nexus.api.game.NexusGame;
import dev.nexus.api.queue.QueueEntry;
import dev.nexus.hub.game.GameRegistry;
import dev.nexus.hub.minion.MinionManager;
import dev.nexus.hub.redis.RedisManager;
import org.bukkit.scheduler.BukkitRunnable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Periodic task that checks all game queues and triggers minion provisioning when a
 * queue reaches the minimum player threshold.
 *
 * <p>A Redis distributed lock ({@code nexus:lock:provision:{gameId}}) prevents two hub
 * nodes from provisioning for the same game simultaneously.
 *
 * @since 1.0.0
 */
public class MatchmakingTask extends BukkitRunnable {

    private static final long LOCK_TTL_MS = 10_000;

    private final GameRegistry gameRegistry;
    private final QueueManager queueManager;
    private final MinionManager minionManager;
    private final RedisManager redis;
    private final Logger logger;

    /**
     * @param gameRegistry   registry of all registered games
     * @param queueManager   Redis-backed queue manager
     * @param minionManager  handles provisioning when a match is ready
     * @param redis          used to acquire the distributed provision lock
     * @param logger         plugin logger
     */
    public MatchmakingTask(GameRegistry gameRegistry, QueueManager queueManager,
                           MinionManager minionManager, RedisManager redis, Logger logger) {
        this.gameRegistry = gameRegistry;
        this.queueManager = queueManager;
        this.minionManager = minionManager;
        this.redis = redis;
        this.logger = logger;
    }

    @Override
    public void run() {
        for (NexusGame game : gameRegistry.getAll()) {
            GameMeta meta = game.getClass().getAnnotation(GameMeta.class);
            if (meta == null) continue;

            long queueSize = queueManager.getSize(meta.id());
            if (queueSize < meta.minPlayers()) continue;

            if (!acquireLock(meta.id())) continue;

            try {
                List<QueueEntry> matched = queueManager.dequeue(meta.id(), meta.maxPlayers());
                if (matched.size() < meta.minPlayers()) {
                    // Re-enqueue if we raced and got too few; release lock and skip
                    for (QueueEntry entry : matched) {
                        queueManager.enqueue(entry.getPlayerUUID(), entry.getPlayerName(),
                                meta.id(), entry.getPriority());
                    }
                    return;
                }

                List<UUID> players = matched.stream().map(QueueEntry::getPlayerUUID).toList();
                minionManager.provision(meta.id(), players);

            } finally {
                releaseLock(meta.id());
            }
        }
    }

    private boolean acquireLock(String gameId) {
        try (Jedis jedis = redis.getResource()) {
            String result = jedis.set(lockKey(gameId), "1",
                    SetParams.setParams().nx().px(LOCK_TTL_MS));
            return "OK".equals(result);
        }
    }

    private void releaseLock(String gameId) {
        try (Jedis jedis = redis.getResource()) {
            jedis.del(lockKey(gameId));
        }
    }

    private static String lockKey(String gameId) {
        return "nexus:lock:provision:" + gameId;
    }
}

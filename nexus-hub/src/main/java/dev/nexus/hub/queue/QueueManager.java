package dev.nexus.hub.queue;

import dev.nexus.api.queue.QueueEntry;
import dev.nexus.hub.redis.RedisManager;
import com.google.gson.Gson;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Redis-backed manager for all game queues.
 *
 * <p>Redis schema:
 * <ul>
 *   <li>{@code nexus:queue:{gameId}} — List of serialised {@link QueueEntry} JSON strings (LPUSH/RPOP)
 *   <li>{@code nexus:queue:{gameId}:priority} — Sorted set for VIP/priority players (score = priority)
 *   <li>{@code nexus:queue:player:{uuid}} — String mapping a player to their current game queue
 * </ul>
 *
 * @since 1.0.0
 */
public class QueueManager {

    private static final Gson GSON = new Gson();

    private final RedisManager redis;

    /**
     * @param redis shared Redis manager
     */
    public QueueManager(RedisManager redis) {
        this.redis = redis;
    }

    /**
     * Adds a player to the queue for the given game.
     *
     * <p>Silently replaces any existing queue entry for this player.
     *
     * @param playerUUID UUID of the player joining
     * @param playerName display name of the player
     * @param gameId     game to queue for
     * @param priority   queue priority; 0 for regular players, higher = matched sooner
     */
    public void enqueue(UUID playerUUID, String playerName, String gameId, int priority) {
        remove(playerUUID);

        QueueEntry entry = new QueueEntry(playerUUID, playerName, gameId,
                System.currentTimeMillis(), priority);
        String json = GSON.toJson(entry);

        try (Jedis jedis = redis.getResource()) {
            jedis.lpush(queueKey(gameId), json);
            if (priority > 0) {
                jedis.zadd(priorityKey(gameId), priority, playerUUID.toString());
            }
            jedis.set(playerQueueKey(playerUUID), gameId);
        }
    }

    /**
     * Removes a player from whichever queue they are currently in.
     *
     * @param playerUUID UUID of the player to remove
     */
    public void remove(UUID playerUUID) {
        try (Jedis jedis = redis.getResource()) {
            String gameId = jedis.get(playerQueueKey(playerUUID));
            if (gameId == null) return;

            // Remove by scanning the list — acceptable for queue sizes in this use case
            List<String> entries = jedis.lrange(queueKey(gameId), 0, -1);
            for (String json : entries) {
                QueueEntry entry = GSON.fromJson(json, QueueEntry.class);
                if (entry.getPlayerUUID() != null
                        && entry.getPlayerUUID().toString().equals(playerUUID.toString())) {
                    jedis.lrem(queueKey(gameId), 1, json);
                    break;
                }
            }
            jedis.zrem(priorityKey(gameId), playerUUID.toString());
            jedis.del(playerQueueKey(playerUUID));
        }
    }

    /**
     * Dequeues up to {@code count} players from the given game's queue.
     *
     * <p>Priority players are dequeued first via the sorted set; remaining slots
     * are filled from the regular FIFO list.
     *
     * @param gameId game to dequeue from
     * @param count  maximum number of players to dequeue
     * @return list of dequeued entries (may be smaller than {@code count})
     */
    public List<QueueEntry> dequeue(String gameId, int count) {
        List<QueueEntry> result = new ArrayList<>();
        try (Jedis jedis = redis.getResource()) {
            // Drain priority players first
            List<String> priorityUuids = jedis.zrevrange(priorityKey(gameId), 0, count - 1);
            for (String uuid : priorityUuids) {
                if (result.size() >= count) break;
                QueueEntry entry = removeByUuid(jedis, gameId, UUID.fromString(uuid));
                if (entry != null) result.add(entry);
            }

            // Fill remainder from regular queue
            while (result.size() < count) {
                String json = jedis.rpop(queueKey(gameId));
                if (json == null) break;
                QueueEntry entry = GSON.fromJson(json, QueueEntry.class);
                if (entry.getPlayerUUID() != null) {
                    jedis.del(playerQueueKey(entry.getPlayerUUID()));
                    result.add(entry);
                }
            }
        }
        return result;
    }

    /**
     * Returns the number of players currently queued for the given game.
     *
     * @param gameId game identifier
     * @return queue size
     */
    public long getSize(String gameId) {
        try (Jedis jedis = redis.getResource()) {
            return jedis.llen(queueKey(gameId));
        }
    }

    /**
     * Returns the 1-based queue position of a player, or 0 if not queued.
     *
     * @param playerUUID player to look up
     * @param gameId     game queue to search
     * @return 1-based position, or 0 if not found
     */
    public int getPosition(UUID playerUUID, String gameId) {
        try (Jedis jedis = redis.getResource()) {
            List<String> entries = jedis.lrange(queueKey(gameId), 0, -1);
            for (int i = 0; i < entries.size(); i++) {
                QueueEntry entry = GSON.fromJson(entries.get(i), QueueEntry.class);
                if (entry.getPlayerUUID() != null
                        && entry.getPlayerUUID().toString().equals(playerUUID.toString())) {
                    return i + 1;
                }
            }
            return 0;
        }
    }

    private QueueEntry removeByUuid(Jedis jedis, String gameId, UUID playerUUID) {
        List<String> entries = jedis.lrange(queueKey(gameId), 0, -1);
        for (String json : entries) {
            QueueEntry entry = GSON.fromJson(json, QueueEntry.class);
            if (entry.getPlayerUUID() != null
                    && entry.getPlayerUUID().toString().equals(playerUUID.toString())) {
                jedis.lrem(queueKey(gameId), 1, json);
                jedis.zrem(priorityKey(gameId), playerUUID.toString());
                jedis.del(playerQueueKey(playerUUID));
                return entry;
            }
        }
        return null;
    }

    private static String queueKey(String gameId) { return "nexus:queue:" + gameId; }
    private static String priorityKey(String gameId) { return "nexus:queue:" + gameId + ":priority"; }
    private static String playerQueueKey(UUID uuid) { return "nexus:queue:player:" + uuid; }
}

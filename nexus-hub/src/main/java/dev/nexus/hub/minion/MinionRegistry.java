package dev.nexus.hub.minion;

import dev.nexus.api.minion.Minion;
import dev.nexus.api.minion.MinionState;
import dev.nexus.hub.redis.RedisManager;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Redis-backed registry tracking all live minion instances.
 *
 * <p>Redis schema:
 * <ul>
 *   <li>{@code nexus:minions} — Set of all active minion IDs
 *   <li>{@code nexus:minion:{id}} — Hash with all minion fields
 *   <li>{@code nexus:minion:{id}:players} — Set of player UUIDs assigned to this minion
 * </ul>
 *
 * @since 1.0.0
 */
public class MinionRegistry {

    private static final String KEY_MINIONS = "nexus:minions";

    private final RedisManager redis;

    /**
     * @param redis shared Redis manager
     */
    public MinionRegistry(RedisManager redis) {
        this.redis = redis;
    }

    /**
     * Registers a newly provisioned minion in Redis.
     *
     * @param minion the minion to register
     */
    public void register(Minion minion) {
        try (Jedis jedis = redis.getResource()) {
            String key = minionKey(minion.getMinionId());
            jedis.sadd(KEY_MINIONS, minion.getMinionId());
            jedis.hset(key, "gameId", minion.getGameId());
            jedis.hset(key, "turretId", minion.getTurretId());
            jedis.hset(key, "serverName", minion.getServerName());
            jedis.hset(key, "host", minion.getHost());
            jedis.hset(key, "port", String.valueOf(minion.getPort()));
            jedis.hset(key, "state", minion.getState().name());
            jedis.hset(key, "mapName", minion.getMapName() != null ? minion.getMapName() : "");
            jedis.hset(key, "startedAt", String.valueOf(minion.getStartedAt()));
            jedis.hset(key, "playerCount", String.valueOf(minion.getPlayerCount()));
        }
    }

    /**
     * Removes a dead minion from Redis. Does not touch turret load — call
     * {@link dev.nexus.hub.turret.TurretRegistry#onMinionDead} separately.
     *
     * @param minionId ID of the minion to remove
     */
    public void unregister(String minionId) {
        try (Jedis jedis = redis.getResource()) {
            jedis.srem(KEY_MINIONS, minionId);
            jedis.del(minionKey(minionId));
            jedis.del(minionKey(minionId) + ":players");
        }
    }

    /**
     * Returns a snapshot of the minion with the given ID, or {@code null} if not found.
     *
     * @param minionId minion identifier
     * @return minion snapshot, or {@code null}
     */
    public Minion get(String minionId) {
        try (Jedis jedis = redis.getResource()) {
            return readMinion(jedis, minionId);
        }
    }

    /**
     * Returns snapshots of all currently registered minions.
     *
     * @return list of minion snapshots
     */
    public List<Minion> getAll() {
        try (Jedis jedis = redis.getResource()) {
            List<Minion> result = new ArrayList<>();
            for (String id : jedis.smembers(KEY_MINIONS)) {
                Minion m = readMinion(jedis, id);
                if (m != null) result.add(m);
            }
            return result;
        }
    }

    /**
     * Updates the lifecycle state of a minion.
     *
     * @param minionId minion identifier
     * @param state    new lifecycle state
     */
    public void updateState(String minionId, MinionState state) {
        try (Jedis jedis = redis.getResource()) {
            jedis.hset(minionKey(minionId), "state", state.name());
        }
    }

    /**
     * Adds a player to the minion's player set and increments its player count.
     *
     * @param minionId minion identifier
     * @param player   UUID of the player joining
     */
    public void addPlayer(String minionId, UUID player) {
        try (Jedis jedis = redis.getResource()) {
            jedis.sadd(minionKey(minionId) + ":players", player.toString());
            jedis.hincrBy(minionKey(minionId), "playerCount", 1);
        }
    }

    /**
     * Removes a player from the minion's player set and decrements its player count.
     *
     * @param minionId minion identifier
     * @param player   UUID of the player leaving
     */
    public void removePlayer(String minionId, UUID player) {
        try (Jedis jedis = redis.getResource()) {
            jedis.srem(minionKey(minionId) + ":players", player.toString());
            jedis.hincrBy(minionKey(minionId), "playerCount", -1);
        }
    }

    /**
     * Returns the UUIDs of all players assigned to a minion.
     *
     * @param minionId minion identifier
     * @return set of player UUIDs
     */
    public Set<String> getPlayers(String minionId) {
        try (Jedis jedis = redis.getResource()) {
            return jedis.smembers(minionKey(minionId) + ":players");
        }
    }

    private Minion readMinion(Jedis jedis, String id) {
        Map<String, String> hash = jedis.hgetAll(minionKey(id));
        if (hash == null || hash.isEmpty()) return null;

        Minion m = new Minion();
        m.setMinionId(id);
        m.setGameId(hash.getOrDefault("gameId", ""));
        m.setTurretId(hash.getOrDefault("turretId", ""));
        m.setServerName(hash.getOrDefault("serverName", ""));
        m.setHost(hash.getOrDefault("host", ""));
        m.setPort(parseInt(hash.get("port"), 0));
        String stateStr = hash.getOrDefault("state", MinionState.DEAD.name());
        try { m.setState(MinionState.valueOf(stateStr)); }
        catch (IllegalArgumentException e) { m.setState(MinionState.DEAD); }
        m.setMapName(hash.getOrDefault("mapName", ""));
        m.setStartedAt(parseLong(hash.get("startedAt"), 0L));
        m.setPlayerCount(parseInt(hash.get("playerCount"), 0));
        return m;
    }

    private static String minionKey(String minionId) {
        return "nexus:minion:" + minionId;
    }

    private static int parseInt(String val, int def) {
        if (val == null) return def;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return def; }
    }

    private static long parseLong(String val, long def) {
        if (val == null) return def;
        try { return Long.parseLong(val); } catch (NumberFormatException e) { return def; }
    }
}

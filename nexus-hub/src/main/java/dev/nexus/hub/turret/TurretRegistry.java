package dev.nexus.hub.turret;

import dev.nexus.api.turret.Turret;
import dev.nexus.api.turret.TurretStatus;
import dev.nexus.hub.config.TurretConfig;
import dev.nexus.hub.redis.RedisManager;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Redis-backed registry of all turret hosts.
 *
 * <p>Static configuration is loaded from {@link TurretConfig} objects and written to Redis on
 * startup. Dynamic load ({@code currentMinions}, {@code usedRamMb}) is updated atomically
 * in Redis whenever a minion is provisioned or retired.
 *
 * <p>Redis schema:
 * <ul>
 *   <li>{@code nexus:turrets} — Set of all turret IDs
 *   <li>{@code nexus:turret:{id}} — Hash with all turret fields
 *   <li>{@code nexus:turret:{id}:heartbeat} — epoch ms of last heartbeat
 * </ul>
 *
 * @since 1.0.0
 */
public class TurretRegistry {

    private static final String KEY_TURRETS = "nexus:turrets";

    private final RedisManager redis;
    private final Logger logger;

    /**
     * @param redis  shared Redis manager
     * @param logger plugin logger
     */
    public TurretRegistry(RedisManager redis, Logger logger) {
        this.redis = redis;
        this.logger = logger;
    }

    /**
     * Populates Redis with the static configuration for each turret.
     *
     * <p>This does not reset dynamic load counters if they already exist in Redis,
     * allowing a hub restart to pick up in-flight minion counts.
     *
     * @param configs list of turret configs from {@code config.yml}
     */
    public void loadFromConfig(List<TurretConfig> configs) {
        try (Jedis jedis = redis.getResource()) {
            for (TurretConfig cfg : configs) {
                String key = turretKey(cfg.getId());
                jedis.sadd(KEY_TURRETS, cfg.getId());

                jedis.hset(key, "host", cfg.getHost());
                jedis.hset(key, "maxMinions", String.valueOf(cfg.getMaxMinions()));
                jedis.hset(key, "totalRamMb", String.valueOf(cfg.getTotalRamMb()));
                jedis.hset(key, "tags", String.join(",", cfg.getTags()));

                jedis.hsetnx(key, "currentMinions", "0");
                jedis.hsetnx(key, "usedRamMb", "0");
                jedis.hsetnx(key, "status", TurretStatus.ONLINE.name());

                logger.info("Loaded turret: " + cfg.getId() + " (" + cfg.getHost() + ")");
            }
        }
    }

    /**
     * Returns all turrets currently tracked in Redis.
     *
     * @return list of turret snapshots
     */
    public List<Turret> getAll() {
        try (Jedis jedis = redis.getResource()) {
            List<Turret> turrets = new ArrayList<>();
            for (String id : jedis.smembers(KEY_TURRETS)) {
                Turret t = readTurret(jedis, id);
                if (t != null) turrets.add(t);
            }
            return turrets;
        }
    }

    /**
     * Returns the turret with the given ID, or {@code null} if not found.
     *
     * @param turretId turret identifier
     * @return turret snapshot, or {@code null}
     */
    public Turret get(String turretId) {
        try (Jedis jedis = redis.getResource()) {
            return readTurret(jedis, turretId);
        }
    }

    /**
     * Sets the operational status of a turret.
     *
     * @param turretId turret identifier
     * @param status   new status to write
     */
    public void setStatus(String turretId, TurretStatus status) {
        try (Jedis jedis = redis.getResource()) {
            jedis.hset(turretKey(turretId), "status", status.name());
        }
    }

    /**
     * Increments turret load when a minion is provisioned.
     *
     * @param turretId   turret that received the new minion
     * @param minionId   ID of the provisioned minion (added to the turret's minion set)
     * @param minionRamMb RAM allocated to the new minion
     */
    public void onMinionProvisioned(String turretId, String minionId, int minionRamMb) {
        try (Jedis jedis = redis.getResource()) {
            jedis.hincrBy(turretKey(turretId), "currentMinions", 1);
            jedis.hincrBy(turretKey(turretId), "usedRamMb", minionRamMb);
            jedis.sadd(turretKey(turretId) + ":minions", minionId);
        }
    }

    /**
     * Decrements turret load when a minion dies.
     *
     * @param turretId   turret that hosted the dead minion
     * @param minionId   ID of the dead minion
     * @param minionRamMb RAM that was allocated to the dead minion
     */
    public void onMinionDead(String turretId, String minionId, int minionRamMb) {
        try (Jedis jedis = redis.getResource()) {
            jedis.hincrBy(turretKey(turretId), "currentMinions", -1);
            jedis.hincrBy(turretKey(turretId), "usedRamMb", -minionRamMb);
            jedis.srem(turretKey(turretId) + ":minions", minionId);
        }
    }

    /**
     * Records a heartbeat timestamp for the given turret.
     *
     * @param turretId turret that sent the heartbeat
     */
    public void recordHeartbeat(String turretId) {
        try (Jedis jedis = redis.getResource()) {
            jedis.set(turretKey(turretId) + ":heartbeat",
                    String.valueOf(System.currentTimeMillis()));
        }
    }

    /**
     * Returns the epoch milliseconds of the last heartbeat, or {@code 0} if never seen.
     *
     * @param turretId turret identifier
     * @return epoch ms of last heartbeat
     */
    public long getLastHeartbeat(String turretId) {
        try (Jedis jedis = redis.getResource()) {
            String val = jedis.get(turretKey(turretId) + ":heartbeat");
            return val == null ? 0L : Long.parseLong(val);
        }
    }

    private Turret readTurret(Jedis jedis, String id) {
        Map<String, String> hash = jedis.hgetAll(turretKey(id));
        if (hash == null || hash.isEmpty()) return null;

        Turret t = new Turret();
        t.setTurretId(id);
        t.setHost(hash.getOrDefault("host", ""));
        t.setMaxMinions(parseInt(hash.get("maxMinions"), 0));
        t.setCurrentMinions(parseInt(hash.get("currentMinions"), 0));
        t.setTotalRamMb(parseInt(hash.get("totalRamMb"), 0));
        t.setUsedRamMb(parseInt(hash.get("usedRamMb"), 0));
        String statusStr = hash.getOrDefault("status", TurretStatus.OFFLINE.name());
        try { t.setStatus(TurretStatus.valueOf(statusStr)); }
        catch (IllegalArgumentException e) { t.setStatus(TurretStatus.OFFLINE); }
        String tagsStr = hash.getOrDefault("tags", "");
        t.setTags(tagsStr.isEmpty() ? List.of() : List.of(tagsStr.split(",")));
        return t;
    }

    private static String turretKey(String turretId) {
        return "nexus:turret:" + turretId;
    }

    private static int parseInt(String val, int def) {
        if (val == null) return def;
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) { return def; }
    }
}

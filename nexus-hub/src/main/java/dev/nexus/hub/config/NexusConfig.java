package dev.nexus.hub.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed wrapper around the hub plugin's {@code config.yml}.
 *
 * <p>Load once on plugin enable via {@link #NexusConfig(FileConfiguration)} and inject
 * into components that need it. All getters return pre-parsed values; no raw config
 * access should occur outside this class.
 *
 * @since 1.0.0
 */
public class NexusConfig {

    private final String redisHost;
    private final int redisPort;
    private final String redisPassword;
    private final int redisPoolSize;

    private final String serverNamePrefix;
    private final String templateDirectory;
    private final String workingDirectory;
    private final String startupCommand;

    private final String turretSelectorStrategy;
    private final Map<String, String> tagAffinityMap;

    private final long matchmakingTickIntervalMs;
    private final int queueTimeoutSeconds;
    private final String fillStrategy;
    private final int timedStartWaitSeconds;
    private final int timedStartMinPlayersOverride;

    private final int maxConcurrentTotal;
    private final int bootTimeoutSeconds;
    private final int heartbeatTimeoutSeconds;

    private final String returnServer;
    private final List<String> managedGames;

    private final List<TurretConfig> turrets;
    private final Map<String, MapPoolConfig> mapPools;

    /**
     * Parses all values from the provided Bukkit {@link FileConfiguration}.
     *
     * @param cfg the loaded {@code config.yml}
     */
    public NexusConfig(FileConfiguration cfg) {
        redisHost = cfg.getString("nexus.redis.host", "127.0.0.1");
        redisPort = cfg.getInt("nexus.redis.port", 6379);
        redisPassword = cfg.getString("nexus.redis.password", "");
        redisPoolSize = cfg.getInt("nexus.redis.pool-size", 10);

        serverNamePrefix = cfg.getString("nexus.provisioner.server-name-prefix", "minion-");
        templateDirectory = cfg.getString("nexus.provisioner.template-directory", "/servers/templates");
        workingDirectory = cfg.getString("nexus.provisioner.working-directory", "/servers/minions");
        startupCommand = cfg.getString("nexus.provisioner.startup-command",
                "java -Xms{ramMb}M -Xmx{ramMb}M -jar paper.jar nogui");

        turretSelectorStrategy = cfg.getString("nexus.turret-selector.strategy", "LEAST_LOADED");
        tagAffinityMap = new HashMap<>();
        ConfigurationSection tagSection = cfg.getConfigurationSection("nexus.turret-selector.tag-affinity");
        if (tagSection != null) {
            for (String key : tagSection.getKeys(false)) {
                tagAffinityMap.put(key, tagSection.getString(key));
            }
        }

        matchmakingTickIntervalMs = cfg.getLong("nexus.matchmaking.tick-interval-ms", 2000);
        queueTimeoutSeconds = cfg.getInt("nexus.matchmaking.queue-timeout-seconds", 120);
        fillStrategy = cfg.getString("nexus.matchmaking.fill-strategy", "FILL_THEN_START");
        timedStartWaitSeconds = cfg.getInt("nexus.matchmaking.timed-start.wait-seconds", 30);
        timedStartMinPlayersOverride = cfg.getInt("nexus.matchmaking.timed-start.min-players-override", 4);

        maxConcurrentTotal = cfg.getInt("nexus.minion.max-concurrent-total", 100);
        bootTimeoutSeconds = cfg.getInt("nexus.minion.boot-timeout-seconds", 45);
        heartbeatTimeoutSeconds = cfg.getInt("nexus.minion.heartbeat-timeout-seconds", 30);

        returnServer = cfg.getString("nexus.hub.return-server", "hub");
        managedGames = cfg.getStringList("nexus.hub.managed-games");

        turrets = new ArrayList<>();
        List<?> turretList = cfg.getList("nexus.turrets");
        if (turretList != null) {
            for (Object entry : turretList) {
                if (entry instanceof Map<?, ?> map) {
                    String id = (String) map.get("id");
                    String host = (String) map.get("host");
                    int maxMinions = toInt(map.get("max-minions"), 20);
                    int totalRamMb = toInt(map.get("total-ram-mb"), 4096);
                    Object apiKeyObj = map.get("api-key");
                    String apiKey = apiKeyObj instanceof String s ? s : "";
                    Object tagsObj = map.get("tags");
                    @SuppressWarnings("unchecked")
                    List<String> tags = tagsObj instanceof List<?> ? (List<String>) tagsObj : List.of("all");
                    turrets.add(new TurretConfig(id, host, maxMinions, totalRamMb, apiKey, tags));
                }
            }
        }

        mapPools = new HashMap<>();
        ConfigurationSection poolsSection = cfg.getConfigurationSection("nexus.map-pools");
        if (poolsSection != null) {
            for (String poolKey : poolsSection.getKeys(false)) {
                String rotStr = poolsSection.getString(poolKey + ".rotation", "RANDOM");
                MapPoolConfig.Rotation rot;
                try { rot = MapPoolConfig.Rotation.valueOf(rotStr); }
                catch (IllegalArgumentException e) { rot = MapPoolConfig.Rotation.RANDOM; }
                List<String> maps = poolsSection.getStringList(poolKey + ".maps");
                mapPools.put(poolKey, new MapPoolConfig(poolKey, rot, maps));
            }
        }
    }

    private static int toInt(Object value, int def) {
        if (value instanceof Number n) return n.intValue();
        return def;
    }

    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public String getRedisPassword() { return redisPassword; }
    public int getRedisPoolSize() { return redisPoolSize; }

    public String getServerNamePrefix() { return serverNamePrefix; }
    public String getTemplateDirectory() { return templateDirectory; }
    public String getWorkingDirectory() { return workingDirectory; }
    public String getStartupCommand() { return startupCommand; }

    public String getTurretSelectorStrategy() { return turretSelectorStrategy; }
    public Map<String, String> getTagAffinityMap() { return tagAffinityMap; }

    public long getMatchmakingTickIntervalMs() { return matchmakingTickIntervalMs; }
    public int getQueueTimeoutSeconds() { return queueTimeoutSeconds; }
    public String getFillStrategy() { return fillStrategy; }
    public int getTimedStartWaitSeconds() { return timedStartWaitSeconds; }
    public int getTimedStartMinPlayersOverride() { return timedStartMinPlayersOverride; }

    public int getMaxConcurrentTotal() { return maxConcurrentTotal; }
    public int getBootTimeoutSeconds() { return bootTimeoutSeconds; }
    public int getHeartbeatTimeoutSeconds() { return heartbeatTimeoutSeconds; }

    public String getReturnServer() { return returnServer; }

    /**
     * Returns the list of game IDs this hub is configured to manage.
     * An empty list signals that this hub manages all registered games.
     *
     * @return list of game ID strings; never null
     * @since 1.0.0
     */
    public List<String> getManagedGames() { return managedGames; }

    public List<TurretConfig> getTurrets() { return turrets; }
    public Map<String, MapPoolConfig> getMapPools() { return mapPools; }
}

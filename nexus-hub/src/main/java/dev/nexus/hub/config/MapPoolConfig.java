package dev.nexus.hub.config;

import java.util.List;

/**
 * Configuration for a map pool, loaded from {@code config.yml}.
 *
 * <p>Stub — map rotation is not wired until Phase 4.
 *
 * @since 1.0.0
 */
public class MapPoolConfig {

    /** Map rotation strategies. */
    public enum Rotation { RANDOM, ROUND_ROBIN, VOTE }

    private final String poolKey;
    private final Rotation rotation;
    private final List<String> maps;

    /**
     * @param poolKey  key matching the pool name in config (e.g. {@code "default"})
     * @param rotation strategy used to pick a map from this pool
     * @param maps     list of map names in this pool
     */
    public MapPoolConfig(String poolKey, Rotation rotation, List<String> maps) {
        this.poolKey = poolKey;
        this.rotation = rotation;
        this.maps = maps;
    }

    public String getPoolKey() { return poolKey; }
    public Rotation getRotation() { return rotation; }
    public List<String> getMaps() { return maps; }
}

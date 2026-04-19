package dev.nexus.hub.config;

import java.util.List;

/**
 * Immutable configuration for a single turret host, loaded from {@code config.yml}.
 *
 * @since 1.0.0
 */
public class TurretConfig {

    private final String id;
    private final String host;
    private final int maxMinions;
    private final int totalRamMb;
    private final String apiKey;
    private final List<String> tags;

    /**
     * @param id         unique turret identifier (e.g. {@code "turret-nl-1"})
     * @param host       IP address or hostname
     * @param maxMinions hard cap on concurrent minions regardless of RAM
     * @param totalRamMb total RAM available for minions on this machine
     * @param apiKey     API key for the panel managing this turret
     * @param tags       operator labels used for tag-affinity turret selection
     */
    public TurretConfig(String id, String host, int maxMinions, int totalRamMb,
                        String apiKey, List<String> tags) {
        this.id = id;
        this.host = host;
        this.maxMinions = maxMinions;
        this.totalRamMb = totalRamMb;
        this.apiKey = apiKey;
        this.tags = tags;
    }

    public String getId() { return id; }
    public String getHost() { return host; }
    public int getMaxMinions() { return maxMinions; }
    public int getTotalRamMb() { return totalRamMb; }
    public String getApiKey() { return apiKey; }
    public List<String> getTags() { return tags; }
}

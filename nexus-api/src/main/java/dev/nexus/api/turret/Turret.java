package dev.nexus.api.turret;

import java.util.List;

/**
 * Represents a physical or virtual host machine capable of running minion server processes.
 *
 * <p>Static configuration (host, max capacity, RAM, tags) is defined in {@code config.yml}.
 * Dynamic load ({@code currentMinions}, {@code usedRamMb}) is tracked atomically in Redis
 * under {@code nexus:turret:{turretId}}.
 *
 * @since 1.0.0
 */
public class Turret {

    private String turretId;
    private String host;
    private int maxMinions;
    private int currentMinions;
    private int totalRamMb;
    private int usedRamMb;
    private TurretStatus status;
    private List<String> tags;

    public Turret() {}

    /**
     * @param turretId      unique identifier (e.g. {@code "turret-nl-1"})
     * @param host          IP address or hostname of this machine
     * @param maxMinions    hard cap on concurrent minions regardless of RAM
     * @param currentMinions number of minions currently running on this turret
     * @param totalRamMb    total RAM available for minions, as declared by the operator
     * @param usedRamMb     RAM currently allocated to running minions
     * @param status        operational status
     * @param tags          operator-defined labels used by tag-affinity turret selection
     */
    public Turret(String turretId, String host, int maxMinions, int currentMinions,
                  int totalRamMb, int usedRamMb, TurretStatus status, List<String> tags) {
        this.turretId = turretId;
        this.host = host;
        this.maxMinions = maxMinions;
        this.currentMinions = currentMinions;
        this.totalRamMb = totalRamMb;
        this.usedRamMb = usedRamMb;
        this.status = status;
        this.tags = tags;
    }

    public String getTurretId() { return turretId; }
    public void setTurretId(String turretId) { this.turretId = turretId; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getMaxMinions() { return maxMinions; }
    public void setMaxMinions(int maxMinions) { this.maxMinions = maxMinions; }

    public int getCurrentMinions() { return currentMinions; }
    public void setCurrentMinions(int currentMinions) { this.currentMinions = currentMinions; }

    public int getTotalRamMb() { return totalRamMb; }
    public void setTotalRamMb(int totalRamMb) { this.totalRamMb = totalRamMb; }

    public int getUsedRamMb() { return usedRamMb; }
    public void setUsedRamMb(int usedRamMb) { this.usedRamMb = usedRamMb; }

    public TurretStatus getStatus() { return status; }
    public void setStatus(TurretStatus status) { this.status = status; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    /** @return remaining minion slots ({@code maxMinions - currentMinions}) */
    public int getFreeSlots() { return maxMinions - currentMinions; }

    /** @return remaining RAM in megabytes ({@code totalRamMb - usedRamMb}) */
    public int getFreeRamMb() { return totalRamMb - usedRamMb; }

    @Override
    public String toString() {
        return "Turret{id=" + turretId + ", status=" + status
                + ", minions=" + currentMinions + "/" + maxMinions
                + ", ram=" + usedRamMb + "/" + totalRamMb + "MB}";
    }
}

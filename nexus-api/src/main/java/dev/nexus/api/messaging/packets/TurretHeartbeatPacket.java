package dev.nexus.api.messaging.packets;

import dev.nexus.api.messaging.NexusPacket;
import dev.nexus.api.messaging.PacketType;

/**
 * Periodic health signal sent by the turret agent process to the hub every 10 seconds.
 *
 * <p>The hub updates the turret's last-seen timestamp in Redis on receipt. If no heartbeat
 * arrives within 30 seconds the hub marks the turret {@code OFFLINE}.
 *
 * @since 1.0.0
 */
public class TurretHeartbeatPacket extends NexusPacket {

    private String turretId;
    private int currentMinions;
    private double cpuPercent;
    private double memPercent;

    public TurretHeartbeatPacket() { super(PacketType.TURRET_HEARTBEAT); }

    public TurretHeartbeatPacket(String turretId, int currentMinions, double cpuPercent, double memPercent) {
        super(PacketType.TURRET_HEARTBEAT);
        this.turretId = turretId;
        this.currentMinions = currentMinions;
        this.cpuPercent = cpuPercent;
        this.memPercent = memPercent;
    }

    public String getTurretId() { return turretId; }
    public int getCurrentMinions() { return currentMinions; }
    public double getCpuPercent() { return cpuPercent; }
    public double getMemPercent() { return memPercent; }
}

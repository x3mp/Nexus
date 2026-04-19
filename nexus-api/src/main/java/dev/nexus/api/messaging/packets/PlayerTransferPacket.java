package dev.nexus.api.messaging.packets;

import dev.nexus.api.messaging.NexusPacket;
import dev.nexus.api.messaging.PacketType;

import java.util.UUID;

/**
 * Sent from hub to Velocity via Redis pub/sub to transfer a player to a target server.
 *
 * @since 1.0.0
 */
public class PlayerTransferPacket extends NexusPacket {

    private UUID playerUUID;
    private String targetServerName;

    public PlayerTransferPacket() { super(PacketType.PLAYER_TRANSFER); }

    public PlayerTransferPacket(UUID playerUUID, String targetServerName) {
        super(PacketType.PLAYER_TRANSFER);
        this.playerUUID = playerUUID;
        this.targetServerName = targetServerName;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public String getTargetServerName() { return targetServerName; }
}

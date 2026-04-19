package dev.nexus.api.messaging.packets;

import dev.nexus.api.messaging.NexusPacket;
import dev.nexus.api.messaging.PacketType;

import java.util.UUID;

/**
 * Sent from hub to a queued player's action bar to show queue position and wait estimate.
 *
 * @since 1.0.0
 */
public class QueueStatusPacket extends NexusPacket {

    private UUID playerUUID;
    private String gameId;
    private int position;
    private int estimatedWaitSeconds;

    public QueueStatusPacket() { super(PacketType.QUEUE_STATUS); }

    public QueueStatusPacket(UUID playerUUID, String gameId, int position, int estimatedWaitSeconds) {
        super(PacketType.QUEUE_STATUS);
        this.playerUUID = playerUUID;
        this.gameId = gameId;
        this.position = position;
        this.estimatedWaitSeconds = estimatedWaitSeconds;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public String getGameId() { return gameId; }
    public int getPosition() { return position; }
    public int getEstimatedWaitSeconds() { return estimatedWaitSeconds; }
}

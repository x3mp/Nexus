package dev.nexus.api.messaging.packets;

import dev.nexus.api.messaging.NexusPacket;
import dev.nexus.api.messaging.PacketType;

import java.util.UUID;

/**
 * Sent from a minion to the hub to return a player to the hub server.
 *
 * @since 1.0.0
 */
public class PlayerReturnPacket extends NexusPacket {

    private UUID playerUUID;

    public PlayerReturnPacket() { super(PacketType.PLAYER_RETURN); }

    public PlayerReturnPacket(UUID playerUUID) {
        super(PacketType.PLAYER_RETURN);
        this.playerUUID = playerUUID;
    }

    public UUID getPlayerUUID() { return playerUUID; }
}

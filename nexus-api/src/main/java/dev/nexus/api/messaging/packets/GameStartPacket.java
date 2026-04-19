package dev.nexus.api.messaging.packets;

import dev.nexus.api.messaging.NexusPacket;
import dev.nexus.api.messaging.PacketType;

import java.util.List;
import java.util.UUID;

/**
 * Sent from hub to a minion to start a match with the given players and map.
 *
 * @since 1.0.0
 */
public class GameStartPacket extends NexusPacket {

    private String minionId;
    private List<UUID> playerUUIDs;
    private String mapName;

    public GameStartPacket() { super(PacketType.GAME_START); }

    public GameStartPacket(String minionId, List<UUID> playerUUIDs, String mapName) {
        super(PacketType.GAME_START);
        this.minionId = minionId;
        this.playerUUIDs = playerUUIDs;
        this.mapName = mapName;
    }

    public String getMinionId() { return minionId; }
    public List<UUID> getPlayerUUIDs() { return playerUUIDs; }
    public String getMapName() { return mapName; }
}

package dev.nexus.api.messaging.packets;

import dev.nexus.api.messaging.NexusPacket;
import dev.nexus.api.messaging.PacketType;

import java.util.UUID;

/**
 * Sent from a minion to the hub when a match concludes.
 *
 * @since 1.0.0
 */
public class GameEndPacket extends NexusPacket {

    private String minionId;
    private UUID winnerUUID;
    private String teamId;
    private String statsJson;

    public GameEndPacket() { super(PacketType.GAME_END); }

    public GameEndPacket(String minionId, UUID winnerUUID, String teamId, String statsJson) {
        super(PacketType.GAME_END);
        this.minionId = minionId;
        this.winnerUUID = winnerUUID;
        this.teamId = teamId;
        this.statsJson = statsJson;
    }

    public String getMinionId() { return minionId; }
    public UUID getWinnerUUID() { return winnerUUID; }
    public String getTeamId() { return teamId; }
    public String getStatsJson() { return statsJson; }
}

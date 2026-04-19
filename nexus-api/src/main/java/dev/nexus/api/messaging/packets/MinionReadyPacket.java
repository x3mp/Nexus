package dev.nexus.api.messaging.packets;

import dev.nexus.api.messaging.NexusPacket;
import dev.nexus.api.messaging.PacketType;

/**
 * Sent by a minion to the hub when its process is ready to receive players.
 *
 * @since 1.0.0
 */
public class MinionReadyPacket extends NexusPacket {

    private String minionId;
    private String serverName;
    private String host;
    private int port;

    public MinionReadyPacket() { super(PacketType.MINION_READY); }

    public MinionReadyPacket(String minionId, String serverName, String host, int port) {
        super(PacketType.MINION_READY);
        this.minionId = minionId;
        this.serverName = serverName;
        this.host = host;
        this.port = port;
    }

    public String getMinionId() { return minionId; }
    public String getServerName() { return serverName; }
    public String getHost() { return host; }
    public int getPort() { return port; }
}

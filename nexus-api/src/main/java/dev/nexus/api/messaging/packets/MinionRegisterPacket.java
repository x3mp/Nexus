package dev.nexus.api.messaging.packets;

import dev.nexus.api.messaging.NexusPacket;
import dev.nexus.api.messaging.PacketType;

/**
 * Sent from hub to Velocity via Redis pub/sub to register a new minion server with the proxy.
 *
 * @since 1.0.0
 */
public class MinionRegisterPacket extends NexusPacket {

    private String serverName;
    private String host;
    private int port;

    public MinionRegisterPacket() { super(PacketType.MINION_REGISTER); }

    public MinionRegisterPacket(String serverName, String host, int port) {
        super(PacketType.MINION_REGISTER);
        this.serverName = serverName;
        this.host = host;
        this.port = port;
    }

    public String getServerName() { return serverName; }
    public String getHost() { return host; }
    public int getPort() { return port; }
}

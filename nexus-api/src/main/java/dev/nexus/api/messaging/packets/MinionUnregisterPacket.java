package dev.nexus.api.messaging.packets;

import dev.nexus.api.messaging.NexusPacket;
import dev.nexus.api.messaging.PacketType;

/**
 * Sent from hub to Velocity via Redis pub/sub to remove a dead minion server from the proxy.
 *
 * @since 1.0.0
 */
public class MinionUnregisterPacket extends NexusPacket {

    private String serverName;

    public MinionUnregisterPacket() { super(PacketType.MINION_UNREGISTER); }

    public MinionUnregisterPacket(String serverName) {
        super(PacketType.MINION_UNREGISTER);
        this.serverName = serverName;
    }

    public String getServerName() { return serverName; }
}

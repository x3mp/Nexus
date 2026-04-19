package dev.nexus.api.messaging;

/**
 * Plugin messaging channel and Redis pub/sub channel name constants.
 *
 * @since 1.0.0
 */
public final class NexusChannel {

    /** Plugin messaging channel used between minions and hub. */
    public static final String MAIN = "nexus:main";

    /** Redis pub/sub channel used to push packets from hub to Velocity. */
    public static final String VELOCITY = "nexus:velocity";

    private NexusChannel() {}
}

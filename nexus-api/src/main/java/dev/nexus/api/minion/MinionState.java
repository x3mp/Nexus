package dev.nexus.api.minion;

/**
 * Lifecycle states of a minion server process.
 *
 * <p>State transitions are written atomically to Redis under
 * {@code nexus:minion:{minionId}:state}.
 *
 * @since 1.0.0
 */
public enum MinionState {

    /** Turret is starting the process; players are held in the hub lobby. */
    BOOTING,

    /** Minion is online and ready; pre-game countdown may be running. */
    WAITING,

    /** Match is live; no new players accepted unless {@code canJoinMidGame} is true. */
    IN_GAME,

    /** Game over; players are being returned to hub. */
    ENDING,

    /** Process killed or world unloaded; turret slot has been freed. */
    DEAD
}

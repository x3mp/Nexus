package dev.nexus.api.messaging;

/**
 * All packet types exchanged between Nexus components.
 *
 * @since 1.0.0
 */
public enum PacketType {

    /** Minion → Hub: minion process is ready; carries address and port. */
    MINION_READY,

    /** Hub → Velocity: register a new minion server with the proxy. */
    MINION_REGISTER,

    /** Hub → Velocity: unregister a dead minion server from the proxy. */
    MINION_UNREGISTER,

    /** Hub → Minion: start the match with the assigned players and map. */
    GAME_START,

    /** Minion → Hub: match has ended; carries winner and statistics. */
    GAME_END,

    /** Hub → Velocity: transfer a player to a specific server. */
    PLAYER_TRANSFER,

    /** Minion → Hub: return a player to the hub. */
    PLAYER_RETURN,

    /** Hub → Player: queue position and estimated wait time for action bar. */
    QUEUE_STATUS,

    /** Turret agent → Hub: periodic health signal from a turret host. */
    TURRET_HEARTBEAT
}

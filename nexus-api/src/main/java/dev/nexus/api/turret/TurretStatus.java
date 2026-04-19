package dev.nexus.api.turret;

/**
 * Operational status of a turret host.
 *
 * @since 1.0.0
 */
public enum TurretStatus {

    /** Turret is healthy and accepting new minions. */
    ONLINE,

    /**
     * Turret will not receive new minions but existing ones finish normally.
     * Used for maintenance without hard-killing live games.
     */
    DRAINING,

    /** Turret is unreachable or manually disabled; no minions routed to it. */
    OFFLINE
}

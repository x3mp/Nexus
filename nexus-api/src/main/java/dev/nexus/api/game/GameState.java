package dev.nexus.api.game;

/**
 * Represents the high-level state of a game match running on a minion.
 *
 * @since 1.0.0
 */
public enum GameState {

    /** Players are in the lobby waiting for the match to fill. */
    WAITING,

    /** Match countdown is running; the game is about to begin. */
    STARTING,

    /** Match is live and active. */
    IN_PROGRESS,

    /** Match has concluded; players are being returned to hub. */
    ENDING
}

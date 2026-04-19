package dev.nexus.api.game;

import dev.nexus.api.minion.Minion;

import java.util.List;
import java.util.UUID;

/**
 * Contract that every Nexus-compatible gamemode must implement.
 *
 * <p>Implementing classes must also be annotated with {@link GameMeta} to declare
 * their ID, player counts, and resource requirements. Nexus never touches game
 * logic directly — it only calls the lifecycle hooks defined here.
 *
 * <p>Example registration in a game plugin's {@code onEnable}:
 * <pre>{@code
 * NexusAPI.registerGame(new RevenantGame());
 * }</pre>
 *
 * <p>Game plugins only need {@code nexus-api} as a compile-time dependency.
 *
 * @since 1.0.0
 */
public interface NexusGame {

    /**
     * Called by Nexus when a minion is ready and players have been assigned.
     *
     * <p>Implementations should set up the map, initialise game state, and start
     * any countdowns here.
     *
     * @param minion  the minion instance that will host this match
     * @param players UUIDs of all players assigned to this minion
     */
    void onMinionStart(Minion minion, List<UUID> players);

    /**
     * Called when the game ends. Nexus retires the minion after this method returns.
     *
     * <p>Implementations should persist results, clean up world state, and notify
     * players of the outcome here.
     *
     * @param minion the minion instance that hosted the match
     * @param result outcome data including winner and statistics
     */
    void onMinionEnd(Minion minion, GameEndResult result);

    /**
     * Whether players may join this minion after the match has started.
     *
     * @return {@code true} if mid-game joins are allowed
     */
    boolean canJoinMidGame();
}

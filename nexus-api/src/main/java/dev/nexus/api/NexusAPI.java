package dev.nexus.api;

import dev.nexus.api.game.NexusGame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Static service locator for Nexus game registration.
 *
 * <p>Game plugins call {@link #registerGame(NexusGame)} in their {@code onEnable}.
 * nexus-hub drains this list on startup (1-tick delayed) and moves entries into
 * its internal {@code GameRegistry}.
 *
 * <p>Game plugins only need {@code nexus-api} as a compile-time dependency —
 * no dependency on {@code nexus-hub} required.
 */
public class NexusAPI {

    private static final List<NexusGame> games = new ArrayList<>();

    private NexusAPI() {}

    /**
     * Registers a game to be picked up by nexus-hub on startup.
     *
     * @param game the game implementation to register
     */
    public static void registerGame(NexusGame game) {
        games.add(game);
    }

    /**
     * Returns all games registered via {@link #registerGame(NexusGame)}.
     *
     * @return unmodifiable view of registered games
     */
    public static List<NexusGame> getRegisteredGames() {
        return Collections.unmodifiableList(games);
    }
}

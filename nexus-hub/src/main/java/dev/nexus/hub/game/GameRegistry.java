package dev.nexus.hub.game;

import dev.nexus.api.game.GameMeta;
import dev.nexus.api.game.NexusGame;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Registry of all {@link NexusGame} implementations known to this hub.
 *
 * <p>Game plugins register themselves on their {@code onEnable} by calling
 * {@link #register(NexusGame)}. The registry validates the {@link GameMeta} annotation
 * at registration time and rejects games that lack it.
 *
 * @since 1.0.0
 */
public class GameRegistry {

    private final Map<String, NexusGame> games = new ConcurrentHashMap<>();
    private final Logger logger;

    /**
     * @param logger plugin logger used for registration diagnostics
     */
    public GameRegistry(Logger logger) {
        this.logger = logger;
    }

    /**
     * Registers a game with this hub.
     *
     * @param game the game implementation to register
     * @throws IllegalArgumentException if the class lacks a {@link GameMeta} annotation
     */
    public void register(NexusGame game) {
        GameMeta meta = game.getClass().getAnnotation(GameMeta.class);
        if (meta == null) {
            throw new IllegalArgumentException(
                    game.getClass().getName() + " must be annotated with @GameMeta");
        }
        games.put(meta.id(), game);
        logger.info("Registered game: " + meta.displayName() + " (id=" + meta.id()
                + ", players=" + meta.minPlayers() + "-" + meta.maxPlayers() + ")");
    }

    /**
     * Returns the game registered under the given ID, or {@code null} if not found.
     *
     * @param gameId the {@link GameMeta#id()} to look up
     * @return the registered game, or {@code null}
     */
    public NexusGame get(String gameId) {
        return games.get(gameId);
    }

    /**
     * Returns the {@link GameMeta} annotation for a registered game.
     *
     * @param gameId the game ID to look up
     * @return the annotation, or {@code null} if the game is not registered
     */
    public GameMeta getMeta(String gameId) {
        NexusGame game = games.get(gameId);
        return game == null ? null : game.getClass().getAnnotation(GameMeta.class);
    }

    /**
     * @return an unmodifiable view of all registered games, keyed by game ID
     */
    public Collection<NexusGame> getAll() {
        return Collections.unmodifiableCollection(games.values());
    }

    /**
     * @return an unmodifiable view of all registered game IDs
     */
    public Collection<String> getGameIds() {
        return Collections.unmodifiableSet(games.keySet());
    }
}

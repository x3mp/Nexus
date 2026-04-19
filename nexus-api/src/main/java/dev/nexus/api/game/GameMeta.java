package dev.nexus.api.game;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares metadata for a {@link NexusGame} implementation.
 *
 * <p>Nexus reads this annotation at registration time to derive queue thresholds,
 * resource requirements, and map pool assignments. Every {@link NexusGame} class
 * must be annotated with {@code @GameMeta}.
 *
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GameMeta {

    /** Unique identifier used as the Redis key segment (e.g. {@code "revenant"}). */
    String id();

    /** Human-readable name shown in GUIs and logs. */
    String displayName();

    /** Minimum players required in queue before a match is started. */
    int minPlayers();

    /** Maximum players allowed per minion instance. */
    int maxPlayers();

    /** Minion is force-killed after this many seconds regardless of game state. */
    int instanceTimeoutSeconds();

    /** Map pool key referencing an entry in the hub {@code config.yml}. */
    String mapPool() default "default";

    /** Whether the game supports team play. */
    boolean allowTeams() default false;

    /** Number of players per team. Ignored when {@link #allowTeams()} is {@code false}. */
    int teamSize() default 1;

    /**
     * If {@code true}, the minion runs multiple sequential matches before being retired.
     * The game plugin is responsible for resetting state between matches.
     */
    boolean reuseMinion() default false;

    /** RAM in megabytes allocated per minion. Used by the provisioner and turret selector. */
    int minionRamMb() default 1024;

    /** Disk space in megabytes allocated per minion working directory. */
    int minionDiskMb() default 512;
}

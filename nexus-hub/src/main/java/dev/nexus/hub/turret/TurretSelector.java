package dev.nexus.hub.turret;

import dev.nexus.api.turret.Turret;

import java.util.List;

/**
 * Strategy interface for selecting which turret should receive a new minion.
 *
 * <p>Implementations must apply the standard eligibility check before selecting:
 * turret must be {@link dev.nexus.api.turret.TurretStatus#ONLINE}, have free slots
 * ({@code currentMinions < maxMinions}), and have enough RAM headroom
 * ({@code usedRamMb + minionRamMb <= totalRamMb}).
 *
 * @since 1.0.0
 */
public interface TurretSelector {

    /**
     * Selects the best eligible turret for a new minion of the given game.
     *
     * @param gameId      game type being provisioned (used for tag-affinity strategies)
     * @param available   all turrets currently tracked in Redis
     * @param minionRamMb RAM the new minion requires, from {@link dev.nexus.api.game.GameMeta#minionRamMb()}
     * @return the chosen turret, or {@code null} if no eligible turret exists
     */
    Turret select(String gameId, List<Turret> available, int minionRamMb);
}

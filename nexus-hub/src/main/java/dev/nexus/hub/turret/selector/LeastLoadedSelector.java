package dev.nexus.hub.turret.selector;

import dev.nexus.api.turret.Turret;
import dev.nexus.api.turret.TurretStatus;
import dev.nexus.hub.turret.TurretSelector;

import java.util.Comparator;
import java.util.List;

/**
 * Picks the turret with the most remaining minion slots.
 *
 * <p>Eligibility requires all of:
 * <ul>
 *   <li>Status is {@link TurretStatus#ONLINE}
 *   <li>{@code currentMinions < maxMinions} (free slot exists)
 *   <li>{@code usedRamMb + minionRamMb <= totalRamMb} (enough RAM headroom)
 * </ul>
 *
 * @since 1.0.0
 */
public class LeastLoadedSelector implements TurretSelector {

    @Override
    public Turret select(String gameId, List<Turret> available, int minionRamMb) {
        return available.stream()
                .filter(t -> t.getStatus() == TurretStatus.ONLINE)
                .filter(t -> t.getCurrentMinions() < t.getMaxMinions())
                .filter(t -> t.getUsedRamMb() + minionRamMb <= t.getTotalRamMb())
                .max(Comparator.comparingInt(Turret::getFreeSlots))
                .orElse(null);
    }
}

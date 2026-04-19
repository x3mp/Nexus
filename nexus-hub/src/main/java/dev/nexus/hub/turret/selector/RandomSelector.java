package dev.nexus.hub.turret.selector;

import dev.nexus.api.turret.Turret;
import dev.nexus.api.turret.TurretStatus;
import dev.nexus.hub.turret.TurretSelector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Selects a random eligible turret.
 *
 * <p>Applies the same eligibility check as {@link LeastLoadedSelector}:
 * ONLINE status, free minion slot, and sufficient RAM headroom.
 *
 * @since 1.0.0
 */
public class RandomSelector implements TurretSelector {

    @Override
    public Turret select(String gameId, List<Turret> available, int minionRamMb) {
        List<Turret> eligible = available.stream()
                .filter(t -> t.getStatus() == TurretStatus.ONLINE)
                .filter(t -> t.getCurrentMinions() < t.getMaxMinions())
                .filter(t -> t.getUsedRamMb() + minionRamMb <= t.getTotalRamMb())
                .collect(Collectors.toList());

        if (eligible.isEmpty()) return null;
        return eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
    }
}

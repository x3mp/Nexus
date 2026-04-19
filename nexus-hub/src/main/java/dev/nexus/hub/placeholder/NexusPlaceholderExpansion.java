package dev.nexus.hub.placeholder;

import dev.nexus.api.turret.TurretStatus;
import dev.nexus.hub.minion.MinionRegistry;
import dev.nexus.hub.queue.QueueManager;
import dev.nexus.hub.turret.TurretRegistry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Registers Nexus {@code %nexus_*%} placeholders with PlaceholderAPI.
 *
 * <p>Only registered if PlaceholderAPI is present on the server.
 * Placeholders:
 * <ul>
 *   <li>{@code %nexus_queue_size_<gameId>%} — players currently queued
 *   <li>{@code %nexus_minions_<gameId>%} — live minion count for the game
 *   <li>{@code %nexus_position_<gameId>%} — player's queue position (0 = not queued)
 *   <li>{@code %nexus_turrets_online%} — number of ONLINE turrets
 *   <li>{@code %nexus_turrets_capacity%} — total maxMinions across all ONLINE turrets
 *   <li>{@code %nexus_turrets_used%} — total running minions across all turrets
 * </ul>
 *
 * @since 1.0.0
 */
public class NexusPlaceholderExpansion extends PlaceholderExpansion {

    private final QueueManager queueManager;
    private final MinionRegistry minionRegistry;
    private final TurretRegistry turretRegistry;

    /**
     * @param queueManager   for queue size and position placeholders
     * @param minionRegistry for live minion count placeholders
     * @param turretRegistry for turret health placeholders
     */
    public NexusPlaceholderExpansion(QueueManager queueManager, MinionRegistry minionRegistry,
                                     TurretRegistry turretRegistry) {
        this.queueManager = queueManager;
        this.minionRegistry = minionRegistry;
        this.turretRegistry = turretRegistry;
    }

    @Override
    public @NotNull String getIdentifier() { return "nexus"; }

    @Override
    public @NotNull String getAuthor() { return "Nexus"; }

    @Override
    public @NotNull String getVersion() { return "1.0.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("queue_size_")) {
            String gameId = params.substring("queue_size_".length());
            return String.valueOf(queueManager.getSize(gameId));
        }
        if (params.startsWith("minions_")) {
            String gameId = params.substring("minions_".length());
            return String.valueOf(minionRegistry.getAll().stream()
                    .filter(m -> m.getGameId().equals(gameId)).count());
        }
        if (params.startsWith("position_") && player != null) {
            String gameId = params.substring("position_".length());
            return String.valueOf(queueManager.getPosition(player.getUniqueId(), gameId));
        }
        if (params.equals("turrets_online")) {
            return String.valueOf(turretRegistry.getAll().stream()
                    .filter(t -> t.getStatus() == TurretStatus.ONLINE).count());
        }
        if (params.equals("turrets_capacity")) {
            return String.valueOf(turretRegistry.getAll().stream()
                    .filter(t -> t.getStatus() == TurretStatus.ONLINE)
                    .mapToInt(dev.nexus.api.turret.Turret::getMaxMinions).sum());
        }
        if (params.equals("turrets_used")) {
            return String.valueOf(turretRegistry.getAll().stream()
                    .mapToInt(dev.nexus.api.turret.Turret::getCurrentMinions).sum());
        }
        return null;
    }
}

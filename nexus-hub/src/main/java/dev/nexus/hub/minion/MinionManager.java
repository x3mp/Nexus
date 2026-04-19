package dev.nexus.hub.minion;

import dev.nexus.api.game.GameEndResult;
import dev.nexus.api.game.GameMeta;
import dev.nexus.api.game.NexusGame;
import dev.nexus.api.minion.Minion;
import dev.nexus.api.minion.MinionState;
import dev.nexus.api.turret.Turret;
import dev.nexus.hub.game.GameRegistry;
import dev.nexus.hub.messaging.HubMessageSender;
import dev.nexus.hub.turret.TurretProvisioner;
import dev.nexus.hub.turret.TurretRegistry;
import dev.nexus.hub.turret.TurretSelector;
import dev.nexus.api.messaging.packets.GameStartPacket;
import dev.nexus.api.messaging.packets.MinionRegisterPacket;
import dev.nexus.api.messaging.packets.MinionUnregisterPacket;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Orchestrates the full minion lifecycle from provisioning to retirement.
 *
 * <p>Flow:
 * <ol>
 *   <li>Matchmaking calls {@link #provision(String, List)} with matched players.
 *   <li>Manager selects a turret, provisions the minion, and registers it in Redis with state BOOTING.
 *   <li>A boot-timeout watchdog is armed; if no {@code MINION_READY} arrives the minion is marked DEAD.
 *   <li>When {@link #onMinionReady(Minion)} is called, the manager registers the server with Velocity
 *       and sends {@code GAME_START} to the minion.
 *   <li>When {@link #onGameEnd(String, GameEndResult)} is called, the manager unregisters the minion
 *       and frees turret load.
 * </ol>
 *
 * @since 1.0.0
 */
public class MinionManager {

    private final GameRegistry gameRegistry;
    private final TurretRegistry turretRegistry;
    private final TurretSelector turretSelector;
    private final TurretProvisioner turretProvisioner;
    private final MinionRegistry minionRegistry;
    private final HubMessageSender messageSender;
    private final int bootTimeoutSeconds;
    private final Logger logger;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * @param gameRegistry      registry of all known games
     * @param turretRegistry    Redis-backed turret registry
     * @param turretSelector    strategy for picking a turret
     * @param turretProvisioner implementation that starts/kills minion processes
     * @param minionRegistry    Redis-backed minion registry
     * @param messageSender     sends packets to other components
     * @param bootTimeoutSeconds seconds before an unresponsive minion is declared DEAD
     * @param logger            plugin logger
     */
    public MinionManager(GameRegistry gameRegistry, TurretRegistry turretRegistry,
                         TurretSelector turretSelector, TurretProvisioner turretProvisioner,
                         MinionRegistry minionRegistry, HubMessageSender messageSender,
                         int bootTimeoutSeconds, Logger logger) {
        this.gameRegistry = gameRegistry;
        this.turretRegistry = turretRegistry;
        this.turretSelector = turretSelector;
        this.turretProvisioner = turretProvisioner;
        this.minionRegistry = minionRegistry;
        this.messageSender = messageSender;
        this.bootTimeoutSeconds = bootTimeoutSeconds;
        this.logger = logger;
    }

    /**
     * Provisions a new minion for the given game and assigns the matched players to it.
     *
     * @param gameId  game type to provision
     * @param players UUIDs of the matched players
     */
    public void provision(String gameId, List<UUID> players) {
        GameMeta meta = gameRegistry.getMeta(gameId);
        if (meta == null) {
            logger.warning("Cannot provision: game '" + gameId + "' not registered");
            return;
        }

        List<Turret> allTurrets = turretRegistry.getAll();
        Turret turret = turretSelector.select(gameId, allTurrets, meta.minionRamMb());
        if (turret == null) {
            logger.warning("No eligible turret for game '" + gameId + "' — all full or no RAM");
            return;
        }

        String mapName = "default"; // Phase 4: replace with MapPoolManager.pick(meta.mapPool())

        Minion minion;
        try {
            minion = turretProvisioner.provision(gameId, turret, mapName);
        } catch (Exception e) {
            logger.severe("Provisioning failed for game '" + gameId + "': " + e.getMessage());
            return;
        }

        minion.setState(MinionState.BOOTING);
        minion.setStartedAt(System.currentTimeMillis());
        minionRegistry.register(minion);
        turretRegistry.onMinionProvisioned(turret.getTurretId(), minion.getMinionId(), meta.minionRamMb());

        for (UUID player : players) {
            minionRegistry.addPlayer(minion.getMinionId(), player);
        }

        logger.info("Provisioned " + minion.getMinionId() + " on " + turret.getTurretId()
                + " for game " + gameId);

        armBootTimeout(minion.getMinionId(), gameId, meta.minionRamMb());
    }

    /**
     * Called by {@link dev.nexus.hub.messaging.HubMessageListener} when a minion sends {@code MINION_READY}.
     *
     * @param minion the ready minion with updated host/port
     */
    public void onMinionReady(Minion minion) {
        minionRegistry.updateState(minion.getMinionId(), MinionState.WAITING);
        messageSender.sendToVelocity(new MinionRegisterPacket(
                minion.getServerName(), minion.getHost(), minion.getPort()));

        List<UUID> players = minionRegistry.getPlayers(minion.getMinionId()).stream()
                .map(UUID::fromString).toList();

        String mapName = minionRegistry.get(minion.getMinionId()).getMapName();
        messageSender.sendToMinion(minion.getServerName(),
                new GameStartPacket(minion.getMinionId(), players, mapName));

        minionRegistry.updateState(minion.getMinionId(), MinionState.IN_GAME);
        logger.info(minion.getMinionId() + " is ready; sent GAME_START with "
                + players.size() + " players");
    }

    /**
     * Called by {@link dev.nexus.hub.messaging.HubMessageListener} when a minion sends {@code GAME_END}.
     *
     * @param minionId ID of the minion that ended
     * @param result   match outcome
     */
    public void onGameEnd(String minionId, GameEndResult result) {
        Minion minion = minionRegistry.get(minionId);
        if (minion == null) return;

        minionRegistry.updateState(minionId, MinionState.ENDING);

        GameMeta meta = gameRegistry.getMeta(minion.getGameId());
        int minionRamMb = meta != null ? meta.minionRamMb() : 0;

        NexusGame game = gameRegistry.get(minion.getGameId());
        if (game != null) {
            try { game.onMinionEnd(minion, result); }
            catch (Exception e) { logger.warning("onMinionEnd threw: " + e.getMessage()); }
        }

        retire(minionId, minion.getTurretId(), minionRamMb);
    }

    /**
     * Shuts down the internal scheduler. Call on plugin disable.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void armBootTimeout(String minionId, String gameId, int minionRamMb) {
        scheduler.schedule(() -> {
            Minion m = minionRegistry.get(minionId);
            if (m != null && m.getState() == MinionState.BOOTING) {
                logger.warning("Boot timeout for " + minionId + " — marking DEAD");
                retire(minionId, m.getTurretId(), minionRamMb);
            }
        }, bootTimeoutSeconds, TimeUnit.SECONDS);
    }

    private void retire(String minionId, String turretId, int minionRamMb) {
        minionRegistry.updateState(minionId, MinionState.DEAD);
        turretRegistry.onMinionDead(turretId, minionId, minionRamMb);
        messageSender.sendToVelocity(new MinionUnregisterPacket(
                minionRegistry.get(minionId) != null
                        ? minionRegistry.get(minionId).getServerName() : minionId));
        minionRegistry.unregister(minionId);

        try { turretProvisioner.kill(minionId); }
        catch (Exception e) { logger.warning("Kill request failed for " + minionId + ": " + e.getMessage()); }

        logger.info("Retired minion " + minionId);
    }
}

package dev.nexus.hub.turret;

import dev.nexus.api.minion.Minion;
import dev.nexus.api.turret.Turret;

/**
 * Contract for provisioning and retiring minion server processes on turrets.
 *
 * <p>Nexus core provides no concrete implementation — operators supply their own
 * implementation and register it via
 * {@link dev.nexus.hub.NexusHubPlugin#setTurretProvisioner(TurretProvisioner)}.
 * For local development a no-op stub can be used.
 *
 * @since 1.0.0
 */
public interface TurretProvisioner {

    /**
     * Provisions a new minion server process on the given turret.
     *
     * <p>The implementation is responsible for copying the map template, launching
     * the process, and returning a {@link Minion} with at least
     * {@code minionId}, {@code serverName}, {@code host}, and {@code port} populated.
     * The minion's state should be set to {@link dev.nexus.api.minion.MinionState#BOOTING}.
     *
     * @param gameId  game type to provision
     * @param turret  target turret host
     * @param mapName map template name to load
     * @return a {@link Minion} descriptor for the newly started process
     * @throws Exception if provisioning fails
     */
    Minion provision(String gameId, Turret turret, String mapName) throws Exception;

    /**
     * Force-kills a running minion process and frees its resources on the turret.
     *
     * @param minionId ID of the minion to kill
     * @throws Exception if the kill request fails
     */
    void kill(String minionId) throws Exception;
}

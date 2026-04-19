package dev.nexus.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Transfers players between servers registered with the Velocity proxy.
 *
 * @since 1.0.0
 */
public class PlayerRouter {

    private final ProxyServer proxy;
    private final Logger logger;

    /**
     * @param proxy  Velocity proxy instance
     * @param logger plugin logger
     */
    public PlayerRouter(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    /**
     * Transfers a player to the given server.
     *
     * <p>If the player is not connected or the server is not registered, the transfer
     * is silently skipped with a warning log.
     *
     * @param playerUUID     UUID of the player to transfer
     * @param targetServerName Velocity server name to transfer to
     */
    public void transfer(UUID playerUUID, String targetServerName) {
        Optional<Player> player = proxy.getPlayer(playerUUID);
        if (player.isEmpty()) {
            logger.warning("Transfer failed: player " + playerUUID + " not connected");
            return;
        }

        Optional<RegisteredServer> server = proxy.getServer(targetServerName);
        if (server.isEmpty()) {
            logger.warning("Transfer failed: server '" + targetServerName + "' not registered");
            return;
        }

        player.get().createConnectionRequest(server.get()).fireAndForget();
        logger.fine("Transferring " + playerUUID + " to " + targetServerName);
    }
}

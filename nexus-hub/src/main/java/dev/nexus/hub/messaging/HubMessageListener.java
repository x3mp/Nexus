package dev.nexus.hub.messaging;

import dev.nexus.api.game.GameEndResult;
import dev.nexus.api.messaging.NexusPacket;
import dev.nexus.api.messaging.PacketType;
import dev.nexus.api.messaging.packets.GameEndPacket;
import dev.nexus.api.messaging.packets.MinionReadyPacket;
import dev.nexus.api.messaging.packets.PlayerReturnPacket;
import dev.nexus.api.messaging.packets.TurretHeartbeatPacket;
import dev.nexus.api.minion.Minion;
import dev.nexus.api.minion.MinionState;
import dev.nexus.hub.minion.MinionManager;
import dev.nexus.hub.minion.MinionRegistry;
import dev.nexus.hub.turret.TurretRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Receives plugin messaging packets from minion servers and dispatches them to the
 * appropriate hub component.
 *
 * <p>Registered on Bukkit's incoming plugin messaging channel {@code nexus:main}.
 *
 * @since 1.0.0
 */
public class HubMessageListener implements PluginMessageListener {

    private final MinionManager minionManager;
    private final MinionRegistry minionRegistry;
    private final TurretRegistry turretRegistry;
    private final String returnServer;
    private final Logger logger;

    /**
     * @param minionManager   handles MINION_READY and GAME_END lifecycle events
     * @param minionRegistry  used to look up minion data on PLAYER_RETURN
     * @param turretRegistry  updated on TURRET_HEARTBEAT
     * @param returnServer    Velocity server name players are sent to after a game ends
     * @param logger          plugin logger
     */
    public HubMessageListener(MinionManager minionManager, MinionRegistry minionRegistry,
                               TurretRegistry turretRegistry, String returnServer, Logger logger) {
        this.minionManager = minionManager;
        this.minionRegistry = minionRegistry;
        this.turretRegistry = turretRegistry;
        this.returnServer = returnServer;
        this.logger = logger;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        String json = new String(message, StandardCharsets.UTF_8);
        PacketType type = NexusPacket.typeOf(json);
        if (type == null) {
            logger.warning("Received unrecognised packet on " + channel + ": " + json);
            return;
        }

        switch (type) {
            case MINION_READY -> handleMinionReady(json);
            case GAME_END -> handleGameEnd(json);
            case PLAYER_RETURN -> handlePlayerReturn(json);
            case TURRET_HEARTBEAT -> handleHeartbeat(json);
            default -> logger.fine("Ignored packet type " + type + " on hub listener");
        }
    }

    private void handleMinionReady(String json) {
        MinionReadyPacket pkt = NexusPacket.fromJson(json, MinionReadyPacket.class);
        Minion minion = minionRegistry.get(pkt.getMinionId());
        if (minion == null) {
            logger.warning("MINION_READY for unknown minion: " + pkt.getMinionId());
            return;
        }
        minion.setHost(pkt.getHost());
        minion.setPort(pkt.getPort());
        minion.setServerName(pkt.getServerName());
        minionManager.onMinionReady(minion);
    }

    private void handleGameEnd(String json) {
        GameEndPacket pkt = NexusPacket.fromJson(json, GameEndPacket.class);
        GameEndResult result = new GameEndResult(pkt.getMinionId(), pkt.getWinnerUUID(),
                pkt.getTeamId(), pkt.getStatsJson());
        minionManager.onGameEnd(pkt.getMinionId(), result);
    }

    private void handlePlayerReturn(String json) {
        PlayerReturnPacket pkt = NexusPacket.fromJson(json, PlayerReturnPacket.class);
        UUID uuid = pkt.getPlayerUUID();
        Player target = Bukkit.getPlayer(uuid);
        if (target != null) {
            // Velocity handles the actual transfer; send PLAYER_TRANSFER via Redis
            // For now we log — the full transfer flow is in MinionManager
            logger.fine("Player return requested for " + uuid);
        }
    }

    private void handleHeartbeat(String json) {
        TurretHeartbeatPacket pkt = NexusPacket.fromJson(json, TurretHeartbeatPacket.class);
        turretRegistry.recordHeartbeat(pkt.getTurretId());
        logger.fine("Heartbeat from turret " + pkt.getTurretId()
                + " — minions=" + pkt.getCurrentMinions()
                + " cpu=" + pkt.getCpuPercent() + "%");
    }
}

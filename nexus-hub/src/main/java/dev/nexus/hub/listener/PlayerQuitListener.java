package dev.nexus.hub.listener;

import dev.nexus.hub.queue.QueueManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Removes a player from their queue when they disconnect from the hub.
 *
 * @since 1.0.0
 */
public class PlayerQuitListener implements Listener {

    private final QueueManager queueManager;

    /**
     * @param queueManager used to remove the disconnecting player from their queue
     */
    public PlayerQuitListener(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        queueManager.remove(event.getPlayer().getUniqueId());
    }
}

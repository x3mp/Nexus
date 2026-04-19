package dev.nexus.api.queue;

import java.util.UUID;

/**
 * A single player's position in a game queue.
 *
 * <p>Serialised as JSON and stored in Redis under {@code nexus:queue:{gameId}}.
 * Priority players are additionally tracked in the sorted set
 * {@code nexus:queue:{gameId}:priority}.
 *
 * @since 1.0.0
 */
public class QueueEntry {

    private UUID playerUUID;
    private String playerName;
    private String gameId;
    private long joinedAt;
    private int priority;

    public QueueEntry() {}

    /**
     * @param playerUUID UUID of the queued player
     * @param playerName display name of the queued player
     * @param gameId     game the player is queued for
     * @param joinedAt   epoch milliseconds when the player joined the queue
     * @param priority   higher values are matched first; 0 for regular players
     */
    public QueueEntry(UUID playerUUID, String playerName, String gameId, long joinedAt, int priority) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.gameId = gameId;
        this.joinedAt = joinedAt;
        this.priority = priority;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public void setPlayerUUID(UUID playerUUID) { this.playerUUID = playerUUID; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    @Override
    public String toString() {
        return "QueueEntry{player=" + playerName + ", game=" + gameId
                + ", priority=" + priority + "}";
    }
}

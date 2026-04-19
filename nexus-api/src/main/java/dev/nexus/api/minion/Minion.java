package dev.nexus.api.minion;

/**
 * Represents a single running game instance (one match of one gamemode).
 *
 * <p>Minion state is the source of truth in Redis under {@code nexus:minion:{minionId}}.
 * Instances of this class are local snapshots deserialized from Redis.
 *
 * @since 1.0.0
 */
public class Minion {

    private String minionId;
    private String gameId;
    private String turretId;
    private String serverName;
    private String host;
    private int port;
    private MinionState state;
    private String mapName;
    private long startedAt;
    private int playerCount;

    public Minion() {}

    /**
     * @param minionId    unique identifier for this minion (e.g. {@code "minion-revenant-1"})
     * @param gameId      game type identifier matching {@link dev.nexus.api.game.GameMeta#id()}
     * @param turretId    ID of the turret hosting this minion
     * @param serverName  Velocity server name used for player routing
     * @param host        hostname or IP address of the minion process
     * @param port        port the minion process is listening on
     * @param state       current lifecycle state
     * @param mapName     name of the map loaded in this minion
     * @param startedAt   epoch milliseconds when this minion was provisioned
     * @param playerCount number of players currently assigned
     */
    public Minion(String minionId, String gameId, String turretId, String serverName,
                  String host, int port, MinionState state, String mapName,
                  long startedAt, int playerCount) {
        this.minionId = minionId;
        this.gameId = gameId;
        this.turretId = turretId;
        this.serverName = serverName;
        this.host = host;
        this.port = port;
        this.state = state;
        this.mapName = mapName;
        this.startedAt = startedAt;
        this.playerCount = playerCount;
    }

    public String getMinionId() { return minionId; }
    public void setMinionId(String minionId) { this.minionId = minionId; }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getTurretId() { return turretId; }
    public void setTurretId(String turretId) { this.turretId = turretId; }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public MinionState getState() { return state; }
    public void setState(MinionState state) { this.state = state; }

    public String getMapName() { return mapName; }
    public void setMapName(String mapName) { this.mapName = mapName; }

    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }

    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }

    @Override
    public String toString() {
        return "Minion{id=" + minionId + ", game=" + gameId + ", turret=" + turretId
                + ", server=" + serverName + ", state=" + state + "}";
    }
}

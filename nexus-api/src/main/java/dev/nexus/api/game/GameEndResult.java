package dev.nexus.api.game;

import java.util.UUID;

/**
 * Carries the outcome of a completed game match.
 *
 * <p>Passed to {@link NexusGame#onMinionEnd} when a match concludes normally.
 * {@code winnerUUID} and {@code teamId} are mutually exclusive; set whichever
 * applies and leave the other {@code null}.
 *
 * @since 1.0.0
 */
public class GameEndResult {

    private final String minionId;
    private final UUID winnerUUID;
    private final String teamId;
    private final String statsJson;

    /**
     * @param minionId   ID of the minion that ran this match
     * @param winnerUUID UUID of the winning player, or {@code null} for team games
     * @param teamId     identifier of the winning team, or {@code null} for solo games
     * @param statsJson  arbitrary JSON blob of match statistics; may be {@code null}
     */
    public GameEndResult(String minionId, UUID winnerUUID, String teamId, String statsJson) {
        this.minionId = minionId;
        this.winnerUUID = winnerUUID;
        this.teamId = teamId;
        this.statsJson = statsJson;
    }

    /** @return ID of the minion that ran this match */
    public String getMinionId() { return minionId; }

    /** @return UUID of the winning player, or {@code null} for team games */
    public UUID getWinnerUUID() { return winnerUUID; }

    /** @return identifier of the winning team, or {@code null} for solo games */
    public String getTeamId() { return teamId; }

    /** @return arbitrary JSON blob of match statistics, may be {@code null} */
    public String getStatsJson() { return statsJson; }
}

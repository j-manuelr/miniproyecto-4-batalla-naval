package com.example.navalbattle.model.player;

/**
 * Tracks the two pieces of information HU-5 requires to be saved in a
 * plain-text file: the player's nickname and how many of the opponent's
 * ships this player has sunk so far. Deliberately not {@code Serializable}
 * — it is written and read as plain text (nickname=..., sunkShips=...) by
 * the persistence layer, never as a {@code .ser} object.
 */
public class PlayerStats {

    private final String nickname;
    private int sunkShipsCount;

    /**
     * Creates stats for a new player with zero ships sunk.
     *
     * @param nickname the player's nickname
     */
    public PlayerStats(String nickname) {
        this.nickname = nickname;
        this.sunkShipsCount = 0;
    }

    /**
     * Creates stats restored from a saved plain-text file.
     *
     * @param nickname       the player's nickname
     * @param sunkShipsCount the number of ships already sunk by this player
     */
    public PlayerStats(String nickname, int sunkShipsCount) {
        this.nickname = nickname;
        this.sunkShipsCount = sunkShipsCount;
    }

    public String getNickname() {
        return nickname;
    }

    public int getSunkShipsCount() {
        return sunkShipsCount;
    }

    /**
     * Increments the count of enemy ships sunk by this player. Called by
     * {@code GameEngine} whenever a shot fired by this player results in
     * {@code ShotResult.SUNK}.
     */
    public void incrementSunkShips() {
        sunkShipsCount++;
    }
}

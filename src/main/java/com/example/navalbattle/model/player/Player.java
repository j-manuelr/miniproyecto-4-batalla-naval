package com.example.navalbattle.model.player;

import com.example.navalbattle.model.board.Board;

/**
 * Base class shared by {@link HumanPlayer} and {@link MachinePlayer}: every
 * player has a nickname, owns exactly one {@link Board} (their own fleet)
 * and has {@link PlayerStats}. Both subclasses are fully substitutable
 * wherever a {@code Player} is expected (Liskov Substitution Principle) —
 * {@code GameEngine} never needs to know which one it is holding.
 */
public abstract class Player {

    private final Board board;
    private final PlayerStats stats;

    /**
     * Creates a player with brand-new stats (zero ships sunk) — the normal
     * case when starting a fresh game.
     *
     * @param nickname the player's nickname
     * @param board    this player's own board
     */
    protected Player(String nickname, Board board) {
        this(board, new PlayerStats(nickname));
    }

    /**
     * Creates a player with pre-built stats, so the sunk-ship count can be
     * restored exactly as it was when a {@code GameState} was saved (HU-5).
     * Without this constructor there would be no way to hand a loaded
     * {@link PlayerStats} to a freshly reconstructed {@code HumanPlayer}/
     * {@code MachinePlayer}, and every "Continuar" would silently reset
     * both players' sunk-ship counts back to zero.
     *
     * @param board this player's own board (typically deserialized from a
     *              save file)
     * @param stats this player's already-populated stats
     */
    protected Player(Board board, PlayerStats stats) {
        this.board = board;
        this.stats = stats;
    }

    public String getNickname() {
        return stats.getNickname();
    }

    public Board getBoard() {
        return board;
    }

    public PlayerStats getStats() {
        return stats;
    }
}

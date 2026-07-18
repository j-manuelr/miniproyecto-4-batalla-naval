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

    protected Player(String nickname, Board board) {
        this.board = board;
        this.stats = new PlayerStats(nickname);
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

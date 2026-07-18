package com.example.navalbattle.model.game;

import com.example.navalbattle.model.shot.ShotResult;

/**
 * Tracks whose turn is currently active and applies the turn-passing rule
 * described in HU-2: a shot on water passes the turn to the opponent; a hit
 * or a sunk ship lets the same player shoot again.
 */
public class TurnManager {

    /**
     * Identifies which player is currently allowed to shoot.
     */
    public enum Turn {
        HUMAN,
        MACHINE
    }

    private Turn currentTurn;

    /**
     * Creates a turn manager where the human player shoots first.
     */
    public TurnManager() {
        this.currentTurn = Turn.HUMAN;
    }

    /**
     * Returns the player whose turn is currently active.
     *
     * @return the current turn
     */
    public Turn getCurrentTurn() {
        return currentTurn;
    }

    /**
     * Applies the turn-passing rule for the given shot result. Only a
     * {@link ShotResult#WATER} result switches the active player; a
     * {@link ShotResult#HIT} or {@link ShotResult#SUNK} result keeps the
     * turn with the same player, who may shoot again.
     *
     * @param result the result of the shot that was just resolved
     */
    public void advance(ShotResult result) {
        if (result == ShotResult.WATER) {
            currentTurn = (currentTurn == Turn.HUMAN) ? Turn.MACHINE : Turn.HUMAN;
        }
    }

    /**
     * Forces the active turn to a specific player. Used exclusively when
     * restoring a {@link GameState} snapshot loaded from disk (HU-5), so the
     * resumed game continues with the exact turn it had when it was saved.
     *
     * @param turn the turn to restore
     */
    public void restore(Turn turn) {
        this.currentTurn = turn;
    }
}

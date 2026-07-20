package com.example.navalbattle.model.player;

import com.example.navalbattle.model.board.Board;

/**
 * The human player. Places their fleet through mouse/keyboard events during
 * HU-1 and fires shots through {@code GameEngine#fireAsHuman(Coordinate)}
 * during HU-2. It adds no behavior beyond {@link Player}: a human's
 * "strategy" is whatever the player decides through the UI, so there is
 * nothing extra to model here.
 */
public class HumanPlayer extends Player {

    /**
     * Creates the human player with brand-new stats — the normal case when
     * starting a fresh game.
     *
     * @param nickname the player's nickname, later saved to the plain-text
     *                 stats file (HU-5)
     * @param board    this player's own board, where their fleet is placed
     */
    public HumanPlayer(String nickname, Board board) {
        super(nickname, board);
    }

    /**
     * Creates the human player with stats restored from a save file, so a
     * resumed game (HU-5's "Continuar") shows the exact sunk-ship count it
     * had when it was last saved instead of resetting to zero.
     *
     * @param board this player's board, typically deserialized from a
     *              {@code .ser} save file
     * @param stats this player's stats, typically parsed from the
     *              companion {@code .txt} save file
     */
    public HumanPlayer(Board board, PlayerStats stats) {
        super(board, stats);
    }
}

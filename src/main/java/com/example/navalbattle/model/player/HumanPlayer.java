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
     * Creates the human player with an empty board.
     *
     * @param nickname the player's nickname, later saved to the plain-text
     *                 stats file (HU-5)
     * @param board    this player's own board, where their fleet is placed
     */
    public HumanPlayer(String nickname, Board board) {
        super(nickname, board);
    }
}

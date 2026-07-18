package com.example.navalbattle.model.game.events;

import com.example.navalbattle.model.game.TurnManager;

/**
 * Observer notified whenever the active turn changes.
 *
 * <p>A shot on water passes the turn to the opponent; a hit or a sunk ship
 * lets the same player shoot again, so this callback only fires when the
 * turn actually switches, not after every single shot.</p>
 */
public interface TurnListener {

    /**
     * Called when the active turn switches to a different player.
     *
     * @param newTurn the player whose turn is now active
     */
    void onTurnChanged(TurnManager.Turn newTurn);
}

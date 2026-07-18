package com.example.navalbattle.model.game.events;

import com.example.navalbattle.model.player.Player;

/**
 * Observer notified exactly once, when a player has sunk the opponent's
 * entire fleet and the game ends.
 */
public interface GameOverListener {

    /**
     * Called when the game ends because one fleet has been completely sunk.
     *
     * @param winner the player who sank the opponent's entire fleet
     */
    void onGameOver(Player winner);
}

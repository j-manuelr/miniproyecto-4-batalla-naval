package com.example.navalbattle.model.game.events;

import com.example.navalbattle.model.player.Player;
import com.example.navalbattle.model.shot.Shot;

/**
 * Observer notified every time a shot is resolved, regardless of who fired
 * it (human or machine) and regardless of its result (water, hit or sunk).
 *
 * <p>Kept separate from {@link TurnListener} and {@link GameOverListener}
 * (Interface Segregation Principle) so a class that only cares about
 * repainting the board after a shot does not have to implement callbacks
 * for turn changes or game-over events it does not use.</p>
 */
public interface ShotListener {

    /**
     * Called after a shot has been resolved against a board.
     *
     * @param shooter the player who fired the shot
     * @param shot    the resolved shot, including its target coordinate
     *                and {@code ShotResult}
     */
    void onShotFired(Player shooter, Shot shot);
}

package com.example.navalbattle.model.shot.strategy;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.model.shot.ShotResult;

/**
 * Strategy that decides which coordinate the machine player targets next
 * (HU-4). Isolating this decision behind an interface lets the AI's
 * behavior evolve — from purely random to a smarter "hunt after hit"
 * approach — without any change to {@code MachinePlayer} or
 * {@code GameEngine} (Open/Closed Principle).
 */
public interface ShotStrategy {

    /**
     * Chooses the next coordinate to fire at on the opponent's board. Must
     * never return a coordinate {@code opponentBoard} already reports as
     * shot via {@code Board#hasBeenShot(Coordinate)}.
     *
     * @param opponentBoard the board being targeted
     * @return a coordinate that has not been shot at yet
     */
    Coordinate nextShot(Board opponentBoard);

    /**
     * Optional hook, called by {@code GameEngine} right after a shot chosen
     * by this strategy is resolved, so stateful strategies (such as
     * {@link HuntTargetShotStrategy}) can react to the outcome. Stateless
     * strategies can simply rely on this default no-op implementation.
     *
     * @param shot   the coordinate that was targeted
     * @param result the result of that shot
     */
    default void registerResult(Coordinate shot, ShotResult result) {
        // No-op by default: stateless strategies don't need to react.
    }
}

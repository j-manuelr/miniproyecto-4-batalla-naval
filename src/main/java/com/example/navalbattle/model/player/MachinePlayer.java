package com.example.navalbattle.model.player;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.shot.strategy.ShotStrategy;

/**
 * The computer-controlled opponent (HU-4). Holds a {@link ShotStrategy}
 * (Strategy pattern) that decides which coordinate to target on each of its
 * turns, so the targeting algorithm — random, "hunt after hit", or a future
 * smarter one — can be swapped without changing this class or
 * {@code GameEngine} (Open/Closed Principle).
 */
public class MachinePlayer extends Player {

    private final ShotStrategy shotStrategy;

    /**
     * Creates the machine player.
     *
     * @param board        this player's own board, normally filled by a
     *                     {@code FleetPlacementStrategy} such as
     *                     {@code RandomFleetPlacementStrategy}
     * @param shotStrategy the strategy used to pick this player's shots
     */
    public MachinePlayer(Board board, ShotStrategy shotStrategy) {
        super("Machine", board);
        this.shotStrategy = shotStrategy;
    }

    /**
     * @return the strategy this machine uses to pick its next target
     */
    public ShotStrategy getShotStrategy() {
        return shotStrategy;
    }
}

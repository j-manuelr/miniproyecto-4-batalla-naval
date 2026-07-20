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
     * Creates the machine player with brand-new stats — the normal case
     * when starting a fresh game.
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
     * Creates the machine player with stats restored from a save file, so a
     * resumed game (HU-5's "Continuar") shows the exact sunk-ship count it
     * had when it was last saved instead of resetting to zero. Note the
     * {@code shotStrategy} is always a fresh instance: a strategy's
     * in-memory "hunt" state (e.g. {@code HuntTargetShotStrategy}'s queued
     * follow-up targets) is not part of {@code GameState} and is not
     * restored — the machine simply falls back to picking randomly for its
     * first shot after a resume, which is a deliberate, minor trade-off
     * rather than an oversight.
     *
     * @param board        this player's board, typically deserialized from
     *                     a {@code .ser} save file
     * @param stats        this player's stats, typically parsed from the
     *                     companion {@code .txt} save file
     * @param shotStrategy the strategy used to pick this player's shots
     */
    public MachinePlayer(Board board, PlayerStats stats, ShotStrategy shotStrategy) {
        super(board, stats);
        this.shotStrategy = shotStrategy;
    }

    /**
     * @return the strategy this machine uses to pick its next target
     */
    public ShotStrategy getShotStrategy() {
        return shotStrategy;
    }
}

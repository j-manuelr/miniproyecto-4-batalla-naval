package com.example.navalbattle.model.shot.strategy;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.util.AppConfig;

import java.util.Random;

/**
 * Picks a uniformly random, not-yet-shot coordinate on every turn (HU-4:
 * "La máquina selecciona casillas de disparo de manera aleatoria").
 */
public class RandomShotStrategy implements ShotStrategy {

    private final Random random;

    /**
     * Creates a strategy backed by a new {@link Random} instance.
     */
    public RandomShotStrategy() {
        this(new Random());
    }

    /**
     * Creates a strategy backed by the given {@link Random} instance.
     * Exposed so unit tests can pass a seeded {@link Random} and get
     * deterministic, reproducible shots.
     *
     * @param random the random number generator to use
     */
    public RandomShotStrategy(Random random) {
        this.random = random;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Coordinate nextShot(Board opponentBoard) {
        Coordinate candidate;
        do {
            candidate = new Coordinate(
                    random.nextInt(AppConfig.BOARD_SIZE),
                    random.nextInt(AppConfig.BOARD_SIZE));
        } while (opponentBoard.hasBeenShot(candidate));
        return candidate;
    }
}

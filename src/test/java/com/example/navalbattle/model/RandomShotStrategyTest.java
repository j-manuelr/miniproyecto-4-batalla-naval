package com.example.navalbattle.model;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.model.shot.strategy.RandomShotStrategy;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RandomShotStrategy} (HU-4): every coordinate it
 * chooses must fall inside the board, and it must never choose a
 * coordinate that has already been shot.
 */
class RandomShotStrategyTest {

    @Test
    void neverRepeatsAPreviouslyShotCoordinate() {
        Board opponentBoard = new Board();
        RandomShotStrategy strategy = new RandomShotStrategy(new Random(123));

        Set<Coordinate> shotSoFar = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            Coordinate next = strategy.nextShot(opponentBoard);

            assertFalse(shotSoFar.contains(next),
                    "coordinate " + next + " was already shot in a previous iteration");
            shotSoFar.add(next);
            opponentBoard.receiveShot(next);
        }

        assertEquals(100, shotSoFar.size(),
                "after 100 shots on a 10x10 board, every cell must have been targeted exactly once");
    }

    @Test
    void everyChosenCoordinateStaysWithinTheBoard() {
        Board opponentBoard = new Board();
        RandomShotStrategy strategy = new RandomShotStrategy(new Random(456));

        for (int i = 0; i < 50; i++) {
            Coordinate next = strategy.nextShot(opponentBoard);

            assertTrue(next.row() >= 0 && next.row() < 10, "row out of bounds: " + next);
            assertTrue(next.col() >= 0 && next.col() < 10, "column out of bounds: " + next);

            opponentBoard.receiveShot(next);
        }
    }

    @Test
    void isDeterministicForAGivenSeed() {
        RandomShotStrategy strategyA = new RandomShotStrategy(new Random(999));
        RandomShotStrategy strategyB = new RandomShotStrategy(new Random(999));

        Coordinate firstFromA = strategyA.nextShot(new Board());
        Coordinate firstFromB = strategyB.nextShot(new Board());

        assertEquals(firstFromA, firstFromB, "the same seed must produce the same first shot");
    }
}

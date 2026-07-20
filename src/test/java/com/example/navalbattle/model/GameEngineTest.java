package com.example.navalbattle.model;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.model.exceptions.InvalidShipPlacementException;
import com.example.navalbattle.model.exceptions.InvalidTurnException;
import com.example.navalbattle.model.game.GameEngine;
import com.example.navalbattle.model.game.TurnManager;
import com.example.navalbattle.model.placement.RandomFleetPlacementStrategy;
import com.example.navalbattle.model.player.HumanPlayer;
import com.example.navalbattle.model.player.MachinePlayer;
import com.example.navalbattle.model.ship.Orientation;
import com.example.navalbattle.model.ship.ShipFactory;
import com.example.navalbattle.model.ship.ShipType;
import com.example.navalbattle.model.shot.Shot;
import com.example.navalbattle.model.shot.ShotResult;
import com.example.navalbattle.model.shot.strategy.RandomShotStrategy;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link GameEngine}: the turn-passing rule from HU-2 (a
 * miss passes the turn, a hit or sunk ship does not), the
 * {@link InvalidTurnException} guard against playing out of turn, and an
 * end-to-end check that sinking a complete fleet actually ends the game.
 */
class GameEngineTest {

    private GameEngine newEngineWithASingleFrigateEach() throws InvalidShipPlacementException {
        Board humanBoard = new Board();
        Board machineBoard = new Board();
        // A single frigate per side is enough to exercise turn logic in
        // isolation; isFleetSunk() intentionally stays false here since the
        // 10-ship fleet is never completed (that rule is covered on its own
        // in BoardTest).
        humanBoard.placeShip(ShipFactory.createShip(ShipType.FRIGATE), new Coordinate(0, 0), Orientation.HORIZONTAL);
        machineBoard.placeShip(ShipFactory.createShip(ShipType.FRIGATE), new Coordinate(9, 9), Orientation.HORIZONTAL);

        HumanPlayer human = new HumanPlayer("Tester", humanBoard);
        MachinePlayer machine = new MachinePlayer(machineBoard, new RandomShotStrategy(new Random(1)));
        return new GameEngine(human, machine);
    }

    @Test
    void aMissPassesTheTurnToTheMachine() throws InvalidShipPlacementException {
        GameEngine engine = newEngineWithASingleFrigateEach();

        Shot shot = engine.fireAsHuman(new Coordinate(5, 5)); // no ship there: guaranteed water

        assertEquals(ShotResult.WATER, shot.result());
        assertEquals(TurnManager.Turn.MACHINE, engine.getCurrentTurn());
    }

    @Test
    void aHitLetsTheSamePlayerShootAgain() throws InvalidShipPlacementException {
        Board humanBoard = new Board();
        Board machineBoard = new Board();
        machineBoard.placeShip(ShipFactory.createShip(ShipType.DESTROYER), new Coordinate(0, 0), Orientation.HORIZONTAL);
        HumanPlayer human = new HumanPlayer("Tester", humanBoard);
        MachinePlayer machine = new MachinePlayer(machineBoard, new RandomShotStrategy(new Random(1)));
        GameEngine engine = new GameEngine(human, machine);

        // The destroyer needs 2 hits to sink, so this first hit must not end its turn.
        Shot shot = engine.fireAsHuman(new Coordinate(0, 0));

        assertEquals(ShotResult.HIT, shot.result());
        assertEquals(TurnManager.Turn.HUMAN, engine.getCurrentTurn(), "a hit must not pass the turn");
    }

    @Test
    void firingOutOfTurnThrows() throws InvalidShipPlacementException {
        GameEngine engine = newEngineWithASingleFrigateEach();

        assertThrows(InvalidTurnException.class, () -> engine.fireAsMachine(new Coordinate(0, 0)));
    }

    @Test
    void sinkingTheEntireMachineFleetEndsTheGame() throws InvalidShipPlacementException {
        Board humanBoard = new Board();
        Board machineBoard = new Board();
        new RandomFleetPlacementStrategy(new Random(42)).placeFleet(humanBoard);
        new RandomFleetPlacementStrategy(new Random(99)).placeFleet(machineBoard);

        HumanPlayer human = new HumanPlayer("Tester", humanBoard);
        MachinePlayer machine = new MachinePlayer(machineBoard, new RandomShotStrategy(new Random(7)));
        GameEngine engine = new GameEngine(human, machine);
        engine.confirmFleetPlacement(human);
        engine.confirmFleetPlacement(machine);

        // Systematically sweep every cell of the machine's board on the
        // human's turns (skipping cells already shot), so every one of its
        // ships is guaranteed to be hit regardless of where the random
        // placement put them; the machine plays its own turns independently
        // in between.
        Deque<Coordinate> everyCell = new ArrayDeque<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                everyCell.add(new Coordinate(row, col));
            }
        }

        int safetyLimit = 500;
        while (!engine.isGameOver() && safetyLimit-- > 0) {
            if (engine.getCurrentTurn() == TurnManager.Turn.HUMAN) {
                engine.fireAsHuman(nextUnshotCoordinate(everyCell, machineBoard));
            } else {
                engine.fireAsMachine(machine.getShotStrategy().nextShot(humanBoard));
            }
        }

        assertTrue(engine.isGameOver(), "the game must end once every machine ship is sunk");
        assertEquals(10, human.getStats().getSunkShipsCount(),
                "the human must be credited with sinking every one of the machine's 10 ships");
    }

    private Coordinate nextUnshotCoordinate(Deque<Coordinate> pool, Board board) {
        Coordinate candidate;
        do {
            candidate = pool.poll();
        } while (board.hasBeenShot(candidate));
        return candidate;
    }
}

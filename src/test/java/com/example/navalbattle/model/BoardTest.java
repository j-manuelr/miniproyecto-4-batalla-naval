package com.example.navalbattle.model;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.model.exceptions.CellAlreadyShotException;
import com.example.navalbattle.model.exceptions.FleetAlreadyPlacedException;
import com.example.navalbattle.model.exceptions.InvalidShipPlacementException;
import com.example.navalbattle.model.ship.Orientation;
import com.example.navalbattle.model.ship.Ship;
import com.example.navalbattle.model.ship.ShipFactory;
import com.example.navalbattle.model.ship.ShipType;
import com.example.navalbattle.model.shot.ShotResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Board}: the placement rules from HU-1 (bounds,
 * overlap, fleet composition, locking) and the shot-resolution rules from
 * HU-2 (water/hit/sunk, no repeated shots). Together these two areas cover
 * most of the game's actual behavior, which is why {@code Board} gets the
 * most thorough test class in this project.
 */
class BoardTest {

    @Test
    void placesAShipHorizontallyWithoutError() throws InvalidShipPlacementException {
        Board board = new Board();
        Ship destroyer = ShipFactory.createShip(ShipType.DESTROYER);

        board.placeShip(destroyer, new Coordinate(2, 2), Orientation.HORIZONTAL);

        assertEquals(1, board.getShips().size());
        assertTrue(board.getShips().contains(destroyer));
    }

    @Test
    void rejectsAShipThatGoesOutOfBounds() {
        Board board = new Board();
        Ship carrier = ShipFactory.createShip(ShipType.AIRCRAFT_CARRIER);

        assertThrows(InvalidShipPlacementException.class,
                () -> board.placeShip(carrier, new Coordinate(0, 8), Orientation.HORIZONTAL));
    }

    @Test
    void rejectsTwoShipsThatOverlap() throws InvalidShipPlacementException {
        Board board = new Board();
        board.placeShip(ShipFactory.createShip(ShipType.DESTROYER), new Coordinate(5, 5), Orientation.HORIZONTAL);
        Ship overlapping = ShipFactory.createShip(ShipType.FRIGATE);

        assertThrows(InvalidShipPlacementException.class,
                () -> board.placeShip(overlapping, new Coordinate(5, 5), Orientation.HORIZONTAL));
    }

    @Test
    void rejectsMoreShipsOfATypeThanTheFleetAllows() throws InvalidShipPlacementException {
        Board board = new Board();
        board.placeShip(ShipFactory.createShip(ShipType.AIRCRAFT_CARRIER), new Coordinate(0, 0), Orientation.HORIZONTAL);
        Ship secondCarrier = ShipFactory.createShip(ShipType.AIRCRAFT_CARRIER);

        assertThrows(InvalidShipPlacementException.class,
                () -> board.placeShip(secondCarrier, new Coordinate(4, 0), Orientation.HORIZONTAL));
    }

    @Test
    void locksPlacementSoFurtherShipsAreRejected() throws InvalidShipPlacementException {
        Board board = new Board();
        board.placeShip(ShipFactory.createShip(ShipType.FRIGATE), new Coordinate(0, 0), Orientation.HORIZONTAL);
        board.lockPlacement();
        Ship anotherFrigate = ShipFactory.createShip(ShipType.FRIGATE);

        assertThrows(FleetAlreadyPlacedException.class,
                () -> board.placeShip(anotherFrigate, new Coordinate(9, 9), Orientation.HORIZONTAL));
    }

    @Test
    void aShotOnAnEmptyCellIsWater() {
        Board board = new Board();

        assertEquals(ShotResult.WATER, board.receiveShot(new Coordinate(3, 3)));
    }

    @Test
    void aShotThatDoesNotSinkTheShipIsAHit() throws InvalidShipPlacementException {
        Board board = new Board();
        board.placeShip(ShipFactory.createShip(ShipType.DESTROYER), new Coordinate(0, 0), Orientation.HORIZONTAL);

        assertEquals(ShotResult.HIT, board.receiveShot(new Coordinate(0, 0)));
    }

    @Test
    void theLastShotOnAShipSinksIt() throws InvalidShipPlacementException {
        Board board = new Board();
        board.placeShip(ShipFactory.createShip(ShipType.FRIGATE), new Coordinate(0, 0), Orientation.HORIZONTAL);

        assertEquals(ShotResult.SUNK, board.receiveShot(new Coordinate(0, 0)));
    }

    @Test
    void cannotShootTheSameCellTwice() {
        Board board = new Board();
        board.receiveShot(new Coordinate(1, 1));

        assertThrows(CellAlreadyShotException.class, () -> board.receiveShot(new Coordinate(1, 1)));
    }

    @Test
    void isFleetSunkStaysFalseWhileTheFleetIsIncomplete() throws InvalidShipPlacementException {
        Board board = new Board();
        board.placeShip(ShipFactory.createShip(ShipType.FRIGATE), new Coordinate(0, 0), Orientation.HORIZONTAL);

        assertFalse(board.isFleetSunk(), "only 1 of 10 ships is placed, so this must never count as sunk");

        board.receiveShot(new Coordinate(0, 0));
        assertFalse(board.isFleetSunk(), "still false: the 10-ship fleet was never completed, regardless of hits");
    }

    @Test
    void hasBeenShotReflectsPastShotsOnly() {
        Board board = new Board();
        Coordinate target = new Coordinate(4, 4);

        assertFalse(board.hasBeenShot(target));
        board.receiveShot(target);
        assertTrue(board.hasBeenShot(target));
    }
}

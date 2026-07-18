package com.example.navalbattle.model.board;

import com.example.navalbattle.model.exceptions.CellAlreadyShotException;
import com.example.navalbattle.model.exceptions.FleetAlreadyPlacedException;
import com.example.navalbattle.model.exceptions.InvalidShipPlacementException;
import com.example.navalbattle.model.ship.Orientation;
import com.example.navalbattle.model.ship.Ship;
import com.example.navalbattle.model.ship.ShipType;
import com.example.navalbattle.model.shot.ShotResult;
import com.example.navalbattle.util.AppConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A single player's 10x10 board: it owns the grid of {@link Cell}s, the
 * fleet placed on it, and the set of cells already targeted. The same
 * {@code Board} class backs both the "tablero de posición" and the
 * "tablero principal" described in the assignment — the only difference
 * between them is how {@code view.BoardView} chooses to render this data
 * (owner view vs. fog-of-war vs. verification view), which keeps this
 * class free of any UI concern (Single Responsibility Principle).
 *
 * <p>Implements {@link Iterable} over its cells (in row-major order) via a
 * private inner class, so callers such as {@code BoardView} can iterate
 * with a plain for-each loop instead of nested index loops.</p>
 */
public class Board implements Iterable<Cell>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final int TOTAL_SHIPS =
            Arrays.stream(ShipType.values()).mapToInt(ShipType::getFleetCount).sum();

    private final Cell[][] grid;
    private final List<Ship> ships = new ArrayList<>();
    private final Map<Coordinate, Ship> shipsByCoordinate = new HashMap<>();
    private final Set<Coordinate> shotCoordinates = new HashSet<>();
    private boolean placementLocked = false;

    /**
     * Creates an empty {@value AppConfig#BOARD_SIZE}x{@value AppConfig#BOARD_SIZE} board.
     */
    public Board() {
        grid = new Cell[AppConfig.BOARD_SIZE][AppConfig.BOARD_SIZE];
        for (int row = 0; row < AppConfig.BOARD_SIZE; row++) {
            for (int col = 0; col < AppConfig.BOARD_SIZE; col++) {
                grid[row][col] = new Cell();
            }
        }
    }

    // ------------------------------------------------------------------
    // Placement (HU-1)
    // ------------------------------------------------------------------

    /**
     * Places a ship on this board.
     *
     * @param ship        the ship to place
     * @param origin      the coordinate of the ship's first cell
     * @param orientation the direction the remaining cells extend in
     * @throws InvalidShipPlacementException if any cell falls outside the
     *         board, overlaps an already-placed ship, or the fleet already
     *         has the maximum number of ships of this type
     * @throws FleetAlreadyPlacedException if placement has already been
     *         locked with {@link #lockPlacement()}
     */
    public void placeShip(Ship ship, Coordinate origin, Orientation orientation)
            throws InvalidShipPlacementException {
        if (placementLocked) {
            throw new FleetAlreadyPlacedException(
                    "The fleet is already placed and cannot be modified");
        }

        long alreadyPlacedOfType = ships.stream()
                .filter(placed -> placed.getType() == ship.getType())
                .count();
        if (alreadyPlacedOfType >= ship.getType().getFleetCount()) {
            throw new InvalidShipPlacementException(
                    "The fleet already has the maximum number of " + ship.getType().getDisplayName());
        }

        List<Coordinate> targetCells = computeOccupiedCoordinates(origin, orientation, ship.getSize());
        validateCellsAreFree(targetCells);

        for (Coordinate cell : targetCells) {
            shipsByCoordinate.put(cell, ship);
            grid[cell.row()][cell.col()].setState(CellState.SHIP);
        }
        ships.add(ship);
    }

    private List<Coordinate> computeOccupiedCoordinates(Coordinate origin, Orientation orientation, int size) {
        List<Coordinate> cells = new ArrayList<>(size);
        for (int offset = 0; offset < size; offset++) {
            int row = origin.row() + (orientation == Orientation.VERTICAL ? offset : 0);
            int col = origin.col() + (orientation == Orientation.HORIZONTAL ? offset : 0);
            cells.add(new Coordinate(row, col));
        }
        return cells;
    }

    private void validateCellsAreFree(List<Coordinate> cells) throws InvalidShipPlacementException {
        for (Coordinate cell : cells) {
            if (!cell.isWithinBounds(AppConfig.BOARD_SIZE)) {
                throw new InvalidShipPlacementException("Coordinate " + cell + " is outside the board");
            }
            if (shipsByCoordinate.containsKey(cell)) {
                throw new InvalidShipPlacementException("Coordinate " + cell + " overlaps another ship");
            }
        }
    }

    /**
     * Locks the placement phase: after this call, {@link #placeShip} always
     * throws {@link FleetAlreadyPlacedException}. Intended to be called
     * once the player confirms their full fleet (HU-1).
     */
    public void lockPlacement() {
        this.placementLocked = true;
    }

    /**
     * @return {@code true} once all ten ships from every {@link ShipType}
     *         have been placed
     */
    public boolean isFleetComplete() {
        return ships.size() == TOTAL_SHIPS;
    }

    // ------------------------------------------------------------------
    // Shooting (HU-2, HU-4)
    // ------------------------------------------------------------------

    /**
     * Resolves a shot fired at this board.
     *
     * @param target the targeted coordinate
     * @return the result of the shot
     * @throws CellAlreadyShotException if this coordinate was already
     *         targeted before
     */
    public ShotResult receiveShot(Coordinate target) {
        if (!target.isWithinBounds(AppConfig.BOARD_SIZE)) {
            throw new IllegalArgumentException("Coordinate " + target + " is outside the board");
        }
        if (shotCoordinates.contains(target)) {
            throw new CellAlreadyShotException("Cell " + target + " has already been targeted");
        }
        shotCoordinates.add(target);

        Ship ship = shipsByCoordinate.get(target);
        if (ship == null) {
            grid[target.row()][target.col()].setState(CellState.WATER);
            return ShotResult.WATER;
        }

        ship.registerHit();
        if (ship.isSunk()) {
            markShipAsSunk(ship);
            return ShotResult.SUNK;
        }
        grid[target.row()][target.col()].setState(CellState.HIT);
        return ShotResult.HIT;
    }

    private void markShipAsSunk(Ship ship) {
        for (Map.Entry<Coordinate, Ship> entry : shipsByCoordinate.entrySet()) {
            if (entry.getValue() == ship) {
                grid[entry.getKey().row()][entry.getKey().col()].setState(CellState.SUNK);
            }
        }
    }

    /**
     * @param coordinate the coordinate to check
     * @return {@code true} if a shot has already been resolved at this
     *         coordinate; used by shot strategies to avoid repeating a shot
     */
    public boolean hasBeenShot(Coordinate coordinate) {
        return shotCoordinates.contains(coordinate);
    }

    /**
     * @return {@code true} once every ship of a complete fleet has been sunk
     */
    public boolean isFleetSunk() {
        return isFleetComplete() && ships.stream().allMatch(Ship::isSunk);
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public Cell getCell(Coordinate coordinate) {
        return grid[coordinate.row()][coordinate.col()];
    }

    /**
     * @return an unmodifiable view of the ships placed on this board
     */
    public List<Ship> getShips() {
        return Collections.unmodifiableList(ships);
    }

    // ------------------------------------------------------------------
    // Iterable<Cell> (Iterator pattern, inner class)
    // ------------------------------------------------------------------

    /**
     * Iterates over every cell of this board in row-major order.
     */
    @Override
    public Iterator<Cell> iterator() {
        return new BoardIterator();
    }

    /**
     * Private inner class implementing {@link Iterator}: it needs direct
     * access to the enclosing {@code Board}'s {@code grid} field, which is
     * exactly the kind of tight coupling an inner class is meant for.
     */
    private class BoardIterator implements Iterator<Cell> {

        private int row = 0;
        private int col = 0;

        @Override
        public boolean hasNext() {
            return row < AppConfig.BOARD_SIZE;
        }

        @Override
        public Cell next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more cells in this board");
            }
            Cell cell = grid[row][col];
            col++;
            if (col == AppConfig.BOARD_SIZE) {
                col = 0;
                row++;
            }
            return cell;
        }
    }
}

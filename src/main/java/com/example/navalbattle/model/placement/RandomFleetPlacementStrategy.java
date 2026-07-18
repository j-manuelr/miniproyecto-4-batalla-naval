package com.example.navalbattle.model.placement;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.model.exceptions.InvalidShipPlacementException;
import com.example.navalbattle.model.ship.Orientation;
import com.example.navalbattle.model.ship.Ship;
import com.example.navalbattle.model.ship.ShipFactory;
import com.example.navalbattle.model.ship.ShipType;
import com.example.navalbattle.util.AppConfig;

import java.util.Random;

/**
 * Places the entire fleet at random, valid coordinates and orientations.
 *
 * <p>Used by {@code MachinePlayer} to satisfy HU-4 ("la máquina coloca sus
 * barcos aleatoriamente siguiendo las reglas del juego"), but it can also be
 * offered to the human player as a "random placement" convenience button
 * without any change to {@link Board} or {@link com.example.navalbattle.model.game.GameEngine}.</p>
 */
public class RandomFleetPlacementStrategy implements FleetPlacementStrategy {

    /**
     * Maximum number of attempts allowed per ship before giving up. Prevents
     * an infinite loop in the unlikely case the board becomes too crowded
     * to fit the remaining ships.
     */
    private static final int MAX_ATTEMPTS_PER_SHIP = 200;

    private final Random random;

    /**
     * Creates a strategy backed by a new {@link Random} instance.
     */
    public RandomFleetPlacementStrategy() {
        this(new Random());
    }

    /**
     * Creates a strategy backed by the given {@link Random} instance.
     * Exposed for unit testing, where a seeded {@link Random} makes the
     * placement deterministic and therefore reproducible in assertions.
     *
     * @param random the random number generator to use
     */
    public RandomFleetPlacementStrategy(Random random) {
        this.random = random;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void placeFleet(Board board) throws InvalidShipPlacementException {
        for (ShipType type : ShipType.values()) {
            for (int i = 0; i < type.getFleetCount(); i++) {
                placeSingleShip(board, ShipFactory.createShip(type));
            }
        }
    }

    /**
     * Attempts random coordinates and orientations for a single ship until
     * the board accepts the placement or the attempt budget is exhausted.
     *
     * @param board the target board
     * @param ship  the ship to place
     * @throws InvalidShipPlacementException if no valid position is found
     *         within {@link #MAX_ATTEMPTS_PER_SHIP} attempts
     */
    private void placeSingleShip(Board board, Ship ship) throws InvalidShipPlacementException {
        InvalidShipPlacementException lastFailure = null;

        for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_SHIP; attempt++) {
            Coordinate origin = randomCoordinate();
            Orientation orientation = randomOrientation();
            try {
                board.placeShip(ship, origin, orientation);
                return;
            } catch (InvalidShipPlacementException failure) {
                lastFailure = failure;
            }
        }

        throw new InvalidShipPlacementException(
                "Could not find a valid random position for " + ship.getType().getDisplayName()
                        + " after " + MAX_ATTEMPTS_PER_SHIP + " attempts",
                lastFailure);
    }

    private Coordinate randomCoordinate() {
        int row = random.nextInt(AppConfig.BOARD_SIZE);
        int col = random.nextInt(AppConfig.BOARD_SIZE);
        return new Coordinate(row, col);
    }

    private Orientation randomOrientation() {
        return random.nextBoolean() ? Orientation.HORIZONTAL : Orientation.VERTICAL;
    }
}

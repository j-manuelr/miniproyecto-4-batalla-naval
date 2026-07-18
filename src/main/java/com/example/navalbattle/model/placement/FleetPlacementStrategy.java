package com.example.navalbattle.model.placement;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.exceptions.InvalidShipPlacementException;

/**
 * Strategy for placing a complete fleet on a {@link Board}.
 *
 * <p>Implementations decide how each ship is positioned (coordinates and
 * orientation). Keeping this behavior behind an interface allows new
 * placement algorithms (e.g. a manual, UI-driven placement or a
 * difficulty-aware placement for the machine player) to be added without
 * modifying {@link Board} or any client code that depends on this
 * interface (Open/Closed Principle).</p>
 */
public interface FleetPlacementStrategy {

    /**
     * Places the full fleet (one {@code AircraftCarrier}, two
     * {@code Submarine}s, three {@code Destroyer}s and four
     * {@code Frigate}s, as defined by {@code ShipType}) on the given board.
     *
     * @param board the board on which the fleet must be placed; must not
     *              already contain a fleet
     * @throws InvalidShipPlacementException if the strategy cannot find a
     *         valid position for one or more ships
     */
    void placeFleet(Board board) throws InvalidShipPlacementException;
}

package com.example.navalbattle.model.exceptions;

/**
 * Checked exception thrown when a ship cannot be placed at the requested
 * coordinate and orientation: it overlaps another ship, falls outside the
 * board, or would exceed the number of ships allowed for its
 * {@code ShipType} (HU-1: "El sistema debe validar que los barcos no se
 * superpongan ni salgan del tablero").
 *
 * <p>Declared as checked on purpose: a controller that calls
 * {@code Board.placeShip(...)} must explicitly decide how to react (e.g.
 * show an alert and let the player try again), so the compiler enforces
 * that this case is never silently ignored.</p>
 */
public class InvalidShipPlacementException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidShipPlacementException(String message) {
        super(message);
    }

    public InvalidShipPlacementException(String message, Throwable cause) {
        super(message, cause);
    }
}

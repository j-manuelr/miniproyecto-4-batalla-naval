package com.example.navalbattle.model.exceptions;

/**
 * Unchecked exception thrown when code attempts to place or move a ship on
 * a {@code Board} whose placement phase has already been locked (HU-1:
 * "Una vez colocados, los barcos no pueden ser movidos ni modificados").
 */
public class FleetAlreadyPlacedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FleetAlreadyPlacedException(String message) {
        super(message);
    }
}

package com.example.navalbattle.model.exceptions;

/**
 * Unchecked exception thrown when a player attempts to fire a shot while it
 * is not their turn, or after the game has already ended. Used by
 * {@code GameEngine#fireAsHuman(Coordinate)} and
 * {@code GameEngine#fireAsMachine(Coordinate)}.
 */
public class InvalidTurnException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidTurnException(String message) {
        super(message);
    }
}

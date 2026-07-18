package com.example.navalbattle.model.exceptions;

/**
 * Unchecked exception thrown when a shot targets a cell that has already
 * been fired at (HU-2: "El jugador no puede disparar en una posición donde
 * sea agua, tocado o hundido").
 *
 * <p>Declared unchecked because, in normal play, the UI is expected to
 * disable already-shot cells so this condition should never actually be
 * reachable through the mouse — it exists as a defensive, programmer-error
 * guard in the model layer, which is the conventional use of an unchecked
 * exception.</p>
 */
public class CellAlreadyShotException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CellAlreadyShotException(String message) {
        super(message);
    }
}

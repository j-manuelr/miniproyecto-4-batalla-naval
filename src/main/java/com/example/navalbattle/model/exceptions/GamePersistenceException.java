package com.example.navalbattle.model.exceptions;

/**
 * The project's own, domain-specific checked exception (the single
 * "excepción propia" required by the rubric). Thrown by the persistence
 * layer whenever saving or loading a {@code GameState} snapshot or a
 * player's plain-text stats file fails.
 *
 * <p>It exists so that controllers never have to catch low-level
 * {@link java.io.IOException} or {@link ClassNotFoundException} directly —
 * those are wrapped here as the {@code cause}, and the message is phrased
 * in terms the UI can show the player directly (e.g. "No se pudo guardar
 * la partida"), instead of a raw I/O stack trace.</p>
 */
public class GamePersistenceException extends Exception {

    private static final long serialVersionUID = 1L;

    public GamePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public GamePersistenceException(String message) {
        super(message);
    }
}

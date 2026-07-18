package com.example.navalbattle.model.board;

import java.io.Serializable;

/**
 * A single square of a {@link Board}. Holds only its current
 * {@link CellState}; which {@link com.example.navalbattle.model.ship.Ship} (if any)
 * occupies it is tracked separately by {@code Board}, keeping this class
 * small and focused on one thing (Single Responsibility Principle).
 */
public class Cell implements Serializable {

    private static final long serialVersionUID = 1L;

    private CellState state;

    /**
     * Creates an empty cell.
     */
    public Cell() {
        this.state = CellState.EMPTY;
    }

    public CellState getState() {
        return state;
    }

    public void setState(CellState state) {
        this.state = state;
    }
}

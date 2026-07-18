package com.example.navalbattle.model.board;

import java.io.Serializable;

/**
 * An immutable (row, column) position on a {@link Board}. Implemented as a
 * record because it is a pure value: two coordinates with the same row and
 * column are always equal and interchangeable, which is exactly what
 * records provide (equals/hashCode/toString) without boilerplate.
 *
 * @param row the zero-based row index
 * @param col the zero-based column index
 */
public record Coordinate(int row, int col) implements Serializable {

    /**
     * Checks whether this coordinate falls within a square board of the
     * given size.
     *
     * @param boardSize the size of the (square) board
     * @return {@code true} if both {@code row} and {@code col} are within
     *         {@code [0, boardSize)}
     */
    public boolean isWithinBounds(int boardSize) {
        return row >= 0 && row < boardSize && col >= 0 && col < boardSize;
    }
}

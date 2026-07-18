package com.example.navalbattle.model.board;

/**
 * Every possible visual/logical state of a single {@link Cell}.
 */
public enum CellState {

    /** Nothing has been placed or shot at this cell yet. */
    EMPTY,

    /** A ship occupies this cell but it has not been hit. */
    SHIP,

    /** A shot was fired here and there was no ship (a miss). */
    WATER,

    /** A shot hit a ship here, but the ship is not fully sunk yet. */
    HIT,

    /** A shot hit this cell and the ship it belongs to is fully sunk. */
    SUNK
}

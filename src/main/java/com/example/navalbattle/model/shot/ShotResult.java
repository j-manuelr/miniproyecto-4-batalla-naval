package com.example.navalbattle.model.shot;

/**
 * The three possible outcomes of a shot, exactly as named in the
 * assignment: "agua" (water), "tocado" (hit) and "hundido" (sunk).
 */
public enum ShotResult {

    /** No ship occupied the targeted cell. Passes the turn. */
    WATER,

    /** A ship was hit but at least one of its cells is still intact. */
    HIT,

    /** The hit ship had no remaining intact cells: it is fully sunk. */
    SUNK
}

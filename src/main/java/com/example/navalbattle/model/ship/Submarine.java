package com.example.navalbattle.model.ship;

/**
 * A 3-cell ship. Two are placed per game, as defined by
 * {@link ShipType#SUBMARINE}.
 */
public class Submarine extends Ship {

    private static final long serialVersionUID = 1L;

    public Submarine() {
        super(ShipType.SUBMARINE);
    }
}

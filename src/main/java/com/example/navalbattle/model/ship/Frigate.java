package com.example.navalbattle.model.ship;

/**
 * The smallest ship in the fleet: a single cell. Four are placed per game,
 * as defined by {@link ShipType#FRIGATE}. A single hit sinks it.
 */
public class Frigate extends Ship {

    private static final long serialVersionUID = 1L;

    public Frigate() {
        super(ShipType.FRIGATE);
    }
}

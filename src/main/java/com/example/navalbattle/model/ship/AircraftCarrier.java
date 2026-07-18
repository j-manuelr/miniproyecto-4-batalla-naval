package com.example.navalbattle.model.ship;

/**
 * The largest ship in the fleet: occupies 4 cells. Exactly one is placed
 * per game, as defined by {@link ShipType#AIRCRAFT_CARRIER}.
 */
public class AircraftCarrier extends Ship {

    private static final long serialVersionUID = 1L;

    public AircraftCarrier() {
        super(ShipType.AIRCRAFT_CARRIER);
    }
}

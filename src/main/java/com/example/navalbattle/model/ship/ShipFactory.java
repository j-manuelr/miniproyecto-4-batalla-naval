package com.example.navalbattle.model.ship;

/**
 * Factory Method that creates the concrete {@link Ship} subclass matching a
 * given {@link ShipType}, so callers (e.g. {@code RandomFleetPlacementStrategy})
 * never need a chain of {@code instanceof}/{@code switch} checks scattered
 * across the codebase. Adding a new ship type only requires a new
 * {@code Ship} subclass and a new branch here (Open/Closed Principle at the
 * boundary of this single class).
 */
public final class ShipFactory {

    private ShipFactory() {
        // Static factory; not meant to be instantiated.
    }

    /**
     * Creates a new, undamaged ship of the requested type.
     *
     * @param type the type of ship to create
     * @return a new {@link Ship} instance matching {@code type}
     */
    public static Ship createShip(ShipType type) {
        return switch (type) {
            case AIRCRAFT_CARRIER -> new AircraftCarrier();
            case SUBMARINE -> new Submarine();
            case DESTROYER -> new Destroyer();
            case FRIGATE -> new Frigate();
        };
    }
}

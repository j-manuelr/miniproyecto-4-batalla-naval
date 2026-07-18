package com.example.navalbattle.model.ship;

import java.io.Serializable;

/**
 * Base class for every ship in the fleet. A ship only tracks its
 * {@link ShipType} and how many of its cells have been hit — it does not
 * know its own coordinates on the board; {@code Board} owns that mapping,
 * which keeps position (a board concern) separate from damage (a ship
 * concern), following the Single Responsibility Principle.
 *
 * <p>Concrete subclasses ({@link AircraftCarrier}, {@link Submarine},
 * {@link Destroyer}, {@link Frigate}) exist so each ship type is a real,
 * substitutable {@code Ship} (Liskov Substitution Principle) instead of a
 * single class parameterized by an enum; new ship types can be added by
 * creating a new subclass and registering it in {@link ShipFactory}
 * without touching this class.</p>
 */
public abstract class Ship implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ShipType type;
    private int hitCount;

    /**
     * Creates a ship of the given type with zero hits registered.
     *
     * @param type the ship's type
     */
    protected Ship(ShipType type) {
        this.type = type;
    }

    public ShipType getType() {
        return type;
    }

    /**
     * @return the number of cells this ship occupies
     */
    public int getSize() {
        return type.getSize();
    }

    /**
     * Registers a hit on this ship. Called by {@code Board} once per shot
     * that lands on one of this ship's cells.
     */
    public void registerHit() {
        if (hitCount < type.getSize()) {
            hitCount++;
        }
    }

    /**
     * @return {@code true} if every cell of this ship has been hit
     */
    public boolean isSunk() {
        return hitCount >= type.getSize();
    }
}

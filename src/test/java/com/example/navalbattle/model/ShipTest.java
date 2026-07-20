package com.example.navalbattle.model;

import com.example.navalbattle.model.ship.Ship;
import com.example.navalbattle.model.ship.ShipFactory;
import com.example.navalbattle.model.ship.ShipType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Ship} and {@link ShipFactory}: verifies the
 * size/fleet-count rules from {@link ShipType} are honored, and that a
 * ship only reports itself as sunk once every one of its cells has been
 * hit — never before, and no differently if it keeps getting hit after.
 */
class ShipTest {

    @Test
    void createShipMatchesTheRequestedTypeForEveryShipInTheFleet() {
        for (ShipType type : ShipType.values()) {
            Ship ship = ShipFactory.createShip(type);

            assertEquals(type, ship.getType());
            assertEquals(type.getSize(), ship.getSize());
            assertFalse(ship.isSunk(), type + " must not start out sunk");
        }
    }

    @Test
    void aircraftCarrierNeedsFourHitsToSink() {
        Ship carrier = ShipFactory.createShip(ShipType.AIRCRAFT_CARRIER);

        for (int hit = 1; hit <= 3; hit++) {
            carrier.registerHit();
            assertFalse(carrier.isSunk(), "should still be afloat after " + hit + " hit(s)");
        }

        carrier.registerHit();
        assertTrue(carrier.isSunk(), "should be sunk after exactly 4 hits");
    }

    @Test
    void frigateSinksOnItsSingleHit() {
        Ship frigate = ShipFactory.createShip(ShipType.FRIGATE);

        assertFalse(frigate.isSunk());
        frigate.registerHit();
        assertTrue(frigate.isSunk());
    }

    @Test
    void extraHitsBeyondTheShipSizeAreIgnoredAndNeverThrow() {
        Ship frigate = ShipFactory.createShip(ShipType.FRIGATE);

        frigate.registerHit();
        frigate.registerHit();
        frigate.registerHit();

        assertTrue(frigate.isSunk(), "extra hits must not change the sunk outcome");
    }

    @Test
    void fleetCompositionMatchesTheAssignment() {
        int totalShips = 0;
        int totalCells = 0;
        for (ShipType type : ShipType.values()) {
            totalShips += type.getFleetCount();
            totalCells += type.getFleetCount() * type.getSize();
        }

        assertEquals(10, totalShips, "the fleet must have exactly 10 ships"
                + " (1 aircraft carrier + 2 submarines + 3 destroyers + 4 frigates)");
        assertEquals(20, totalCells, "the fleet must occupy exactly 20 cells in total");
    }
}

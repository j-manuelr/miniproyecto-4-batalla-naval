package com.example.navalbattle.model.ship;

/**
 * A 2-cell ship. Three are placed per game, as defined by
 * {@link ShipType#DESTROYER}.
 */
public class Destroyer extends Ship {

    private static final long serialVersionUID = 1L;

    public Destroyer() {
        super(ShipType.DESTROYER);
    }
}

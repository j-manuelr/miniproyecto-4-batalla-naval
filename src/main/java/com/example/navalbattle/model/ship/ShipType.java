package com.example.navalbattle.model.ship;

/**
 * Defines the fleet composition required by the assignment: one aircraft
 * carrier (4 cells), two submarines (3 cells each), three destroyers (2
 * cells each) and four frigates (1 cell each) — ten ships in total.
 *
 * <p>Keeping size and fleet count here, instead of scattering "magic
 * numbers" across {@code Board} and {@code RandomFleetPlacementStrategy},
 * means the fleet composition only has to change in one place if the
 * rules ever do (Open/Closed Principle).</p>
 */
public enum ShipType {

    AIRCRAFT_CARRIER(4, 1, "Aircraft carrier"),
    SUBMARINE(3, 2, "Submarine"),
    DESTROYER(2, 3, "Destroyer"),
    FRIGATE(1, 4, "Frigate");

    private final int size;
    private final int fleetCount;
    private final String displayName;

    ShipType(int size, int fleetCount, String displayName) {
        this.size = size;
        this.fleetCount = fleetCount;
        this.displayName = displayName;
    }

    /**
     * @return how many cells a ship of this type occupies
     */
    public int getSize() {
        return size;
    }

    /**
     * @return how many ships of this type make up a full fleet
     */
    public int getFleetCount() {
        return fleetCount;
    }

    /**
     * @return a human-readable name suitable for UI labels and messages
     */
    public String getDisplayName() {
        return displayName;
    }
}

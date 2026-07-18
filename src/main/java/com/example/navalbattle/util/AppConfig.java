package com.example.navalbattle.util;

/**
 * Central place for constants shared across the model, so magic numbers
 * (board dimensions, file locations) never get hard-coded twice. This is a
 * plain constants holder, not a Singleton: it has no instance state and is
 * never instantiated.
 */
public final class AppConfig {

    /** Width and height of every board (10x10, as required by the rules). */
    public static final int BOARD_SIZE = 10;

    /** Total number of ships in a full fleet (1 + 2 + 3 + 4). */
    public static final int TOTAL_FLEET_SIZE = 10;

    /** Directory where autosave files (.ser and .txt) are written. */
    public static final String SAVES_DIRECTORY = "saves";

    private AppConfig() {
        // Prevents instantiation of this constants holder.
    }
}

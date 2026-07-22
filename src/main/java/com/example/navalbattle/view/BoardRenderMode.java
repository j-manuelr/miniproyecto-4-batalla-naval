package com.example.navalbattle.view;

/**
 * Determines how much of a {@code Board} is revealed to the player and
 * whether the rendered board reacts to mouse clicks.
 * <p>
 * This single enum is what lets {@link BoardView} render the same
 * {@code Board} model in three different ways instead of needing three
 * different board classes (see section 1 of the architecture plan).
 */
public enum BoardRenderMode {

    /**
     * "Tablero de posicion" (HU-1): the player's own board.
     * Shows the full fleet, no shot listeners attached (read/plan only,
     * or drag-to-place during HU-1's placement phase).
     */
    OWNER_VIEW,

    /**
     * "Tablero principal" (HU-2 / HU-4): the opponent's board as seen
     * during normal play. Undiscovered ships stay hidden ("fog of war"),
     * and mouse-click listeners are attached so the player can fire shots.
     */
    FOG_VIEW,

    /**
     * "Vista de verificacion" (HU-3): read-only window that reveals the
     * opponent's entire board, including undiscovered ships. No click
     * listeners are attached.
     */
    VERIFICATION_VIEW
}

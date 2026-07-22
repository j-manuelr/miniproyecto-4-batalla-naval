package com.example.navalbattle.view;

import com.example.navalbattle.model.board.CellState;
import com.example.navalbattle.view.BoardRenderMode;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/**
 * Creational Factory Method that turns a {@link CellState} (plus the mode
 * the cell is being rendered in) into a ready-to-drop-in JavaFX {@link Node}.
 * <p>
 * Every returned node is sized to fit inside a single board cell and
 * styled exclusively through the CSS classes already defined in
 * {@code battle-view.css} ({@code ship-shape}, {@code enemy-ship-shape},
 * {@code sunk-ship}, {@code water-mark}, {@code hit-mark}, {@code sunk-line}):
 * no colors or fonts are hardcoded here, so re-skinning the game only
 * requires touching the stylesheet (OCP).
 * <p>
 * Adding a new figure for a new {@link CellState} (or a new render mode)
 * only means adding a branch here — no other class needs to change.
 * This is the "family: Creational / pattern: Factory Method" entry from
 * section 5 of the architecture plan.
 */
public final class CellShapeFactory {

    /** Logical size (in pixels) of one cell; matches 380 / 10 from the board panes in the FXML/CSS. */
    public static final double CELL_SIZE = 38.0;

    private static final double MARGIN = 5.0;
    private static final double SHIP_SIDE = CELL_SIZE - 2 * MARGIN;

    private CellShapeFactory() {
        // static factory, not meant to be instantiated
    }

    /**
     * Builds the figure to draw on top of a cell's water background.
     *
     * @param state the cell's current state
     * @param mode  the board's render mode; only used to decide whether a
     *              {@code SHIP} cell should stay hidden (fog of war) and
     *              which ship style (own vs. enemy) applies
     * @return a node ready to be added to the cell's container, or
     *         {@code null} when nothing should be drawn (an untouched,
     *         still-hidden cell)
     */
    public static Node createShape(CellState state, BoardRenderMode mode) {
        return switch (state) {
            case EMPTY -> null;
            case SHIP -> createShipShape(mode);
            case WATER -> createMissShape();
            case HIT -> createHitShape();
            case SUNK -> createSunkShape();
        };
    }

    /** Ship hull: hidden entirely in {@code FOG_VIEW}, otherwise a rounded rectangle. */
    private static Node createShipShape(BoardRenderMode mode) {
        if (mode == BoardRenderMode.FOG_VIEW) {
            // Undiscovered ship: nothing is drawn, the enemy board keeps its fog.
            return null;
        }
        Rectangle hull = new Rectangle(SHIP_SIDE, SHIP_SIDE);
        hull.setArcWidth(6.0);
        hull.setArcHeight(6.0);
        hull.getStyleClass().add(mode == BoardRenderMode.VERIFICATION_VIEW
                ? "enemy-ship-shape"
                : "ship-shape");
        return centered(hull);
    }

    /** Miss: a splash ring plus an "X", both styled via {@code water-mark}. */
    private static Node createMissShape() {
        double radius = CELL_SIZE / 2.0 - MARGIN;

        Circle splash = new Circle(radius);
        splash.setFill(null);
        splash.getStyleClass().add("water-mark");
        splash.setOpacity(0.5);

        Text mark = new Text("X");
        mark.getStyleClass().add("water-mark");
        mark.setX(-radius / 2.2);
        mark.setY(radius / 2.2);

        Group group = new Group(splash, mark);
        return centered(group);
    }

    /** Hit (not sunk yet): an impact circle plus a small "star" polygon, styled via {@code hit-mark}. */
    private static Node createHitShape() {
        double radius = CELL_SIZE / 2.0 - MARGIN;

        Circle impact = new Circle(radius * 0.55);
        impact.getStyleClass().add("hit-mark");

        Polygon star = fourPointStar(radius);
        star.getStyleClass().add("hit-mark");
        star.setOpacity(0.6);

        Group group = new Group(star, impact);
        return centered(group);
    }

    /** Sunk: the darkened hull crossed out by a diagonal line, styled via {@code sunk-ship} / {@code sunk-line}. */
    private static Node createSunkShape() {
        double half = SHIP_SIDE / 2.0;

        Rectangle hull = new Rectangle(-half, -half, SHIP_SIDE, SHIP_SIDE);
        hull.setArcWidth(6.0);
        hull.setArcHeight(6.0);
        hull.getStyleClass().add("sunk-ship");

        Line crossA = new Line(-half, -half, half, half);
        Line crossB = new Line(-half, half, half, -half);
        crossA.getStyleClass().add("sunk-line");
        crossB.getStyleClass().add("sunk-line");

        Group group = new Group(hull, crossA, crossB);
        return centered(group);
    }

    /** A simple 4-point star used as the "impact" decoration on hit cells. */
    private static Polygon fourPointStar(double radius) {
        Polygon star = new Polygon();
        double outer = radius;
        double inner = radius * 0.4;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI / 4 * i;
            double r = (i % 2 == 0) ? outer : inner;
            star.getPoints().addAll(r * Math.cos(angle), r * Math.sin(angle));
        }
        return star;
    }

    /**
     * Wraps a shape (built around its own local origin, i.e. centered on
     * (0,0) or with width/height starting at 0,0) in a {@link Group} whose
     * layout places it centered inside a {@code CELL_SIZE} x {@code CELL_SIZE}
     * cell, so callers never have to think about coordinates.
     */
    private static Node centered(Node shape) {
        Group wrapper = new Group(shape);
        wrapper.setLayoutX(CELL_SIZE / 2.0);
        wrapper.setLayoutY(CELL_SIZE / 2.0);
        if (shape instanceof Rectangle rect) {
            // Rectangles are anchored at their top-left corner, not their center.
            rect.setX(-rect.getWidth() / 2.0);
            rect.setY(-rect.getHeight() / 2.0);
        }
        return wrapper;
    }
}


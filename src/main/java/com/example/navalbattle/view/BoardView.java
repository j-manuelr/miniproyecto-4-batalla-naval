package com.example.navalbattle.view;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.board.Cell;
import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.util.AppConfig;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Builds and refreshes the visual representation (a 10x10 {@link GridPane})
 * of a single {@link Board}.
 * <p>
 * The same class renders all three views described in the architecture
 * plan: the same {@code Board} model can back a {@link BoardRenderMode#OWNER_VIEW},
 * a {@link BoardRenderMode#FOG_VIEW}, or a {@link BoardRenderMode#VERIFICATION_VIEW}
 * — only the mode changes what gets drawn and whether clicks are listened to.
 * That is what keeps this a single class instead of two duplicated board
 * implementations (section 1 of the plan).
 * <p>
 * Usage from a controller:
 * <pre>{@code
 * BoardView enemyBoardView = new BoardView(machinePlayer.getBoard(), BoardRenderMode.FOG_VIEW);
 * enemyBoardPane.getChildren().add(enemyBoardView.getView());
 * enemyBoardView.setOnCellClicked((row, col) -> gameEngine.fireAt(row, col));
 * // after every shot is applied to the model:
 * enemyBoardView.refresh();
 * }</pre>
 */
public class BoardView {

    private static final int SIZE = AppConfig.BOARD_SIZE;
    private static final double CELL_SIZE = CellShapeFactory.CELL_SIZE;
    private static final double BOARD_SIZE = CELL_SIZE * SIZE;

    private static final String HOVER_STYLE = "-fx-background-color: rgba(255,255,255,0.18);";

    private final Board board;
    private final BoardRenderMode mode;

    private final StackPane root = new StackPane();
    private final GridPane grid = new GridPane();
    private final StackPane[][] cellPanes = new StackPane[SIZE][SIZE];

    private CellClickListener clickListener;

    public BoardView(Board board, BoardRenderMode mode) {
        this.board = board;
        this.mode = mode;
        build();
        refresh();
    }

    /** The root node to attach into the surrounding FXML {@code Pane}/{@code StackPane}. */
    public Node getView() {
        return root;
    }

    public BoardRenderMode getMode() {
        return mode;
    }

    /**
     * Registers the callback fired when the player clicks a not-yet-shot
     * cell. Only relevant in {@link BoardRenderMode#FOG_VIEW}; ignored
     * (never even attached) for the other two modes, since neither the
     * owner board nor the verification window should be shootable.
     */
    public void setOnCellClicked(CellClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Re-reads every cell's {@code CellState} from the model and redraws
     * its figure. Call this after any change to the underlying board
     * (a shot was fired, a ship was placed/rotated, etc.).
     */
    public void refresh() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Coordinate coordinate = new Coordinate(row, col);
                Cell cell = board.getCell(coordinate);
                StackPane cellPane = cellPanes[row][col];

                cellPane.getChildren().clear();
                Node shape = CellShapeFactory.createShape(cell.getState(), mode);
                if (shape != null) {
                    cellPane.getChildren().add(shape);
                }

                if (mode == BoardRenderMode.FOG_VIEW) {
                    boolean alreadyShot = board.hasBeenShot(coordinate);
                    cellPane.setCursor(alreadyShot ? Cursor.DEFAULT : Cursor.HAND);
                }
            }
        }
    }

    private void build() {
        root.getStyleClass().add("board-pane");
        root.setPrefSize(BOARD_SIZE, BOARD_SIZE);
        root.setMaxSize(BOARD_SIZE, BOARD_SIZE);

        Rectangle background = new Rectangle(BOARD_SIZE, BOARD_SIZE);
        background.getStyleClass().add("board-background");

        grid.getStyleClass().add("board-grid");
        grid.setGridLinesVisible(true);
        grid.setPrefSize(BOARD_SIZE, BOARD_SIZE);
        for (int i = 0; i < SIZE; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / SIZE);
            grid.getColumnConstraints().add(col);

            RowConstraints row = new RowConstraints();
            row.setPercentHeight(100.0 / SIZE);
            grid.getRowConstraints().add(row);
        }

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                StackPane cellPane = createCellPane(row, col);
                cellPanes[row][col] = cellPane;
                grid.add(cellPane, col, row);
            }
        }

        root.getChildren().addAll(background, grid);
    }

    private StackPane createCellPane(int row, int col) {
        StackPane cellPane = new StackPane();
        cellPane.setPrefSize(CELL_SIZE, CELL_SIZE);

        if (mode == BoardRenderMode.FOG_VIEW) {
            Coordinate coordinate = new Coordinate(row, col);

            // Mouse events (HU-2): only the "tablero principal" is shootable.
            cellPane.setOnMouseClicked(event -> {
                if (clickListener != null && !board.hasBeenShot(coordinate)) {
                    clickListener.onCellClicked(row, col);
                }
            });
            cellPane.setOnMouseEntered(event -> {
                if (!board.hasBeenShot(coordinate)) {
                    cellPane.setStyle(HOVER_STYLE);
                }
            });
            cellPane.setOnMouseExited(event -> cellPane.setStyle(""));
        }

        return cellPane;
    }

    /** Functional callback used to notify a controller that a cell was clicked. */
    @FunctionalInterface
    public interface CellClickListener {
        void onCellClicked(int row, int col);
    }
}
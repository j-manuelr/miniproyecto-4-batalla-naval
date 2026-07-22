package com.example.navalbattle.controller;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.board.Cell;
import com.example.navalbattle.model.board.CellState;
import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.model.exceptions.InvalidShipPlacementException;
import com.example.navalbattle.model.game.GameEngine;
import com.example.navalbattle.model.player.MachinePlayer;
import com.example.navalbattle.model.player.Player;
import com.example.navalbattle.model.player.HumanPlayer;
import com.example.navalbattle.model.ship.Orientation;
import com.example.navalbattle.model.ship.Ship;
import com.example.navalbattle.model.ship.ShipFactory;
import com.example.navalbattle.model.ship.ShipType;
import com.example.navalbattle.persistence.GameRepository;
import com.example.navalbattle.persistence.PlainTextPlayerStatsRepository;
import com.example.navalbattle.persistence.PlayerStatsRepository;
import com.example.navalbattle.persistence.SaveGameManager;
import com.example.navalbattle.persistence.SerializedGameRepository;
import com.example.navalbattle.util.AppConfig;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Controller for HU-1 ({@code ship-placement.fxml}): lets the human player
 * place their entire fleet (one {@code AircraftCarrier}, two
 * {@code Submarine}s, three {@code Destroyer}s and four {@code Frigate}s,
 * per {@link ShipType}) on their own board, horizontally or vertically,
 * validating overlap and out-of-bounds through {@link GameEngine#placeShip},
 * and locking the board once the fleet is complete so it can never be
 * "moved or modified" afterward.
 *
 * <p><b>Adapted to the actual view:</b> {@code ship-placement.fxml} exposes
 * {@code lblStatus}, {@code boardGrid}, {@code fleetListView} and
 * {@code btnStartGame}, plus {@code onBackToMenu}/{@code onClearBoard}/
 * {@code onStartGame} action hooks — there is no separate rotate button or
 * random-placement button, so rotation stays keyboard/right-click only, and
 * {@code fleetListView} is kept in sync with the ships still pending
 * instead of being a static legend.</p>
 *
 * <h2>Board rendering</h2>
 * <p>{@code view.BoardView}/{@code CellShapeFactory} are used by the battle
 * screen (HU-2/HU-4); this placement screen still renders its own grid with
 * plain {@link Rectangle}s so it can draw the per-cell hover preview
 * (green/red) that placement needs and {@code BoardView} does not provide.
 * Placement logic never touches these rectangles directly — it only calls
 * {@link GameEngine#placeShip} and re-reads the resulting {@link Board} —
 * so the two renderings can evolve independently.</p>
 *
 * <h2>"Limpiar Tablero"</h2>
 * <p>{@link Board} has no "unplace a ship" operation, so a full reset is
 * implemented by discarding the current {@link GameEngine}/{@link HumanPlayer}
 * and building fresh ones around a brand-new, empty {@link Board} — safe to
 * do at this stage because no {@code ShotListener}/{@code SaveGameManager}
 * has been wired to the engine yet (that only happens once the fleet is
 * confirmed in {@link #onStartGame()}).</p>
 *
 * <h2>One ship at a time</h2>
 * <p>{@code pendingShips} is a {@link Deque}: ships are placed in a fixed
 * order, so a FIFO queue is the natural way to track "what comes next" —
 * the same rationale documented on
 * {@link com.example.navalbattle.model.shot.strategy.HuntTargetShotStrategy}
 * for its own {@link Deque} of targets.</p>
 */
public class ShipPlacementController {

    private static final String GAME_BOARD_FXML = "/com/example/navalbattle/battle-view.fxml";
    private static final String MAIN_MENU_FXML = "/com/example/navalbattle/main-menu.fxml";

    private static final Color COLOR_EMPTY = Color.web("#bfe3ff");
    private static final Color COLOR_SHIP = Color.web("#5c5c5c");
    private static final Color COLOR_PREVIEW_VALID = Color.web("#8fd694");
    private static final Color COLOR_PREVIEW_INVALID = Color.web("#e88a8a");

    @FXML
    private GridPane boardGrid;

    @FXML
    private Label lblStatus;

    @FXML
    private ListView<String> fleetListView;

    @FXML
    private Button btnStartGame;

    private final GameRepository gameRepository = new SerializedGameRepository();
    private final PlayerStatsRepository playerStatsRepository = new PlainTextPlayerStatsRepository();

    private GameEngine gameEngine;
    private Player humanPlayer;
    private Board humanBoard;

    private final Deque<ShipType> pendingShips = new ArrayDeque<>();
    private Orientation currentOrientation = Orientation.HORIZONTAL;
    private Rectangle[][] cellRectangles;

    /**
     * Injects the engine for the game being set up and prepares this
     * screen. Must be called by whoever loads {@code ship-placement.fxml}
     * (currently {@code MainMenuController#onNewGame}) right after
     * loading, before the screen is shown.
     *
     * @param gameEngine the engine whose human player will place their
     *                   fleet here; its machine player is expected to
     *                   already have its own fleet placed and locked
     */
    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
        this.humanPlayer = gameEngine.getHumanPlayer();
        this.humanBoard = humanPlayer.getBoard();

        resetPendingShipsQueue();
        buildGrid();
        refreshGrid();
        updateInstructions();
        refreshFleetListView();
        btnStartGame.setDisable(true);

        boardGrid.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, new RotateKeyAdapter());
            }
        });
    }

    private void resetPendingShipsQueue() {
        pendingShips.clear();
        for (ShipType type : ShipType.values()) {
            for (int i = 0; i < type.getFleetCount(); i++) {
                pendingShips.add(type);
            }
        }
    }

    // ------------------------------------------------------------------
    // Grid construction and rendering
    // ------------------------------------------------------------------

    private void buildGrid() {
        boardGrid.getChildren().clear();
        cellRectangles = new Rectangle[AppConfig.BOARD_SIZE][AppConfig.BOARD_SIZE];

        for (int row = 0; row < AppConfig.BOARD_SIZE; row++) {
            for (int col = 0; col < AppConfig.BOARD_SIZE; col++) {
                Rectangle cellRectangle = new Rectangle(36, 36);
                cellRectangle.setStroke(Color.web("#2b6ca3"));
                cellRectangle.setFill(COLOR_EMPTY);

                int fixedRow = row;
                int fixedCol = col;
                cellRectangle.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.SECONDARY) {
                        rotate();
                    } else {
                        handleCellClick(fixedRow, fixedCol);
                    }
                });
                cellRectangle.setOnMouseEntered(event -> showPreview(fixedRow, fixedCol));
                cellRectangle.setOnMouseExited(event -> refreshGrid());

                cellRectangles[row][col] = cellRectangle;
                boardGrid.add(cellRectangle, col, row);
            }
        }
    }

    private void refreshGrid() {
        for (int row = 0; row < AppConfig.BOARD_SIZE; row++) {
            for (int col = 0; col < AppConfig.BOARD_SIZE; col++) {
                CellState state = humanBoard.getCell(new Coordinate(row, col)).getState();
                cellRectangles[row][col].setFill(state == CellState.SHIP ? COLOR_SHIP : COLOR_EMPTY);
            }
        }
    }

    /**
     * Highlights, without mutating the board, the cells the current
     * pending ship would occupy if placed at {@code (row, col)} with the
     * current orientation — green if free and within bounds, red
     * otherwise (usability heuristic #5: prevent errors before the click).
     */
    private void showPreview(int row, int col) {
        refreshGrid();
        ShipType next = pendingShips.peek();
        if (next == null) {
            return;
        }

        boolean valid = true;
        for (Coordinate cell : occupiedCoordinates(row, col, next.getSize())) {
            boolean withinBounds = cell.isWithinBounds(AppConfig.BOARD_SIZE);
            boolean free = withinBounds && humanBoard.getCell(cell).getState() == CellState.EMPTY;
            if (!withinBounds || !free) {
                valid = false;
                continue;
            }
            cellRectangles[cell.row()][cell.col()].setFill(COLOR_PREVIEW_VALID);
        }
        if (!valid) {
            for (Coordinate cell : occupiedCoordinates(row, col, next.getSize())) {
                if (cell.isWithinBounds(AppConfig.BOARD_SIZE)) {
                    cellRectangles[cell.row()][cell.col()].setFill(COLOR_PREVIEW_INVALID);
                }
            }
        }
    }

    private List<Coordinate> occupiedCoordinates(int originRow, int originCol, int size) {
        List<Coordinate> cells = new ArrayList<>(size);
        for (int offset = 0; offset < size; offset++) {
            int row = originRow + (currentOrientation == Orientation.VERTICAL ? offset : 0);
            int col = originCol + (currentOrientation == Orientation.HORIZONTAL ? offset : 0);
            cells.add(new Coordinate(row, col));
        }
        return cells;
    }

    // ------------------------------------------------------------------
    // Placement
    // ------------------------------------------------------------------

    private void handleCellClick(int row, int col) {
        ShipType next = pendingShips.peek();
        if (next == null) {
            return;
        }

        Ship ship = ShipFactory.createShip(next);
        try {
            gameEngine.placeShip(humanPlayer, ship, new Coordinate(row, col), currentOrientation);
        } catch (InvalidShipPlacementException e) {
            showWarning("Ubicación inválida", e.getMessage());
            return;
        }

        pendingShips.poll();
        refreshGrid();
        updateInstructions();
        refreshFleetListView();

        if (pendingShips.isEmpty()) {
            btnStartGame.setDisable(false);
            lblStatus.setText("Flota completa. Presiona \"Iniciar Partida\" para comenzar.");
        }
    }

    private void rotate() {
        currentOrientation = (currentOrientation == Orientation.HORIZONTAL)
                ? Orientation.VERTICAL
                : Orientation.HORIZONTAL;
        updateInstructions();
    }

    private void updateInstructions() {
        ShipType next = pendingShips.peek();
        if (next == null) {
            return;
        }
        String orientationText = currentOrientation == Orientation.HORIZONTAL ? "Horizontal" : "Vertical";
        lblStatus.setText("Coloca: " + next.getDisplayName() + " (" + next.getSize()
                + " celdas) — Orientación: " + orientationText);
    }

    private void refreshFleetListView() {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (ShipType type : ShipType.values()) {
            long remaining = pendingShips.stream().filter(t -> t == type).count();
            if (remaining > 0) {
                items.add(type.getDisplayName() + " x" + remaining + " (" + type.getSize() + " celdas)");
            }
        }
        fleetListView.setItems(items);
    }

    // ------------------------------------------------------------------
    // Actions ("Volver al Menú" / "Limpiar Tablero" / "Iniciar Partida")
    // ------------------------------------------------------------------

    /**
     * Discards the in-progress placement and starts over on a brand-new,
     * empty board. Safe at this stage because the current
     * {@link GameEngine} has no listeners attached yet — those are only
     * registered once the fleet is confirmed in {@link #onStartGame()}.
     */
    @FXML
    private void onClearBoard() {
        Alert confirmation = new Alert(AlertType.CONFIRMATION);
        confirmation.setTitle("Batalla Naval");
        confirmation.setHeaderText("¿Limpiar el tablero?");
        confirmation.setContentText("Perderás la colocación actual y empezarás de nuevo.");
        Optional<ButtonType> choice = confirmation.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }

        String nickname = humanPlayer.getNickname();
        MachinePlayer machinePlayer = gameEngine.getMachinePlayer();

        Board freshBoard = new Board();
        HumanPlayer freshHuman = new HumanPlayer(nickname, freshBoard);

        this.gameEngine = new GameEngine(freshHuman, machinePlayer);
        this.humanPlayer = freshHuman;
        this.humanBoard = freshBoard;
        this.currentOrientation = Orientation.HORIZONTAL;

        resetPendingShipsQueue();
        refreshGrid();
        updateInstructions();
        refreshFleetListView();
        btnStartGame.setDisable(true);
    }

    /**
     * Locks the human board (HU-1: ships can no longer be moved or
     * modified), builds the {@link SaveGameManager} that will keep
     * autosaving for the rest of the match, and navigates to the battle
     * screen (HU-2/HU-4).
     */
    @FXML
    private void onStartGame() {
        if (!humanBoard.isFleetComplete()) {
            return;
        }
        gameEngine.confirmFleetPlacement(humanPlayer);

        SaveGameManager saveGameManager =
                new SaveGameManager(gameEngine, gameRepository, playerStatsRepository);
        saveGameManager.startAutoSaving();

        navigateToGameBoard(saveGameManager);
    }

    @FXML
    private void onBackToMenu() {
        cancelToMainMenu(boardGrid.getScene());
    }

    private void cancelToMainMenu(Scene scene) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_MENU_FXML));
            Parent root = loader.load();
            scene.setRoot(root);
        } catch (IOException e) {
            showWarning("No se pudo volver al menú principal", e.getMessage());
        }
    }

    private void navigateToGameBoard(SaveGameManager saveGameManager) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(GAME_BOARD_FXML));
            Parent root = loader.load();

            GameBoardController controller = loader.getController();
            controller.setGameEngine(gameEngine);
            controller.setSaveGameManager(saveGameManager);

            Stage stage = (Stage) boardGrid.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showWarning("No se pudo abrir el tablero de batalla", e.getMessage());
        }
    }

    private void showWarning(String header, String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Batalla Naval");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ------------------------------------------------------------------
    // Keyboard adapter (Adapter pattern, inner class)
    // ------------------------------------------------------------------

    /**
     * Translates raw {@link KeyEvent}s into HU-1's placement actions:
     * {@code R} rotates the pending ship, {@code Enter} confirms placement
     * once the fleet is complete, and {@code Escape} cancels back to the
     * main menu. Kept as a private inner class because it exists purely to
     * bridge JavaFX's key-event API to this controller's own methods and
     * has no reason to be reused outside this screen.
     */
    private class RotateKeyAdapter implements javafx.event.EventHandler<KeyEvent> {

        @Override
        public void handle(KeyEvent event) {
            switch (event.getCode()) {
                case R -> rotate();
                case ENTER -> {
                    if (!btnStartGame.isDisabled()) {
                        onStartGame();
                    }
                }
                case ESCAPE -> cancelToMainMenu(boardGrid.getScene());
                default -> { /* no-op for any other key */ }
            }
        }
    }
}
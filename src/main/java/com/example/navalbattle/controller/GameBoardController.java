package com.example.navalbattle.controller;

import com.example.navalbattle.concurrency.GameClockThread;
import com.example.navalbattle.concurrency.MachineTurnTask;
import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.model.exceptions.CellAlreadyShotException;
import com.example.navalbattle.model.exceptions.InvalidTurnException;
import com.example.navalbattle.model.game.GameEngine;
import com.example.navalbattle.model.game.TurnManager;
import com.example.navalbattle.model.game.events.GameOverListener;
import com.example.navalbattle.model.game.events.ShotListener;
import com.example.navalbattle.model.game.events.TurnListener;
import com.example.navalbattle.model.player.Player;
import com.example.navalbattle.model.shot.Shot;
import com.example.navalbattle.persistence.SaveGameManager;
import com.example.navalbattle.view.BoardRenderMode;
import com.example.navalbattle.view.BoardView;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller for the battle screen ({@code battle-view.fxml}): coordinates
 * HU-2 (the human player's shots) and HU-4 (the machine's shots), keeping
 * both boards' rendering in sync in real time and delegating persistence
 * entirely to the already-built {@link SaveGameManager} it receives.
 *
 * <p><b>Adapted to the actual view:</b> {@code battle-view.fxml} exposes
 * {@code playerBoardPane}/{@code enemyBoardPane} (plain {@link Pane}s meant
 * to host a {@link BoardView}, per the architecture plan) instead of a
 * hand-drawn {@code GridPane} of rectangles, and a
 * {@code showEnemyBoardCheckBox} for HU-3 instead of a dedicated button.
 * {@code newGameButton} and {@code startGameButton} exist in the FXML with
 * no {@code onAction} declared in the file, so this controller wires them
 * (and the checkbox) programmatically in {@link #initialize()}:
 * {@code startGameButton} is hidden — the fleet is already fixed by the
 * time this screen loads — and {@code newGameButton} is repurposed as
 * "volver al menú" (the match is already autosaved, so leaving loses
 * nothing). {@code turnLabel}, {@code statusLabel}, {@code timerLabel},
 * {@code humanShipsSunkLabel} and {@code machineShipsSunkLabel} were added
 * to the FXML for this controller to report system state (usability
 * heuristic #1) — the original view had no place to show it.</p>
 *
 * <h2>Two boards, two render modes, one class</h2>
 * <p>Per the architecture plan's key decision, both boards shown here are
 * the same {@link Board} class: the human's own board is rendered with
 * {@link BoardRenderMode#OWNER_VIEW} (full fleet, no clicks), while the
 * machine's board is rendered with {@link BoardRenderMode#FOG_VIEW}
 * (undiscovered ships hidden, clickable) — the "tablero principal" the
 * human actually shoots at.</p>
 *
 * <h2>Threading contract</h2>
 * <p>{@link MachineTurnTask} invokes {@code GameEngine.fireAsMachine}
 * directly from its background thread, and {@code GameEngine} notifies
 * every listener synchronously from whatever thread calls {@code fireAs*}.
 * That means this controller's {@link ShotListener}/{@link TurnListener}/
 * {@link GameOverListener} callbacks run on the JavaFX Application Thread
 * when the <em>human</em> fires (a direct call from a mouse handler, via
 * {@link BoardView}) but on the background task's thread when the
 * <em>machine</em> fires. Every callback in this class therefore wraps its
 * UI work in {@link Platform#runLater(Runnable)} rather than assuming
 * either thread.</p>
 */
public class GameBoardController implements ShotListener, TurnListener, GameOverListener {

    private static final String MAIN_MENU_FXML = "/com/example/navalbattle/main-menu.fxml";

    @FXML
    private Pane playerBoardPane;

    @FXML
    private Pane enemyBoardPane;

    @FXML
    private Button newGameButton;

    @FXML
    private Button startGameButton;

    @FXML
    private ListView<String> fleetListView;

    @FXML
    private CheckBox showEnemyBoardCheckBox;

    @FXML
    private Label turnLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label timerLabel;

    @FXML
    private Label humanShipsSunkLabel;

    @FXML
    private Label machineShipsSunkLabel;

    private GameEngine gameEngine;
    private SaveGameManager saveGameManager;

    private BoardView humanBoardView;
    private BoardView machineBoardView;

    private final ExecutorService machineTurnExecutor =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "machine-turn-thread");
                thread.setDaemon(true);
                return thread;
            });

    private GameClockThread clockThread;

    /**
     * Called automatically once {@code @FXML} fields are injected. Wires
     * the two controls that exist in the FXML without an {@code onAction}
     * ({@code newGameButton}, {@code showEnemyBoardCheckBox}) and hides
     * {@code startGameButton}, which has no purpose on this screen (the
     * fleet is already locked by the time it loads).
     */
    @FXML
    private void initialize() {
        startGameButton.setVisible(false);
        startGameButton.setManaged(false);

        newGameButton.setText("Volver al menú");
        newGameButton.setOnAction(event -> handleExitToMenu());

        showEnemyBoardCheckBox.setOnAction(event -> {
            if (showEnemyBoardCheckBox.isSelected()) {
                try {
                    openVerificationView();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                // Momentary trigger, not a persistent toggle: this window
                // is closed independently via its own "Cerrar" button.
                showEnemyBoardCheckBox.setSelected(false);
            }
        });
    }

    // ------------------------------------------------------------------
    // Dependency injection (called by MainMenuController / ShipPlacementController)
    // ------------------------------------------------------------------

    /**
     * Injects the engine for the game already in progress (either just
     * confirmed by {@code ShipPlacementController}, or restored by
     * {@code MainMenuController}'s "Continuar"). Must be called before
     * {@link #setSaveGameManager(SaveGameManager)}, which finishes wiring
     * this screen and starts the match clock.
     *
     * @param gameEngine the live engine backing this battle screen
     */
    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    /**
     * Injects the already-constructed, already-autosaving
     * {@link SaveGameManager} for this game, finishes initializing the
     * screen (builds both {@link BoardView}s, subscribes as a listener to
     * {@link #gameEngine}, starts the match clock, and — if a resumed game
     * was left mid machine-turn — kicks off that turn) and reports any
     * background autosave failure back to the player as a non-blocking
     * warning.
     *
     * @param saveGameManager the manager already wrapping {@link #gameEngine}
     */
    public void setSaveGameManager(SaveGameManager saveGameManager) {
        this.saveGameManager = saveGameManager;
        this.saveGameManager.setOnSaveError(failure ->
                Platform.runLater(() -> statusLabel.setText("Aviso: no se pudo autoguardar la partida.")));

        gameEngine.addShotListener(this);
        gameEngine.addTurnListener(this);
        gameEngine.addGameOverListener(this);

        buildBoards();
        updateTurnLabel(gameEngine.getCurrentTurn());
        updateSunkCounters();
        startClock();

        // A resumed game may have been saved mid machine-turn (a hit lets
        // the machine keep shooting); make sure play actually continues
        // instead of leaving the board waiting on a turn nobody will start.
        continueIfMachineTurn();
    }

    // ------------------------------------------------------------------
    // Board construction (BoardView, per the architecture plan)
    // ------------------------------------------------------------------

    private void buildBoards() {
        Board humanBoard = gameEngine.getHumanPlayer().getBoard();
        Board machineBoard = gameEngine.getMachinePlayer().getBoard();

        humanBoardView = new BoardView(humanBoard, BoardRenderMode.OWNER_VIEW);
        playerBoardPane.getChildren().setAll(humanBoardView.getView());

        machineBoardView = new BoardView(machineBoard, BoardRenderMode.FOG_VIEW);
        machineBoardView.setOnCellClicked(this::handleCellClicked);
        enemyBoardPane.getChildren().setAll(machineBoardView.getView());
    }

    // ------------------------------------------------------------------
    // Human shots (HU-2)
    // ------------------------------------------------------------------

    /**
     * Called by {@link BoardView} only for cells it has not already marked
     * as shot; this method still guards on whose turn it is, since
     * {@code BoardView} has no notion of turns.
     */
    private void handleCellClicked(int row, int col) {
        if (!isHumanTurnActive()) {
            return;
        }
        Coordinate target = new Coordinate(row, col);
        try {
            gameEngine.fireAsHuman(target);
        } catch (CellAlreadyShotException | InvalidTurnException e) {
            // Defensive only: BoardView already blocks re-shooting an
            // already-targeted cell, so this should not normally happen.
            return;
        }
        continueIfMachineTurn();
    }

    private boolean isHumanTurnActive() {
        return !gameEngine.isGameOver() && gameEngine.getCurrentTurn() == TurnManager.Turn.HUMAN;
    }

    // ------------------------------------------------------------------
    // Machine shots (HU-4)
    // ------------------------------------------------------------------

    /**
     * Launches one more {@link MachineTurnTask} if, after the shot that was
     * just resolved, it is still the machine's turn and the game hasn't
     * ended — covering both a fresh machine turn and a chained shot after
     * its own hit.
     */
    private void continueIfMachineTurn() {
        if (gameEngine.isGameOver() || gameEngine.getCurrentTurn() != TurnManager.Turn.MACHINE) {
            return;
        }

        statusLabel.setText("La máquina está pensando...");
        MachineTurnTask task = new MachineTurnTask(gameEngine);
        task.setOnSucceeded(event -> {
            statusLabel.setText(" ");
            continueIfMachineTurn();
        });
        task.setOnFailed(event -> statusLabel.setText("La máquina no pudo completar su turno."));
        machineTurnExecutor.submit(task);
    }

    // ------------------------------------------------------------------
    // ShotListener / TurnListener / GameOverListener
    // ------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Wrapped in {@link Platform#runLater}: see the threading contract
     * documented on the class.</p>
     */
    @Override
    public void onShotFired(Player shooter, Shot shot) {
        Platform.runLater(() -> {
            humanBoardView.refresh();
            machineBoardView.refresh();
            updateSunkCounters();
            statusLabel.setText(describe(shooter, shot));
        });
    }

    private String describe(Player shooter, Shot shot) {
        String who = (shooter == gameEngine.getHumanPlayer()) ? "Tú" : "La máquina";
        String outcome = switch (shot.result()) {
            case WATER -> "agua";
            case HIT -> "¡tocado!";
            case SUNK -> "¡hundido!";
        };
        return who + " disparó en " + shot.coordinate() + ": " + outcome;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Wrapped in {@link Platform#runLater}: see the threading contract
     * documented on the class.</p>
     */
    @Override
    public void onTurnChanged(TurnManager.Turn newTurn) {
        Platform.runLater(() -> updateTurnLabel(newTurn));
    }

    private void updateTurnLabel(TurnManager.Turn turn) {
        turnLabel.setText(turn == TurnManager.Turn.HUMAN ? "Tu turno" : "Turno de la máquina");
    }

    private void updateSunkCounters() {
        humanShipsSunkLabel.setText(
                "Barcos hundidos por ti: " + gameEngine.getHumanPlayer().getStats().getSunkShipsCount());
        machineShipsSunkLabel.setText(
                "Barcos hundidos por la máquina: " + gameEngine.getMachinePlayer().getStats().getSunkShipsCount());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Wrapped in {@link Platform#runLater}: see the threading contract
     * documented on the class. Stops the match clock and the machine-turn
     * executor, since no further shots can legally be fired once the game
     * has ended.</p>
     */
    @Override
    public void onGameOver(Player winner) {
        Platform.runLater(() -> {
            stopClock();
            machineTurnExecutor.shutdown();

            boolean humanWon = winner == gameEngine.getHumanPlayer();
            statusLabel.setText(humanWon ? "¡Ganaste la partida!" : "La máquina ganó la partida.");

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Batalla Naval");
            alert.setHeaderText(humanWon ? "¡Felicidades, hundiste toda la flota enemiga!"
                    : "La máquina hundió toda tu flota.");
            alert.setContentText("Puedes volver al menú principal para iniciar otra partida.");
            alert.showAndWait();
        });
    }

    // ------------------------------------------------------------------
    // Match clock
    // ------------------------------------------------------------------

    private void startClock() {
        clockThread = new GameClockThread(elapsedSeconds -> timerLabel.setText(formatElapsed(elapsedSeconds)));
        clockThread.start();
    }

    private void stopClock() {
        if (clockThread != null) {
            clockThread.stopClock();
        }
    }

    private String formatElapsed(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // ------------------------------------------------------------------
    // Verification view (HU-3) and navigation
    // ------------------------------------------------------------------

    /**
     * Opens the read-only verification view (HU-3) over the machine's
     * board, in its own window. Available at any point during the match —
     * it never modifies game state. Triggered by
     * {@code showEnemyBoardCheckBox} (see {@link #initialize()}).
     */
    private void openVerificationView() throws IOException {
        Window owner = enemyBoardPane.getScene().getWindow();
        OpponentBoardViewController.open(gameEngine.getMachinePlayer().getBoard(), owner);
    }

    /**
     * Returns to the main menu. Since {@link #saveGameManager} has already
     * autosaved after every shot, leaving mid-match loses nothing — the
     * confirmation dialog exists only so an accidental click doesn't
     * interrupt the player's flow (usability heuristic #3: user control and
     * freedom, without sacrificing #5: error prevention).
     */
    private void handleExitToMenu() {
        Alert confirmation = new Alert(AlertType.CONFIRMATION);
        confirmation.setTitle("Batalla Naval");
        confirmation.setHeaderText("¿Volver al menú principal?");
        confirmation.setContentText("La partida ya quedó guardada; podrás continuarla luego.");

        Optional<ButtonType> choice = confirmation.showAndWait();
        if (choice.isPresent() && choice.get() == ButtonType.OK) {
            stopClock();
            machineTurnExecutor.shutdown();
            navigateToMainMenu();
        }
    }

    private void navigateToMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_MENU_FXML));
            Parent root = loader.load();
            Stage stage = (Stage) newGameButton.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Batalla Naval");
            alert.setHeaderText("No se pudo volver al menú principal");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
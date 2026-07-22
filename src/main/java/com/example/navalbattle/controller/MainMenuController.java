package com.example.navalbattle.controller;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.exceptions.GamePersistenceException;
import com.example.navalbattle.model.exceptions.InvalidShipPlacementException;
import com.example.navalbattle.model.game.GameEngine;
import com.example.navalbattle.model.game.GameState;
import com.example.navalbattle.model.player.HumanPlayer;
import com.example.navalbattle.model.player.MachinePlayer;
import com.example.navalbattle.model.player.PlayerStats;
import com.example.navalbattle.model.placement.RandomFleetPlacementStrategy;
import com.example.navalbattle.model.shot.strategy.HuntTargetShotStrategy;
import com.example.navalbattle.persistence.GameRepository;
import com.example.navalbattle.persistence.PlayerStatsRepository;
import com.example.navalbattle.persistence.PlayerStatsSnapshot;
import com.example.navalbattle.persistence.PlainTextPlayerStatsRepository;
import com.example.navalbattle.persistence.SaveGameManager;
import com.example.navalbattle.persistence.SerializedGameRepository;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Controller for the game's landing screen ({@code main-menu.fxml}).
 *
 * <p><b>Adapted to the actual view:</b> {@code main-menu.fxml} only exposes
 * four buttons ({@code btnNewGame}, {@code btnContinue}, {@code btnHelp},
 * {@code btnExit}) — there is no nickname {@code TextField} and no status
 * {@code Label} in the FXML. This controller therefore asks for the
 * nickname through a {@link TextInputDialog} when "Nueva Partida" is
 * clicked, and surfaces the "Continuar" availability by editing
 * {@code btnContinue}'s own text (e.g. {@code "Continuar Partida (Ada)"})
 * instead of a separate label that does not exist in the view.</p>
 *
 * <p>This controller intentionally does <b>not</b> depend on
 * {@link SaveGameManager} to check for or load an existing save: that class
 * requires an already-built {@link GameEngine} (it registers itself as a
 * {@code ShotListener} on construction), which does not exist yet at the
 * main-menu stage. Instead, {@code MainMenuController} talks directly to
 * {@link GameRepository} and {@link PlayerStatsRepository} to inspect and
 * reconstruct a saved {@link GameState}. Once a {@link GameEngine} exists
 * (new or resumed), a {@link SaveGameManager} is created around it and
 * handed to {@code GameBoardController}, which is the only screen where
 * autosaving after every shot (HU-5) actually needs to run.</p>
 *
 * <h2>Navigation contract with the next screens</h2>
 * <ul>
 *   <li>{@code ShipPlacementController.setGameEngine(GameEngine)} — for a
 *       fresh game, whose human board is still unlocked and empty.</li>
 *   <li>{@code GameBoardController.setGameEngine(GameEngine)} and
 *       {@code GameBoardController.setSaveGameManager(SaveGameManager)} —
 *       used both when placement is confirmed (new game) and when a saved
 *       game is resumed directly into the battle screen.</li>
 * </ul>
 */
public class MainMenuController {

    private static final String SHIP_PLACEMENT_FXML = "/com/example/navalbattle/ship-placement.fxml";
    private static final String GAME_BOARD_FXML = "/com/example/navalbattle/battle-view.fxml";
    private static final DateTimeFormatter SAVED_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Button btnNewGame;

    @FXML
    private Button btnContinue;

    @FXML
    private Button btnHelp;

    @FXML
    private Button btnExit;

    private final GameRepository gameRepository = new SerializedGameRepository();
    private final PlayerStatsRepository playerStatsRepository = new PlainTextPlayerStatsRepository();

    /**
     * Called automatically by the {@link FXMLLoader} once all
     * {@code @FXML} fields have been injected. Looks for the most recent
     * save so "Continuar" can be disabled when there is nothing to resume
     * (usability heuristic #5: error prevention).
     */
    @FXML
    private void initialize() {
        btnContinue.setDisable(true);
        try {
            Optional<GameState> latest = gameRepository.loadLatest();
            latest.ifPresent(this::showResumableGame);
        } catch (GamePersistenceException e) {
            showError("No se pudo leer la partida guardada", e.getMessage());
        }
    }

    private void showResumableGame(GameState state) {
        btnContinue.setDisable(false);
        btnContinue.setText("Continuar Partida (" + state.getNickname()
                + " — " + state.getSavedAt().format(SAVED_AT_FORMAT) + ")");
    }

    // ------------------------------------------------------------------
    // "Nueva partida"
    // ------------------------------------------------------------------

    /**
     * Starts a brand-new game: prompts for the nickname (no dedicated field
     * exists in {@code main-menu.fxml}), builds a fresh {@link GameEngine}
     * with an empty human board and a machine board whose fleet has
     * already been placed at random ({@link RandomFleetPlacementStrategy},
     * HU-4), and navigates to the ship-placement screen (HU-1).
     */
    @FXML
    private void onNewGame() {
        Optional<String> nicknameInput = askNickname();
        if (nicknameInput.isEmpty()) {
            return;
        }
        String nickname = nicknameInput.get();

        Board humanBoard = new Board();
        HumanPlayer human = new HumanPlayer(nickname, humanBoard);

        Board machineBoard = new Board();
        try {
            new RandomFleetPlacementStrategy().placeFleet(machineBoard);
        } catch (InvalidShipPlacementException e) {
            showError("No se pudo preparar la flota de la máquina", e.getMessage());
            return;
        }
        machineBoard.lockPlacement();
        MachinePlayer machine = new MachinePlayer(machineBoard, new HuntTargetShotStrategy());

        GameEngine engine = new GameEngine(human, machine);
        navigateToShipPlacement(engine);
    }

    private Optional<String> askNickname() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Batalla Naval");
        dialog.setHeaderText("Nueva partida");
        dialog.setContentText("Ingresa tu nickname:");

        while (true) {
            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return Optional.empty();
            }
            String trimmed = result.get().trim();
            if (!trimmed.isEmpty()) {
                return Optional.of(trimmed);
            }
            dialog.setContentText("El nickname no puede estar vacío. Ingresa tu nickname:");
        }
    }

    // ------------------------------------------------------------------
    // "Continuar"
    // ------------------------------------------------------------------

    /**
     * Resumes the most recently saved game (HU-5): reconstructs both
     * players (boards and stats) from disk, restores whose turn was
     * active, wires a {@link SaveGameManager} so autosaving keeps working
     * for the rest of the match, and navigates straight to the battle
     * screen — skipping HU-1's placement phase entirely.
     */
    @FXML
    private void onContinueGame() {
        try {
            Optional<GameState> latest = gameRepository.loadLatest();
            if (latest.isEmpty()) {
                btnContinue.setDisable(true);
                showError("Sin partida guardada", "No hay ninguna partida para continuar.");
                return;
            }

            GameState state = latest.get();
            GameEngine engine = rebuildEngine(state);
            SaveGameManager saveGameManager =
                    new SaveGameManager(engine, gameRepository, playerStatsRepository);
            saveGameManager.startAutoSaving();

            navigateToGameBoard(engine, saveGameManager);
        } catch (GamePersistenceException e) {
            showError("No se pudo cargar la partida guardada", e.getMessage());
        }
    }

    private GameEngine rebuildEngine(GameState state) throws GamePersistenceException {
        Optional<PlayerStatsSnapshot> statsSnapshot = playerStatsRepository.load(state.getNickname());

        int humanShipsSunk = statsSnapshot.map(PlayerStatsSnapshot::humanShipsSunk)
                .orElse(state.getHumanShipsSunk());
        int machineShipsSunk = statsSnapshot.map(PlayerStatsSnapshot::machineShipsSunk)
                .orElse(state.getMachineShipsSunk());

        PlayerStats humanStats = new PlayerStats(state.getNickname(), humanShipsSunk);
        PlayerStats machineStats = new PlayerStats("Machine", machineShipsSunk);

        HumanPlayer human = new HumanPlayer(state.getHumanBoard(), humanStats);
        // A resumed machine always starts its "hunt" state fresh: any
        // in-progress targeting queue is not part of GameState — a
        // deliberate, minor trade-off (see MachinePlayer's javadoc).
        MachinePlayer machine =
                new MachinePlayer(state.getMachineBoard(), machineStats, new HuntTargetShotStrategy());

        GameEngine engine = new GameEngine(human, machine);
        engine.restoreTurn(state);
        return engine;
    }

    // ------------------------------------------------------------------
    // "¿Cómo Jugar?" / "Salir"
    // ------------------------------------------------------------------

    /**
     * Usability heuristic #10: help and documentation, accessible from the
     * main menu.
     */
    @FXML
    private void onHelp() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Batalla Naval");
        alert.setHeaderText("¿Cómo jugar?");
        alert.setContentText("""
                1. Coloca tu flota: 1 portaaviones (4), 2 submarinos (3), \
                3 destructores (2) y 4 fragatas (1).
                2. Usa [R] o clic derecho para rotar un barco antes de colocarlo.
                3. Dispara en el tablero principal (el de la máquina); \
                acertar te deja disparar de nuevo, fallar pasa el turno.
                4. La partida se autoguarda tras cada disparo: puedes \
                salir y continuarla luego desde "Continuar Partida".""");
        alert.showAndWait();
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    // ------------------------------------------------------------------
    // Navigation helpers
    // ------------------------------------------------------------------

    private void navigateToShipPlacement(GameEngine engine) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(SHIP_PLACEMENT_FXML));
            Parent root = loader.load();

            ShipPlacementController controller = loader.getController();
            controller.setGameEngine(engine);

            switchScene(root);
        } catch (IOException e) {
            showError("No se pudo abrir la pantalla de colocación de barcos", e.getMessage());
        }
    }

    private void navigateToGameBoard(GameEngine engine, SaveGameManager saveGameManager) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(GAME_BOARD_FXML));
            Parent root = loader.load();

            GameBoardController controller = loader.getController();
            controller.setGameEngine(engine);
            controller.setSaveGameManager(saveGameManager);

            switchScene(root);
        } catch (IOException e) {
            showError("No se pudo abrir el tablero de batalla", e.getMessage());
        }
    }

    private void switchScene(Parent root) {
        Window window = btnNewGame.getScene().getWindow();
        Stage stage = (Stage) window;
        stage.getScene().setRoot(root);
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Batalla Naval");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
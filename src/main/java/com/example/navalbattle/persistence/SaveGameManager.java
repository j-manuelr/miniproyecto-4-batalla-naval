package com.example.navalbattle.persistence;

import com.example.navalbattle.concurrency.AutoSaveTask;
import com.example.navalbattle.model.exceptions.GamePersistenceException;
import com.example.navalbattle.model.game.GameEngine;
import com.example.navalbattle.model.game.GameState;
import com.example.navalbattle.model.game.events.ShotListener;
import com.example.navalbattle.model.player.Player;
import com.example.navalbattle.model.shot.Shot;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Coordinates persistence for a single game (HU-5). {@code SaveGameManager}
 * plays three roles at once:
 *
 * <ul>
 *   <li><b>Observer:</b> it subscribes to {@link GameEngine} as a
 *       {@link ShotListener} and triggers an autosave after every shot
 *       — human or machine, water, hit or sunk — without the engine or the
 *       controllers ever having to know persistence exists.</li>
 *   <li><b>Memento caretaker:</b> {@link GameEngine} is the Memento
 *       <em>originator</em> ({@link GameEngine#createSnapshot()} /
 *       {@link GameEngine#restoreTurn(GameState)}); this class only ever
 *       stores and hands back opaque {@link GameState} snapshots through
 *       {@link GameRepository}, never reaching into their contents.</li>
 *   <li><b>Facade:</b> {@link #saveNow()}, {@link #loadLatestGame()} and
 *       {@link #loadGame(String)} give controllers ({@code MainMenuController}'s
 *       "Continuar" button, for instance) one simple entry point instead of
 *       juggling {@link GameRepository} and {@link PlayerStatsRepository}
 *       separately.</li>
 * </ul>
 *
 * <h2>Concurrency</h2>
 * <p>Every autosave triggered by {@link #onShotFired} runs on a dedicated,
 * single-thread {@link ExecutorService} via {@link AutoSaveTask}, so the
 * (blocking) file I/O never runs on the JavaFX Application Thread. The
 * snapshot handed to each task is created synchronously, on the calling
 * thread, at the exact moment the shot is resolved — before any further
 * shot can be processed — so the bytes being written always correspond to
 * that shot and are never overtaken by a later mutation of the live
 * {@code Board} objects. See {@link AutoSaveTask} for how concurrent
 * autosave writes for the same game are themselves serialized.</p>
 */
public class SaveGameManager implements ShotListener {

    private final GameEngine gameEngine;
    private final GameRepository gameRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final ExecutorService autosaveExecutor;
    private final Object writeLock = new Object();

    private volatile Consumer<GamePersistenceException> onSaveError = failure -> { };

    /**
     * Creates a manager backed by a real, single-thread daemon executor —
     * the configuration used in production.
     *
     * @param gameEngine            the engine whose shots trigger autosaves
     *                              and whose {@link GameEngine#createSnapshot()}
     *                              produces what gets saved
     * @param gameRepository        persists the serialized board snapshot
     * @param playerStatsRepository persists the plain-text stats summary
     */
    public SaveGameManager(GameEngine gameEngine,
                           GameRepository gameRepository,
                           PlayerStatsRepository playerStatsRepository) {
        this(gameEngine, gameRepository, playerStatsRepository,
                Executors.newSingleThreadExecutor(SaveGameManager::newDaemonAutosaveThread));
    }

    /**
     * Creates a manager backed by a caller-supplied executor. Exposed so
     * unit tests can pass an executor whose {@code submit} runs
     * synchronously (or whose {@link Future} can simply be awaited) instead
     * of the real background thread, making autosave assertions
     * deterministic.
     *
     * @param gameEngine            the engine whose shots trigger autosaves
     * @param gameRepository        persists the serialized board snapshot
     * @param playerStatsRepository persists the plain-text stats summary
     * @param autosaveExecutor      executes every {@link AutoSaveTask}
     */
    public SaveGameManager(GameEngine gameEngine,
                           GameRepository gameRepository,
                           PlayerStatsRepository playerStatsRepository,
                           ExecutorService autosaveExecutor) {
        this.gameEngine = Objects.requireNonNull(gameEngine, "gameEngine");
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository");
        this.playerStatsRepository = Objects.requireNonNull(playerStatsRepository, "playerStatsRepository");
        this.autosaveExecutor = Objects.requireNonNull(autosaveExecutor, "autosaveExecutor");
    }

    private static Thread newDaemonAutosaveThread(Runnable task) {
        Thread thread = new Thread(task, "autosave-thread");
        thread.setDaemon(true);
        return thread;
    }

    /**
     * Starts the autosave-after-every-shot behavior by registering this
     * manager as a {@link ShotListener} on the engine. Separate from the
     * constructor so a controller can finish wiring the engine (placement,
     * other listeners) before autosaving becomes active.
     */
    public void startAutoSaving() {
        gameEngine.addShotListener(this);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Triggers one autosave per shot, regardless of its result. The
     * returned {@link Future} from the underlying submission is
     * intentionally not exposed here — callers that need to know when an
     * autosave completes (typically tests) should call
     * {@link #requestAutosave()} directly instead.</p>
     */
    @Override
    public void onShotFired(Player shooter, Shot shot) {
        requestAutosave();
    }

    /**
     * Captures the current game state and submits an {@link AutoSaveTask}
     * for it. Called automatically after every shot via
     * {@link #onShotFired}; also safe to call directly (e.g. for a manual
     * "save now" affordance that should not block the caller).
     *
     * @return a {@link Future} that completes once this autosave has been
     *         written (or has failed and been reported to the
     *         save-error handler)
     */
    public Future<?> requestAutosave() {
        GameState snapshot = gameEngine.createSnapshot();
        PlayerStatsSnapshot statsSnapshot = toStatsSnapshot(snapshot);
        AutoSaveTask task = new AutoSaveTask(
                snapshot, statsSnapshot, gameRepository, playerStatsRepository, writeLock, this::reportSaveError);
        return autosaveExecutor.submit(task);
    }

    /**
     * Registers a callback invoked whenever a background autosave fails.
     * By default failures are silently swallowed (an autosave is a
     * best-effort convenience, not something that should crash the game),
     * so a controller that wants to surface a warning to the player must
     * opt in explicitly.
     *
     * @param handler called with the failure; never invoked concurrently
     *                with itself since every {@link AutoSaveTask} runs on
     *                the same single-thread executor
     */
    public void setOnSaveError(Consumer<GamePersistenceException> handler) {
        this.onSaveError = Objects.requireNonNull(handler, "handler");
    }

    private void reportSaveError(GamePersistenceException failure) {
        onSaveError.accept(failure);
    }

    // ------------------------------------------------------------------
    // Facade: explicit save / load, used outside the autosave flow
    // ------------------------------------------------------------------

    /**
     * Synchronously saves the current game state and stats, on the calling
     * thread. Intended for an explicit "Guardar" action or for a clean
     * shutdown, where the caller can afford to wait for the write to
     * finish and wants to know immediately if it failed.
     *
     * @throws GamePersistenceException if either file cannot be written
     */
    public void saveNow() throws GamePersistenceException {
        GameState snapshot = gameEngine.createSnapshot();
        PlayerStatsSnapshot statsSnapshot = toStatsSnapshot(snapshot);
        synchronized (writeLock) {
            gameRepository.save(snapshot);
            playerStatsRepository.save(statsSnapshot);
        }
    }

    /**
     * Loads the most recently saved game, for the "Continuar" option in the
     * main menu (HU-5).
     *
     * @return the most recent snapshot, or {@link Optional#empty()} if
     *         nothing has been saved yet
     * @throws GamePersistenceException if a save exists but cannot be read
     */
    public Optional<GameState> loadLatestGame() throws GamePersistenceException {
        return gameRepository.loadLatest();
    }

    /**
     * Loads the game previously saved for a specific nickname.
     *
     * @param nickname the human player's nickname
     * @return the loaded snapshot, or {@link Optional#empty()} if none
     *         exists for that nickname
     * @throws GamePersistenceException if a save exists but cannot be read
     */
    public Optional<GameState> loadGame(String nickname) throws GamePersistenceException {
        return gameRepository.load(nickname);
    }

    /**
     * Loads the plain-text stats summary for a specific nickname.
     *
     * @param nickname the human player's nickname
     * @return the loaded stats, or {@link Optional#empty()} if none exist
     *         for that nickname
     * @throws GamePersistenceException if a file exists but cannot be
     *         parsed
     */
    public Optional<PlayerStatsSnapshot> loadStats(String nickname) throws GamePersistenceException {
        return playerStatsRepository.load(nickname);
    }

    /**
     * Stops accepting new autosave work and releases the background
     * thread. Should be called when the game window closes; already-queued
     * autosaves are allowed to finish first.
     */
    public void shutdown() {
        autosaveExecutor.shutdown();
    }

    private PlayerStatsSnapshot toStatsSnapshot(GameState state) {
        return new PlayerStatsSnapshot(
                state.getNickname(), state.getHumanShipsSunk(), state.getMachineShipsSunk(), state.getSavedAt());
    }
}
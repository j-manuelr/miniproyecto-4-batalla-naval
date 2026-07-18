package com.example.navalbattle.concurrency;

import com.example.navalbattle.model.exceptions.GamePersistenceException;
import com.example.navalbattle.model.game.GameState;
import com.example.navalbattle.persistence.GameRepository;
import com.example.navalbattle.persistence.PlayerStatsRepository;
import com.example.navalbattle.persistence.PlayerStatsSnapshot;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * {@link Runnable} that writes one autosave: a serialized {@link GameState}
 * plus its plain-text {@link PlayerStatsSnapshot} companion (HU-5). Intended
 * to be handed to a single-thread {@link java.util.concurrent.ExecutorService}
 * by {@code SaveGameManager} right after every shot, so the (potentially
 * slow) disk I/O never blocks the JavaFX Application Thread.
 *
 * <h2>Why a shared lock</h2>
 * <p>The plan for this project's concurrency explicitly calls out the race
 * between the UI thread and the autosave thread over the same
 * {@code GameState}: two shots fired in quick succession — the second one
 * possible only once the first has already returned control to the UI —
 * can each spawn an {@code AutoSaveTask}, and both would then be writing to
 * the very same {@code board_<nickname>.ser} / {@code player_<nickname>.txt}
 * pair concurrently on the single-thread executor's worker thread if a
 * caller ever changed that executor to allow more parallelism. Every
 * {@code AutoSaveTask} therefore synchronizes on a lock object shared by
 * all autosaves for the same {@code SaveGameManager} while it writes, so
 * two autosave writes for the same game can never interleave their bytes on
 * disk, and the (already-serialized, effectively immutable at this point)
 * {@code GameState} snapshot it holds is never read by two threads at
 * once.</p>
 *
 * <p>This class never throws: a failed save is reported to the supplied
 * {@code onFailure} callback instead, since a {@link Runnable} run by an
 * {@link java.util.concurrent.ExecutorService} has no caller left to
 * propagate a checked exception to by the time it runs.</p>
 */
public class AutoSaveTask implements Runnable {

    private final GameState gameStateSnapshot;
    private final PlayerStatsSnapshot statsSnapshot;
    private final GameRepository gameRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final Object writeLock;
    private final Consumer<GamePersistenceException> onFailure;

    /**
     * Creates an autosave unit of work.
     *
     * @param gameStateSnapshot     the game snapshot to write to the
     *                              {@code .ser} file
     * @param statsSnapshot         the stats snapshot to write to the
     *                              {@code .txt} file
     * @param gameRepository        persists {@code gameStateSnapshot}
     * @param playerStatsRepository persists {@code statsSnapshot}
     * @param writeLock             object every {@code AutoSaveTask} for the
     *                              same game synchronizes on while writing;
     *                              callers should share one instance (e.g.
     *                              the owning {@code SaveGameManager})
     * @param onFailure             called, instead of throwing, if either
     *                              write fails
     */
    public AutoSaveTask(GameState gameStateSnapshot,
                        PlayerStatsSnapshot statsSnapshot,
                        GameRepository gameRepository,
                        PlayerStatsRepository playerStatsRepository,
                        Object writeLock,
                        Consumer<GamePersistenceException> onFailure) {
        this.gameStateSnapshot = Objects.requireNonNull(gameStateSnapshot, "gameStateSnapshot");
        this.statsSnapshot = Objects.requireNonNull(statsSnapshot, "statsSnapshot");
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository");
        this.playerStatsRepository = Objects.requireNonNull(playerStatsRepository, "playerStatsRepository");
        this.writeLock = Objects.requireNonNull(writeLock, "writeLock");
        this.onFailure = Objects.requireNonNull(onFailure, "onFailure");
    }

    /**
     * Writes both files, synchronized on {@link #writeLock}. Never throws;
     * see the class-level documentation.
     */
    @Override
    public void run() {
        synchronized (writeLock) {
            try {
                gameRepository.save(gameStateSnapshot);
                playerStatsRepository.save(statsSnapshot);
            } catch (GamePersistenceException e) {
                onFailure.accept(e);
            }
        }
    }
}
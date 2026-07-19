package com.example.navalbattle.concurrency;

import javafx.application.Platform;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Daemon {@link Thread} that ticks once per second for as long as a game is
 * in progress, reporting the elapsed seconds so the UI can show a match
 * timer. It is deliberately a plain {@code Thread} rather than a
 * {@link javafx.concurrent.Task} — unlike {@link MachineTurnTask} or
 * {@link AutoSaveTask}, it does not produce a single result and finish; it
 * runs indefinitely until told to stop, which is exactly what a raw
 * {@code Thread} loop is for.
 *
 * <h2>Why {@code volatile}</h2>
 * <p>{@link #stopClock()} is expected to be called from the JavaFX
 * Application Thread (e.g. when the game ends or the window closes), while
 * {@link #running} is read on this thread's own loop. Without
 * {@code volatile}, the JIT would be free to cache the flag's value in a
 * register on the clock thread and never observe the write made by the
 * Application Thread, so the loop could spin forever even after
 * {@code stopClock()} returns. Marking it {@code volatile} guarantees the
 * write is visible to the clock thread on its very next check.</p>
 *
 * <h2>Why {@code Platform.runLater}</h2>
 * <p>{@link #onTick} is normally a UI update (e.g. setting a
 * {@code Label}'s text). JavaFX nodes may only be touched from the
 * Application Thread, so every delivery is wrapped in
 * {@link Platform#runLater(Runnable)} instead of calling {@code onTick}
 * directly from this background thread.</p>
 */
public class GameClockThread extends Thread {

    private static final long TICK_MILLIS = 1000;

    private final LongConsumer onTick;
    private volatile boolean running = true;

    /**
     * Creates the clock thread. Does not start ticking until {@link #start()}
     * is called, per the usual {@link Thread} contract.
     *
     * @param onTick callback invoked once per second, on the JavaFX
     *               Application Thread, with the total elapsed seconds
     */
    public GameClockThread(LongConsumer onTick) {
        super("game-clock-thread");
        this.onTick = Objects.requireNonNull(onTick, "onTick");
        setDaemon(true);
    }

    /**
     * Runs the tick loop until {@link #stopClock()} is called. Not meant to
     * be invoked directly — call {@link #start()} instead, as with any
     * {@link Thread}.
     */
    @Override
    public void run() {
        long elapsedSeconds = 0;
        while (running) {
            try {
                Thread.sleep(TICK_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!running) {
                return;
            }
            elapsedSeconds++;
            long secondsToReport = elapsedSeconds;
            Platform.runLater(() -> onTick.accept(secondsToReport));
        }
    }

    /**
     * Signals the loop to stop and interrupts it in case it is currently
     * sleeping, so the thread exits promptly instead of waiting up to
     * {@value #TICK_MILLIS} ms. Safe to call from any thread; idempotent.
     */
    public void stopClock() {
        running = false;
        interrupt();
    }
}

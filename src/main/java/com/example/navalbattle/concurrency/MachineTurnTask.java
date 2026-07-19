package com.example.navalbattle.concurrency;

import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.model.game.GameEngine;
import com.example.navalbattle.model.player.MachinePlayer;
import com.example.navalbattle.model.shot.Shot;
import javafx.concurrent.Task;

/**
 * Background {@link Task} that plays a single machine shot (HU-4): asks the
 * machine's {@code ShotStrategy} for a target, waits briefly to simulate
 * "thinking" (so the human can actually follow what happened instead of
 * seeing an instant response), and resolves the shot through
 * {@link GameEngine} — all off the JavaFX Application Thread, since the
 * "thinking" delay would otherwise freeze the whole UI.
 *
 * <h2>Threading contract</h2>
 * <p>{@link #call()} runs on a background thread (whichever thread executes
 * this task — normally a plain {@code new Thread(task).start()} or a small
 * {@code ExecutorService}, per the project's concurrency plan). It touches
 * no JavaFX node directly. The controller that launches this task should
 * react in {@code setOnSucceeded}, which JavaFX guarantees runs back on the
 * Application Thread, and update the board view there — never inside
 * {@link #call()} itself.</p>
 *
 * <h2>One shot per task, on purpose</h2>
 * <p>A machine turn can chain several shots in a row (a hit or a sunk ship
 * lets it shoot again). Rather than looping inside this task, each shot is
 * its own {@code MachineTurnTask}: the controller's {@code setOnSucceeded}
 * handler checks {@link GameEngine#getCurrentTurn()} and
 * {@link GameEngine#isGameOver()} and simply launches another
 * {@code MachineTurnTask} if the machine is still due to play. This keeps
 * this class single-purpose (Single Responsibility Principle) and keeps
 * the "thinking" delay applied before every individual shot, not just the
 * first one of the turn.</p>
 */
public class MachineTurnTask extends Task<Shot> {

    private static final long THINKING_DELAY_MILLIS = 600;

    private final GameEngine engine;

    /**
     * Creates a task that will play exactly one machine shot against
     * {@code engine} when executed.
     *
     * @param engine the live game the shot is resolved against
     */
    public MachineTurnTask(GameEngine engine) {
        this.engine = engine;
    }

    /**
     * Picks a target with the machine's {@code ShotStrategy}, sleeps for
     * {@value #THINKING_DELAY_MILLIS} ms, and resolves the shot.
     *
     * @return the resolved shot
     * @throws InterruptedException if the background thread is interrupted
     *         while "thinking" (e.g. the game window is closed mid-turn)
     */
    @Override
    protected Shot call() throws InterruptedException {
        Thread.sleep(THINKING_DELAY_MILLIS);

        MachinePlayer machine = engine.getMachinePlayer();
        Coordinate target = machine.getShotStrategy().nextShot(engine.getHumanPlayer().getBoard());
        return engine.fireAsMachine(target);
    }
}

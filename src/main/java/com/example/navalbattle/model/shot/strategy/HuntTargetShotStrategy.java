package com.example.navalbattle.model.shot.strategy;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.model.shot.ShotResult;
import com.example.navalbattle.util.AppConfig;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Shoots randomly while "hunting", but as soon as a shot lands a hit it
 * switches to "targeting mode": it queues the up-to-four orthogonal
 * neighbors of the hit cell in a {@link Deque} and tries those first,
 * mimicking how an attentive human player follows up on a confirmed hit
 * before going back to random shots (HU-4: "La máquina responde
 * adecuadamente a los disparos del jugador humano").
 *
 * <p>The {@link Deque} used for {@code pendingTargets} is one of the
 * non-array data structures the assignment asks for, and it is the natural
 * fit here: candidates must be tried in the order they were discovered
 * (first-in, first-out) and, once a ship sinks, the whole queue is cleared
 * in one call.</p>
 */
public class HuntTargetShotStrategy implements ShotStrategy {

    private final Random random;
    private final Deque<Coordinate> pendingTargets = new ArrayDeque<>();

    /**
     * Creates a strategy backed by a new {@link Random} instance.
     */
    public HuntTargetShotStrategy() {
        this(new Random());
    }

    /**
     * Creates a strategy backed by the given {@link Random} instance, so
     * unit tests can force deterministic random fallback shots.
     *
     * @param random the random number generator to use
     */
    public HuntTargetShotStrategy(Random random) {
        this.random = random;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Coordinate nextShot(Board opponentBoard) {
        Coordinate queued = pollValidQueuedTarget(opponentBoard);
        if (queued != null) {
            return queued;
        }
        return randomUnshotCoordinate(opponentBoard);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A {@link ShotResult#HIT} enqueues the hit cell's orthogonal
     * neighbors for the next shots; a {@link ShotResult#SUNK} clears the
     * queue, since that target area is now fully resolved and any
     * remaining queued neighbors would just waste turns on water.</p>
     */
    @Override
    public void registerResult(Coordinate shot, ShotResult result) {
        if (result == ShotResult.HIT) {
            enqueueNeighbors(shot);
        } else if (result == ShotResult.SUNK) {
            pendingTargets.clear();
        }
    }

    private Coordinate pollValidQueuedTarget(Board opponentBoard) {
        while (!pendingTargets.isEmpty()) {
            Coordinate candidate = pendingTargets.poll();
            if (!opponentBoard.hasBeenShot(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private Coordinate randomUnshotCoordinate(Board opponentBoard) {
        Coordinate candidate;
        do {
            candidate = new Coordinate(
                    random.nextInt(AppConfig.BOARD_SIZE),
                    random.nextInt(AppConfig.BOARD_SIZE));
        } while (opponentBoard.hasBeenShot(candidate));
        return candidate;
    }

    private void enqueueNeighbors(Coordinate origin) {
        int[][] offsets = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] offset : offsets) {
            Coordinate neighbor = new Coordinate(origin.row() + offset[0], origin.col() + offset[1]);
            if (neighbor.isWithinBounds(AppConfig.BOARD_SIZE)) {
                pendingTargets.add(neighbor);
            }
        }
    }
}

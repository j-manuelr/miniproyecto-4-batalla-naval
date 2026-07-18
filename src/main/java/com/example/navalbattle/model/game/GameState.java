package com.example.navalbattle.model.game;

import com.example.navalbattle.model.board.Board;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Immutable snapshot of a full game, playing the Memento role in this
 * design: {@link GameEngine} is the originator that creates and restores
 * snapshots, and {@code SaveGameManager} (in the persistence layer) is the
 * caretaker that writes this object to a {@code .ser} file and reads it
 * back, without knowing anything about the game rules it represents.
 *
 * <p>This class is intentionally a plain, self-contained data holder so it
 * can be serialized with {@link java.io.ObjectOutputStream} and later
 * deserialized into an identical copy, satisfying HU-5's requirement to
 * resume a game from the exact state it was saved in.</p>
 */
public final class GameState implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String nickname;
    private final Board humanBoard;
    private final Board machineBoard;
    private final TurnManager.Turn currentTurn;
    private final int humanShipsSunk;
    private final int machineShipsSunk;
    private final LocalDateTime savedAt;

    /**
     * Creates a new snapshot. Prefer {@link GameEngine#createSnapshot()}
     * over calling this constructor directly, so the snapshot always
     * reflects a consistent, in-sync state of the live game.
     *
     * @param nickname          the human player's nickname
     * @param humanBoard        the human player's board (own fleet)
     * @param machineBoard      the machine player's board (own fleet)
     * @param currentTurn       whose turn is active at the time of saving
     * @param humanShipsSunk    ships the human player has sunk so far
     * @param machineShipsSunk  ships the machine player has sunk so far
     * @param savedAt           the timestamp of this snapshot
     */
    public GameState(String nickname,
                      Board humanBoard,
                      Board machineBoard,
                      TurnManager.Turn currentTurn,
                      int humanShipsSunk,
                      int machineShipsSunk,
                      LocalDateTime savedAt) {
        this.nickname = nickname;
        this.humanBoard = humanBoard;
        this.machineBoard = machineBoard;
        this.currentTurn = currentTurn;
        this.humanShipsSunk = humanShipsSunk;
        this.machineShipsSunk = machineShipsSunk;
        this.savedAt = savedAt;
    }

    public String getNickname() {
        return nickname;
    }

    public Board getHumanBoard() {
        return humanBoard;
    }

    public Board getMachineBoard() {
        return machineBoard;
    }

    public TurnManager.Turn getCurrentTurn() {
        return currentTurn;
    }

    public int getHumanShipsSunk() {
        return humanShipsSunk;
    }

    public int getMachineShipsSunk() {
        return machineShipsSunk;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }
}

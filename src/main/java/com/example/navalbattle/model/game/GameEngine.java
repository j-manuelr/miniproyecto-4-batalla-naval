package com.example.navalbattle.model.game;

import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.model.exceptions.InvalidShipPlacementException;
import com.example.navalbattle.model.exceptions.InvalidTurnException;
import com.example.navalbattle.model.game.events.GameOverListener;
import com.example.navalbattle.model.game.events.ShotListener;
import com.example.navalbattle.model.game.events.TurnListener;
import com.example.navalbattle.model.player.MachinePlayer;
import com.example.navalbattle.model.player.Player;
import com.example.navalbattle.model.ship.Orientation;
import com.example.navalbattle.model.ship.Ship;
import com.example.navalbattle.model.shot.Shot;
import com.example.navalbattle.model.shot.ShotResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Facade that orchestrates a full game of Batalla Naval.
 *
 * <p>{@code GameEngine} is the single entry point that controllers use to
 * place ships, fire shots and query game state; it hides the collaboration
 * between {@code Board}, {@link TurnManager} and the listener lists behind
 * a small, intention-revealing API (HU-2, HU-4). It also plays the
 * Memento's <em>originator</em> role: it knows how to capture itself into a
 * {@link GameState} snapshot and how to restore itself from one, which is
 * what the persistence layer needs for the autosave/resume flow of
 * HU-5.</p>
 *
 * <p>{@code GameEngine} is also the Observer <em>subject</em>: it keeps
 * three independent listener lists (one per concern, following the
 * Interface Segregation Principle) and notifies them after every shot,
 * every turn change and when the game ends. The autosave feature and the
 * UI controllers both subscribe to these notifications without knowing
 * about each other, keeping the design loosely coupled.</p>
 */
public class GameEngine {

    private final Player humanPlayer;
    private final MachinePlayer machinePlayer;
    private final TurnManager turnManager;

    private final List<ShotListener> shotListeners = new ArrayList<>();
    private final List<TurnListener> turnListeners = new ArrayList<>();
    private final List<GameOverListener> gameOverListeners = new ArrayList<>();

    private boolean gameOver = false;

    /**
     * Creates a new game engine for a fresh game. Ship placement is done
     * separately through {@link #placeShip(Player, Ship, Coordinate, Orientation)}
     * for the human player and through a {@code FleetPlacementStrategy} for
     * the machine player before the game actually starts.
     *
     * @param humanPlayer   the human player
     * @param machinePlayer the machine (computer) player
     */
    public GameEngine(Player humanPlayer, MachinePlayer machinePlayer) {
        this.humanPlayer = humanPlayer;
        this.machinePlayer = machinePlayer;
        this.turnManager = new TurnManager();
    }

    // ------------------------------------------------------------------
    // Listener registration (Observer)
    // ------------------------------------------------------------------

    public void addShotListener(ShotListener listener) {
        shotListeners.add(listener);
    }

    public void addTurnListener(TurnListener listener) {
        turnListeners.add(listener);
    }

    public void addGameOverListener(GameOverListener listener) {
        gameOverListeners.add(listener);
    }

    // ------------------------------------------------------------------
    // Ship placement (HU-1)
    // ------------------------------------------------------------------

    /**
     * Places a single ship on the given player's board. Intended for the
     * human player, who places ships one at a time through mouse events;
     * the machine player is normally placed in bulk through a
     * {@code FleetPlacementStrategy} instead.
     *
     * @param player      the player whose board receives the ship
     * @param ship        the ship to place
     * @param origin      the coordinate of the ship's first cell
     * @param orientation the ship's orientation
     * @throws InvalidShipPlacementException if the placement overlaps
     *         another ship or falls outside the board
     */
    public void placeShip(Player player, Ship ship, Coordinate origin, Orientation orientation)
            throws InvalidShipPlacementException {
        player.getBoard().placeShip(ship, origin, orientation);
    }

    /**
     * Locks a player's board so no further ships can be placed or moved
     * (HU-1: "Una vez colocados, los barcos no pueden ser movidos ni
     * modificados"). Called once the player confirms their fleet, or right
     * after a {@code FleetPlacementStrategy} finishes placing the machine's
     * fleet.
     *
     * @param player the player whose board should be locked
     */
    public void confirmFleetPlacement(Player player) {
        player.getBoard().lockPlacement();
    }

    // ------------------------------------------------------------------
    // Shooting (HU-2, HU-4)
    // ------------------------------------------------------------------

    /**
     * Resolves a shot fired by the human player against the machine's
     * board.
     *
     * @param target the targeted coordinate on the machine's board
     * @return the resolved shot
     * @throws InvalidTurnException if it is not currently the human
     *         player's turn
     */
    public Shot fireAsHuman(Coordinate target) {
        requireTurn(TurnManager.Turn.HUMAN);
        return resolveShot(humanPlayer, machinePlayer, target);
    }

    /**
     * Resolves a shot fired by the machine player against the human's
     * board. The coordinate is chosen beforehand by the machine's
     * {@code ShotStrategy} (typically inside {@code MachineTurnTask}, on a
     * background thread) and passed in here already decided.
     *
     * @param target the targeted coordinate on the human's board
     * @return the resolved shot
     * @throws InvalidTurnException if it is not currently the machine
     *         player's turn
     */
    public Shot fireAsMachine(Coordinate target) {
        requireTurn(TurnManager.Turn.MACHINE);
        Shot shot = resolveShot(machinePlayer, humanPlayer, target);
        machinePlayer.getShotStrategy().registerResult(shot.coordinate(), shot.result());
        return shot;
    }

    private void requireTurn(TurnManager.Turn expected) {
        if (gameOver) {
            throw new InvalidTurnException("The game has already ended");
        }
        if (turnManager.getCurrentTurn() != expected) {
            throw new InvalidTurnException("It is not " + expected + "'s turn");
        }
    }

    private Shot resolveShot(Player shooter, Player defender, Coordinate target) {
        ShotResult result = defender.getBoard().receiveShot(target);
        Shot shot = new Shot(target, result);

        notifyShotFired(shooter, shot);

        TurnManager.Turn turnBeforeAdvance = turnManager.getCurrentTurn();
        turnManager.advance(result);
        if (turnManager.getCurrentTurn() != turnBeforeAdvance) {
            notifyTurnChanged(turnManager.getCurrentTurn());
        }

        if (defender.getBoard().isFleetSunk()) {
            gameOver = true;
            notifyGameOver(shooter);
        }

        if (result == ShotResult.SUNK) {
            shooter.getStats().incrementSunkShips();
        }

        return shot;
    }

    // ------------------------------------------------------------------
    // Observer notification helpers
    // ------------------------------------------------------------------

    private void notifyShotFired(Player shooter, Shot shot) {
        for (ShotListener listener : shotListeners) {
            listener.onShotFired(shooter, shot);
        }
    }

    private void notifyTurnChanged(TurnManager.Turn newTurn) {
        for (TurnListener listener : turnListeners) {
            listener.onTurnChanged(newTurn);
        }
    }

    private void notifyGameOver(Player winner) {
        for (GameOverListener listener : gameOverListeners) {
            listener.onGameOver(winner);
        }
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public Player getHumanPlayer() {
        return humanPlayer;
    }

    public MachinePlayer getMachinePlayer() {
        return machinePlayer;
    }

    public TurnManager.Turn getCurrentTurn() {
        return turnManager.getCurrentTurn();
    }

    public boolean isGameOver() {
        return gameOver;
    }

    // ------------------------------------------------------------------
    // Memento: snapshot / restore for HU-5
    // ------------------------------------------------------------------

    /**
     * Captures the current game into an immutable, serializable
     * {@link GameState} snapshot, ready to be handed to the persistence
     * layer for the HU-5 autosave flow.
     *
     * @return a snapshot of the current game
     */
    public GameState createSnapshot() {
        return new GameState(
                humanPlayer.getNickname(),
                humanPlayer.getBoard(),
                machinePlayer.getBoard(),
                turnManager.getCurrentTurn(),
                humanPlayer.getStats().getSunkShipsCount(),
                machinePlayer.getStats().getSunkShipsCount(),
                LocalDateTime.now());
    }

    /**
     * Restores the active turn from a previously loaded {@link GameState}.
     * Board contents are restored separately by the persistence layer,
     * which deserializes the boards directly into the {@code Player}
     * instances before this engine is constructed; this method only
     * synchronizes the turn so play resumes exactly where it left off.
     *
     * @param state the snapshot to restore the turn from
     */
    public void restoreTurn(GameState state) {
        turnManager.restore(state.getCurrentTurn());
        this.gameOver = false;
    }
}

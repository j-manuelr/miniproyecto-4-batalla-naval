package com.example.navalbattle.persistence;

import com.example.navalbattle.model.exceptions.GamePersistenceException;
import com.example.navalbattle.model.game.GameState;

import java.util.Optional;

/**
 * Persists and restores {@link GameState} snapshots (HU-5).
 *
 * <p>Following the Dependency Inversion Principle, {@code SaveGameManager}
 * and every controller depend on this interface, never on a concrete
 * storage mechanism — {@link SerializedGameRepository} is the only
 * implementation shipped with the project, but a different one (a database,
 * a cloud save, an in-memory fake for tests) could be swapped in without
 * touching any caller.</p>
 */
public interface GameRepository {

    /**
     * Persists a full snapshot of the game so it can be resumed later.
     *
     * @param state the snapshot to persist; never {@code null}
     * @throws GamePersistenceException if the snapshot cannot be written
     */
    void save(GameState state) throws GamePersistenceException;

    /**
     * Loads the snapshot previously saved for the given nickname, if any.
     *
     * @param nickname the human player's nickname used at save time
     * @return the loaded snapshot, or {@link Optional#empty()} if no save
     *         exists for that nickname
     * @throws GamePersistenceException if a save exists but cannot be read
     */
    Optional<GameState> load(String nickname) throws GamePersistenceException;

    /**
     * Loads the most recently saved snapshot, regardless of nickname. Used
     * by the "Continuar" option in the main menu (HU-5), which resumes
     * whichever game was played last.
     *
     * @return the most recently saved snapshot, or {@link Optional#empty()}
     *         if no save exists at all
     * @throws GamePersistenceException if the most recent save exists but
     *         cannot be read
     */
    Optional<GameState> loadLatest() throws GamePersistenceException;
}
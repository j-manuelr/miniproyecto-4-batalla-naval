package com.example.navalbattle.persistence;

import com.example.navalbattle.model.exceptions.GamePersistenceException;

import java.util.Optional;

/**
 * Persists and restores the plain-text player stats file (HU-5, rubric
 * #12 "archivos planos"): nickname, ships sunk by each side, and the
 * timestamp of the last save.
 *
 * <p>Kept as its own interface — separate from {@link GameRepository} —
 * following the Interface Segregation Principle: a class that only cares
 * about the human-readable stats summary (e.g. a "hall of fame" screen)
 * should not have to depend on the (much heavier) serialized-board
 * repository, and vice versa.</p>
 */
public interface PlayerStatsRepository {

    /**
     * Writes the given stats snapshot to the plain-text file for its
     * nickname, overwriting any previous contents.
     *
     * @param snapshot the stats to persist
     * @throws GamePersistenceException if the file cannot be written
     */
    void save(PlayerStatsSnapshot snapshot) throws GamePersistenceException;

    /**
     * Reads back the stats previously saved for the given nickname.
     *
     * @param nickname the player's nickname
     * @return the loaded stats, or {@link Optional#empty()} if no stats
     *         file exists for that nickname
     * @throws GamePersistenceException if a file exists but cannot be
     *         parsed
     */
    Optional<PlayerStatsSnapshot> load(String nickname) throws GamePersistenceException;
}
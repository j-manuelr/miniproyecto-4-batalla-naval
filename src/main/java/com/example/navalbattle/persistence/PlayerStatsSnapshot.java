package com.example.navalbattle.persistence;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Immutable, plain-data snapshot of everything HU-5 requires to be written
 * to the plain-text stats file: the human player's nickname, how many ships
 * each side has sunk so far, and when the snapshot was taken.
 *
 * <p>This is deliberately its own small type instead of reusing
 * {@code GameState} or {@code PlayerStats} directly: {@link
 * com.example.navalbattle.model.game.GameState} also carries both full
 * {@code Board} objects, which have nothing to do with a plain-text file,
 * and a single {@code PlayerStats} only tracks one side's count. Keeping
 * {@link PlayerStatsRepository} bound to this tiny record instead — rather
 * than to {@code GameState} — is what lets it stay independent of the
 * board/game model (Dependency Inversion / Interface Segregation): the
 * repository only ever needs these four values, never a live board.</p>
 *
 * @param nickname          the human player's nickname
 * @param humanShipsSunk    ships the human player has sunk so far
 * @param machineShipsSunk  ships the machine player has sunk so far
 * @param savedAt           the timestamp this snapshot was taken
 */
public record PlayerStatsSnapshot(String nickname,
                                  int humanShipsSunk,
                                  int machineShipsSunk,
                                  LocalDateTime savedAt) implements Serializable {
}
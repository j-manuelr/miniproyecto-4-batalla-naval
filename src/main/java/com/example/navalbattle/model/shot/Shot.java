package com.example.navalbattle.model.shot;

import com.example.navalbattle.model.board.Coordinate;

import java.io.Serializable;

/**
 * An immutable record of a single resolved shot: where it landed and what
 * it resulted in. Used both to notify {@code ShotListener}s in real time
 * and, as part of a ship's shot history, to feed a
 * {@code HuntTargetShotStrategy}.
 *
 * @param coordinate the targeted cell
 * @param result     the outcome of firing at that cell
 */
public record Shot(Coordinate coordinate, ShotResult result) implements Serializable {
}

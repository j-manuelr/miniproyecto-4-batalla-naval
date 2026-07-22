package com.example.navalbattle.util;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.board.CellState;
import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.model.ship.Ship;

public class BoardValidator {

    public static String validatePlacement(Board board, Ship ship, int startX, int startY, boolean isHorizontal) {

        int length = ship.getSize();

        if (isHorizontal) {
            if (startX < 0 || startX + length > AppConfig.BOARD_SIZE || startY < 0 || startY >= AppConfig.BOARD_SIZE) {
                return "El barco sale de los límites del tablero.";
            }
        } else {
            if (startX < 0 || startX >= AppConfig.BOARD_SIZE || startY < 0 || startY + length > AppConfig.BOARD_SIZE) {
                return "El barco sale de los límites del tablero.";
            }
        }

        for (int i = 0; i < length; i++) {
            int checkX = isHorizontal ? startX + i : startX;
            int checkY = isHorizontal ? startY : startY + i;

            Coordinate checkCoord = new Coordinate(checkY, checkX);

            if (board.getCell(checkCoord).getState() == CellState.SHIP) {
                return "Esa zona ya está ocupada por otro barco.";
            }
        }

        return null;
    }
}

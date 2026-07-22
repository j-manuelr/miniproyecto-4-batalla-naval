package com.example.navalbattle.controller;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.view.BoardRenderMode;
import com.example.navalbattle.view.BoardView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Objects;

/**
 * Controller for HU-3 ("ver tablero del oponente"): a read-only window,
 * separate from the normal game flow, that reveals the machine's entire
 * board (including undiscovered ships).
 * <p>
 * It never attaches shot listeners and it is opened from its own
 * verification menu/button — never from inside the normal game board
 * pane — which is exactly what the architecture plan asks for HU-3.
 */
public class OpponentBoardViewController {

    @FXML
    private Pane opponentBoardPane;

    @FXML
    private Button closeButton;

    private BoardView opponentBoardView;

    @FXML
    private void initialize() {
        closeButton.setOnAction(event -> closeWindow());
    }

    /**
     * Injects the board to reveal and paints it in
     * {@link BoardRenderMode#VERIFICATION_VIEW}. Call this right after
     * loading the FXML (see {@link #open(Board, Window)}), before showing
     * the stage.
     */
    public void setBoard(Board machineBoard) {
        Objects.requireNonNull(machineBoard, "machineBoard");
        opponentBoardView = new BoardView(machineBoard, BoardRenderMode.VERIFICATION_VIEW);
        opponentBoardPane.getChildren().setAll(opponentBoardView.getView());
    }

    /**
     * Re-reads the model and redraws. Useful if the window is left open
     * while the machine keeps taking shots on its own board in a
     * different context (not the usual case, but harmless to expose).
     */
    public void refresh() {
        if (opponentBoardView != null) {
            opponentBoardView.refresh();
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Convenience factory: loads {@code opponent-board-view.fxml}, wires
     * the board, and shows it as its own {@link Stage} on top of
     * {@code owner}. Call this from {@code GameBoardController}, e.g. on
     * a "Ver flota enemiga" button:
     * <pre>{@code
     * OpponentBoardViewController.open(machinePlayer.getBoard(), mainStage);
     * }</pre>
     *
     * @param machineBoard the machine player's board (source of truth,
     *                     fully revealed)
     * @param owner        the window this verification window belongs to
     * @return the controller, in case the caller wants to keep a
     *         reference (e.g. to call {@link #refresh()} later)
     * @throws IOException if the FXML fails to load
     */
    public static OpponentBoardViewController open(Board machineBoard, Window owner) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                OpponentBoardViewController.class.getResource(
                        "/com/example/navalbattle/opponent-board-view.fxml"));
        Parent root = loader.load();

        OpponentBoardViewController controller = loader.getController();
        controller.setBoard(machineBoard);

        Stage stage = new Stage();
        stage.setTitle("Verificacion - Flota de la maquina");
        stage.initOwner(owner);
        stage.setScene(new Scene(root));
        stage.show();

        return controller;
    }
}

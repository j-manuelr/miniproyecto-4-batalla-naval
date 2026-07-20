package com.example.navalbattle.persistence;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.game.GameEngine;
import com.example.navalbattle.model.player.HumanPlayer;
import com.example.navalbattle.model.player.MachinePlayer;
import com.example.navalbattle.model.shot.strategy.RandomShotStrategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SafeFileNames}, and an end-to-end check that a
 * hostile or accidental nickname (a path-separator sequence like
 * {@code "../../escape"}) can never make {@link SerializedGameRepository}
 * write outside its configured saves directory.
 */
class SafeFileNamesTest {

    @Test
    void leavesAnOrdinaryAccentedNicknameUntouched() {
        assertEquals("José María", SafeFileNames.sanitize("José María"));
    }

    @Test
    void replacesPathSeparatorsSoTheResultCannotEscapeADirectory() {
        String sanitized = SafeFileNames.sanitize("../../escape");

        assertFalse(sanitized.contains("/"), "sanitized name must not contain '/'");
        assertFalse(sanitized.contains("\\"), "sanitized name must not contain '\\'");
    }

    @Test
    void blankOrNullNicknamesFallBackToAPlaceholder() {
        assertEquals("player", SafeFileNames.sanitize(""));
        assertEquals("player", SafeFileNames.sanitize("   "));
        assertEquals("player", SafeFileNames.sanitize(null));
    }

    @Test
    void aPathTraversalNicknameCannotEscapeTheSavesDirectory(@TempDir Path tempSavesDirectory) throws Exception {
        Board humanBoard = new Board();
        Board machineBoard = new Board();
        HumanPlayer human = new HumanPlayer("../../escape", humanBoard);
        MachinePlayer machine = new MachinePlayer(machineBoard, new RandomShotStrategy(new Random(1)));
        GameEngine engine = new GameEngine(human, machine);

        GameRepository gameRepository = new SerializedGameRepository(tempSavesDirectory);
        SaveGameManager saveManager = new SaveGameManager(
                engine, gameRepository, new PlainTextPlayerStatsRepository(tempSavesDirectory),
                java.util.concurrent.Executors.newSingleThreadExecutor());

        saveManager.saveNow();

        // Every file actually written must be a direct child of the
        // configured saves directory -- never a path that climbed out of it.
        try (var files = java.nio.file.Files.list(tempSavesDirectory)) {
            files.forEach(file -> assertTrue(
                    file.getParent().equals(tempSavesDirectory),
                    "file escaped the saves directory: " + file));
        }
    }
}

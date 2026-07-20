package com.example.navalbattle.persistence;

import com.example.navalbattle.model.board.Board;
import com.example.navalbattle.model.board.Coordinate;
import com.example.navalbattle.model.game.GameEngine;
import com.example.navalbattle.model.game.GameState;
import com.example.navalbattle.model.placement.RandomFleetPlacementStrategy;
import com.example.navalbattle.model.player.HumanPlayer;
import com.example.navalbattle.model.player.MachinePlayer;
import com.example.navalbattle.model.shot.strategy.RandomShotStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration-style test for the HU-5 autosave round trip: fires a real
 * shot, waits for the {@code AutoSaveTask} it triggers to actually finish
 * writing, then reads both the {@code .ser} and the {@code .txt} files
 * back from disk through the same repository interfaces the rest of the
 * app uses, and checks their contents match what was true in memory at the
 * moment the shot was resolved.
 *
 * <p>Both repositories accept an explicit save directory precisely so
 * tests like this one never have to touch the real {@code saves/} folder:
 * JUnit's {@link TempDir} hands out a fresh, automatically-deleted
 * directory per test, so there is nothing to clean up afterward and no
 * risk of colliding with (or polluting) an actual save.</p>
 */
class SaveGameManagerTest {

    private static final String TEST_NICKNAME = "Ada";

    @TempDir
    Path tempSavesDirectory;

    private GameRepository gameRepository;
    private PlayerStatsRepository statsRepository;

    @BeforeEach
    void setUp() {
        gameRepository = new SerializedGameRepository(tempSavesDirectory);
        statsRepository = new PlainTextPlayerStatsRepository(tempSavesDirectory);
    }

    @Test
    void aShotTriggersAnAutosaveThatCanBeReadBackFromDisk() throws Exception {
        Board humanBoard = new Board();
        Board machineBoard = new Board();
        new RandomFleetPlacementStrategy(new Random(1)).placeFleet(humanBoard);
        new RandomFleetPlacementStrategy(new Random(2)).placeFleet(machineBoard);

        HumanPlayer human = new HumanPlayer(TEST_NICKNAME, humanBoard);
        MachinePlayer machine = new MachinePlayer(machineBoard, new RandomShotStrategy(new Random(3)));
        GameEngine engine = new GameEngine(human, machine);
        engine.confirmFleetPlacement(human);
        engine.confirmFleetPlacement(machine);

        ExecutorService autosaveExecutor = Executors.newSingleThreadExecutor();
        SaveGameManager saveManager = new SaveGameManager(engine, gameRepository, statsRepository, autosaveExecutor);
        saveManager.startAutoSaving();

        engine.fireAsHuman(new Coordinate(0, 0));

        // The autosave triggered by the shot above runs on autosaveExecutor's
        // background thread. Submitting one more autosave and waiting on its
        // Future guarantees the first one already finished writing by the
        // time this call returns, since a single-thread executor always runs
        // submitted tasks in order.
        saveManager.requestAutosave().get(5, TimeUnit.SECONDS);

        GameState loaded = gameRepository.load(TEST_NICKNAME)
                .orElseGet(() -> fail("expected an autosave .ser file for " + TEST_NICKNAME));
        assertEquals(TEST_NICKNAME, loaded.getNickname());
        assertEquals(engine.getCurrentTurn(), loaded.getCurrentTurn());

        PlayerStatsSnapshot stats = statsRepository.load(TEST_NICKNAME)
                .orElseGet(() -> fail("expected an autosave .txt file for " + TEST_NICKNAME));
        assertEquals(TEST_NICKNAME, stats.nickname());

        autosaveExecutor.shutdown();
    }

    @Test
    void saveNowWritesSynchronouslyOnTheCallingThread() throws Exception {
        Board humanBoard = new Board();
        Board machineBoard = new Board();
        HumanPlayer human = new HumanPlayer(TEST_NICKNAME, humanBoard);
        MachinePlayer machine = new MachinePlayer(machineBoard, new RandomShotStrategy(new Random(4)));
        GameEngine engine = new GameEngine(human, machine);

        ExecutorService autosaveExecutor = Executors.newSingleThreadExecutor();
        SaveGameManager saveManager = new SaveGameManager(engine, gameRepository, statsRepository, autosaveExecutor);

        saveManager.saveNow();

        assertTrue(gameRepository.load(TEST_NICKNAME).isPresent(),
                "saveNow() must have written the file before returning, with no background wait needed");

        autosaveExecutor.shutdown();
    }
}

package com.example.navalbattle.persistence;

import com.example.navalbattle.model.exceptions.GamePersistenceException;
import com.example.navalbattle.model.game.GameState;
import com.example.navalbattle.util.AppConfig;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * {@link GameRepository} implementation that stores each {@link GameState}
 * as a single {@code .ser} file, written and read with
 * {@link ObjectOutputStream}/{@link ObjectInputStream} (HU-5, rubric #12
 * "archivos ... serializables").
 *
 * <p>Files live under {@code saves/board_<nickname>.ser}, one per nickname
 * — a fresh save for the same nickname simply overwrites the previous file,
 * since only the latest state for a given player needs to be resumable.
 * Every low-level {@link IOException} or {@link ClassNotFoundException} is
 * caught here and rethrown as the project's own checked
 * {@link GamePersistenceException}, so callers never have to deal with raw
 * I/O failures.</p>
 */
public class SerializedGameRepository implements GameRepository {

    private static final String FILE_PREFIX = "board_";
    private static final String FILE_SUFFIX = ".ser";

    private final Path savesDirectory;

    /**
     * Creates a repository backed by the default {@value AppConfig#SAVES_DIRECTORY}
     * directory (relative to the working directory).
     */
    public SerializedGameRepository() {
        this(Paths.get(AppConfig.SAVES_DIRECTORY));
    }

    /**
     * Creates a repository backed by an explicit directory. Exposed mainly
     * so unit tests can point saves at a temporary directory instead of the
     * real {@code saves/} folder.
     *
     * @param savesDirectory the directory where {@code .ser} files are read
     *                       from and written to
     */
    public SerializedGameRepository(Path savesDirectory) {
        this.savesDirectory = savesDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GameState state) throws GamePersistenceException {
        Path target = fileFor(state.getNickname());
        try {
            Files.createDirectories(savesDirectory);
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(target))) {
                out.writeObject(state);
            }
        } catch (IOException e) {
            throw new GamePersistenceException(
                    "Could not save the game for player '" + state.getNickname() + "'", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<GameState> load(String nickname) throws GamePersistenceException {
        Path source = fileFor(nickname);
        if (!Files.exists(source)) {
            return Optional.empty();
        }
        return Optional.of(deserialize(source));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<GameState> loadLatest() throws GamePersistenceException {
        Optional<Path> latest = findMostRecentSaveFile();
        if (latest.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(deserialize(latest.get()));
    }

    private Optional<Path> findMostRecentSaveFile() throws GamePersistenceException {
        if (!Files.isDirectory(savesDirectory)) {
            return Optional.empty();
        }
        try (DirectoryStream<Path> files =
                     Files.newDirectoryStream(savesDirectory, FILE_PREFIX + "*" + FILE_SUFFIX)) {
            Path mostRecent = null;
            long mostRecentMillis = Long.MIN_VALUE;
            for (Path candidate : files) {
                long modifiedMillis = Files.getLastModifiedTime(candidate).toMillis();
                if (modifiedMillis > mostRecentMillis) {
                    mostRecentMillis = modifiedMillis;
                    mostRecent = candidate;
                }
            }
            return Optional.ofNullable(mostRecent);
        } catch (IOException e) {
            throw new GamePersistenceException("Could not scan the saves directory for the latest game", e);
        }
    }

    private GameState deserialize(Path source) throws GamePersistenceException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(source))) {
            Object loaded = in.readObject();
            if (!(loaded instanceof GameState state)) {
                throw new GamePersistenceException(
                        "File " + source.getFileName() + " does not contain a valid saved game");
            }
            return state;
        } catch (IOException | ClassNotFoundException e) {
            throw new GamePersistenceException("Could not load the saved game from " + source.getFileName(), e);
        }
    }

    private Path fileFor(String nickname) {
        return savesDirectory.resolve(FILE_PREFIX + nickname + FILE_SUFFIX);
    }
}
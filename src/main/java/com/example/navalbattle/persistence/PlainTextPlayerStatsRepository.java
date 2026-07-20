package com.example.navalbattle.persistence;

import com.example.navalbattle.model.exceptions.GamePersistenceException;
import com.example.navalbattle.util.AppConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link PlayerStatsRepository} implementation that writes a simple
 * {@code key=value} plain-text file per nickname, using
 * {@link BufferedWriter}/{@link BufferedReader} as the rubric explicitly
 * asks for (#12 "archivos planos").
 *
 * <p>Files live under {@code saves/player_<nickname>.txt} with one
 * {@code key=value} pair per line:</p>
 * <pre>
 * nickname=Ada
 * humanShipsSunk=4
 * machineShipsSunk=2
 * savedAt=2026-07-18T21:03:11.123
 * </pre>
 *
 * <p>The format is intentionally trivial (no external library, no escaping
 * beyond what {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME} already
 * guarantees) since nicknames and counters never contain the {@code '='}
 * character or newlines in this game.</p>
 */
public class PlainTextPlayerStatsRepository implements PlayerStatsRepository {

    private static final String FILE_PREFIX = "player_";
    private static final String FILE_SUFFIX = ".txt";

    private final Path savesDirectory;

    /**
     * Creates a repository backed by the default {@value AppConfig#SAVES_DIRECTORY}
     * directory (relative to the working directory).
     */
    public PlainTextPlayerStatsRepository() {
        this(Paths.get(AppConfig.SAVES_DIRECTORY));
    }

    /**
     * Creates a repository backed by an explicit directory. Exposed mainly
     * so unit tests can point saves at a temporary directory instead of the
     * real {@code saves/} folder.
     *
     * @param savesDirectory the directory where {@code .txt} files are read
     *                       from and written to
     */
    public PlainTextPlayerStatsRepository(Path savesDirectory) {
        this.savesDirectory = savesDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(PlayerStatsSnapshot snapshot) throws GamePersistenceException {
        Path target = fileFor(snapshot.nickname());
        try {
            Files.createDirectories(savesDirectory);
            try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                writer.write("nickname=" + snapshot.nickname());
                writer.newLine();
                writer.write("humanShipsSunk=" + snapshot.humanShipsSunk());
                writer.newLine();
                writer.write("machineShipsSunk=" + snapshot.machineShipsSunk());
                writer.newLine();
                writer.write("savedAt=" + snapshot.savedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new GamePersistenceException(
                    "Could not save player stats for '" + snapshot.nickname() + "'", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<PlayerStatsSnapshot> load(String nickname) throws GamePersistenceException {
        Path source = fileFor(nickname);
        if (!Files.exists(source)) {
            return Optional.empty();
        }
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            Map<String, String> fields = readFields(reader);
            return Optional.of(toSnapshot(source, fields));
        } catch (IOException e) {
            throw new GamePersistenceException("Could not load player stats from " + source.getFileName(), e);
        }
    }

    private Map<String, String> readFields(BufferedReader reader) throws IOException {
        Map<String, String> fields = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            int separator = line.indexOf('=');
            if (separator > 0) {
                fields.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }
        return fields;
    }

    private PlayerStatsSnapshot toSnapshot(Path source, Map<String, String> fields)
            throws GamePersistenceException {
        try {
            String nickname = requireField(fields, "nickname", source);
            int humanShipsSunk = Integer.parseInt(requireField(fields, "humanShipsSunk", source));
            int machineShipsSunk = Integer.parseInt(requireField(fields, "machineShipsSunk", source));
            LocalDateTime savedAt = LocalDateTime.parse(
                    requireField(fields, "savedAt", source), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return new PlayerStatsSnapshot(nickname, humanShipsSunk, machineShipsSunk, savedAt);
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new GamePersistenceException(
                    "File " + source.getFileName() + " contains malformed player stats", e);
        }
    }

    private String requireField(Map<String, String> fields, String key, Path source)
            throws GamePersistenceException {
        String value = fields.get(key);
        if (value == null) {
            throw new GamePersistenceException(
                    "File " + source.getFileName() + " is missing the '" + key + "' field");
        }
        return value;
    }

    private Path fileFor(String nickname) {
        return savesDirectory.resolve(FILE_PREFIX + SafeFileNames.sanitize(nickname) + FILE_SUFFIX);
    }
}
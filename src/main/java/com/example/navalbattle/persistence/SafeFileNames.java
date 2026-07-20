package com.example.navalbattle.persistence;

/**
 * Turns a player-supplied nickname into a string safe to use as part of a
 * file name.
 *
 * <p>A nickname is free text typed by the player; nothing before HU-5's
 * save files guaranteed it couldn't contain a path separator or another
 * character a filesystem rejects. Left unsanitized, {@code resolve(...)}
 * could either throw for an otherwise-valid nickname, or — with something
 * like {@code "../../etc"} — write outside the {@code saves/} directory
 * entirely. Only the characters that are actually illegal or dangerous in
 * a file name are replaced, so ordinary nicknames (accents, spaces
 * included — e.g. {@code "José María"}) are left untouched.</p>
 */
final class SafeFileNames {

    private static final String FORBIDDEN_CHARACTERS = "[\\\\/:*?\"<>|\\x00-\\x1F]";

    private SafeFileNames() {
        // Utility class; not meant to be instantiated.
    }

    /**
     * @param nickname the raw, player-supplied nickname
     * @return {@code nickname} with every path separator and every
     *         filesystem-illegal character replaced by {@code '_'}; never
     *         blank, even if {@code nickname} was
     */
    static String sanitize(String nickname) {
        String trimmed = nickname == null ? "" : nickname.trim();
        String sanitized = trimmed.replaceAll(FORBIDDEN_CHARACTERS, "_");
        return sanitized.isEmpty() ? "player" : sanitized;
    }
}

package bg.sofia.uni.fmi.mjt.spotify.server.executor;

import bg.sofia.uni.fmi.mjt.spotify.logger.SpotifyLogger;
import bg.sofia.uni.fmi.mjt.spotify.server.DefaultSpotifyService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class DefaultCommandExecutorTest {

    private static final String LOGGER_FILE_PATH = "commandParserLogs.log";
    private static SpotifyLogger logger;
    private CommandParser parser;
    @Mock
    private SelectionKey key = mock(SelectionKey.class);

    @BeforeAll
    static void setupLogger() throws IOException {
        logger = new SpotifyLogger(LOGGER_FILE_PATH);
    }

    @AfterAll
    static void clear() {
        Path.of(LOGGER_FILE_PATH).toFile().delete();
        Path.of(LOGGER_FILE_PATH + ".lck").toFile().delete();
        logger.close();
    }

    @Test
    void testParseFailure() {
        this.parser = new DefaultCommandExecutor(logger);
        String message = "Invalid command should not throw an exception: ";
        assertDoesNotThrow(() -> this.parser.parse(key, "register invalidEmail password"), message + "register");
        assertDoesNotThrow(() -> this.parser.parse(key, "login invalidEmail password"), message + "login");
        assertDoesNotThrow(() -> this.parser.parse(key, "play InvalidSong"), message + "play");
        assertDoesNotThrow(() -> this.parser.parse(key, "stop"), message + "stop");
        assertDoesNotThrow(() -> this.parser.parse(key, "disconnect"), message + "disconnect");
        assertDoesNotThrow(() -> this.parser.parse(key, "show-playlist IDoNotExist"), message + "show-playlist");
        assertDoesNotThrow(() -> this.parser.parse(key, "add-song-to Playlist Song"), message + "add-song-to");
        assertDoesNotThrow(() -> this.parser.parse(key, "create-playlist Playlist"), message + "create-playlist");
        assertDoesNotThrow(() -> this.parser.parse(key, "top 10"), message + "top");
        assertDoesNotThrow(() -> this.parser.parse(key, "search keyword1 keyword2"), message + "search");
        assertDoesNotThrow(() -> this.parser.parse(key, "non-existing command"), "Non-existing command should not throw an exception");
        assertDoesNotThrow(() -> this.parser.parse(key, "play"), "A command with invalid number of parameters should not throw an exception");
        assertTrue(this.parser.parse(key, "play").startsWith("Error! Invalid number of parameters"),
                "Invalid number of parameters should have it's own response and not be treated like an unknown exception");
        File logsFile = Path.of(LOGGER_FILE_PATH).toFile();
        assertTrue(logsFile.exists(), "Generated errors should be logged");
        assertTrue(logsFile.length() > 0, "When errors are logged, the log file must not be empty");
    }

    @Test
    void testParseMessages() {
        this.parser = new DefaultCommandExecutor(logger, mock(DefaultSpotifyService.class));
        assertEquals("No longer playing music", this.parser.parse(key, "stop"));
        assertEquals("Now playing Song", this.parser.parse(key, "play Song"));
        assertEquals("Playlist created successfully", this.parser.parse(key, "create-playlist Playlist"));
        assertEquals("User successfully logged in", this.parser.parse(key, "login admin admin"));
        assertEquals("User email@abv.bg registered", this.parser.parse(key, "register email@abv.bg password"));
    }
}

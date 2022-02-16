package bg.sofia.uni.fmi.mjt.spotify.logger;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpotifyLoggerTest {

    private static final String TEST_LOGS_PATH = "testLogs.log";

    @Test
    void test() throws IOException {
        SpotifyLogger logger = new SpotifyLogger(TEST_LOGS_PATH);
        logger.log("test client", new IllegalArgumentException("Some exception"));
        File testLogFile = Path.of(TEST_LOGS_PATH).toFile();
        assertTrue(testLogFile.exists(),
                "After a log, the logger should create the file, if it doesn't exist already");
        assertTrue(testLogFile.length() > 0,
                "After a log, the logger should submit the exception and the file shouldn't be empty");
        File testLogFileLock = Path.of(TEST_LOGS_PATH, ".lck").toFile();
        logger.close();
        assertFalse(testLogFileLock.exists(), "Closing the logger should remove the handler files");
        testLogFile.delete();
    }
}

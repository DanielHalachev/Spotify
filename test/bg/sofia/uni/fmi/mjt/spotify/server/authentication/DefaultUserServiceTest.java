package bg.sofia.uni.fmi.mjt.spotify.server.authentication;

import bg.sofia.uni.fmi.mjt.spotify.exceptions.IncorrectLoginCredentialsException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.InvalidRegisterCredentialsException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.SpotifyException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.UserAlreadyLoggedInException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.UserAlreadyRegisteredException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.UserDoesNotExistException;
import bg.sofia.uni.fmi.mjt.spotify.logger.SpotifyLogger;
import bg.sofia.uni.fmi.mjt.spotify.server.DefaultSpotifyService;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifyService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

// testing the UserService through the SpotifyService class, because it's basically a Wrapper
public class DefaultUserServiceTest {

    private static final Path EMAIL_REGEX_PATH = Path.of("resources", "email-regex.txt");
    private static final Path TEST_CREDENTIALS_PATH = Path.of("test-resources", "users", "test-users.csv");
    private static final String LOGGER_FILE_PATH = "userServiceLogs.log";
    private static SpotifyLogger logger;
    private SpotifyService service;

    @Mock
    SelectionKey key1 = mock(SelectionKey.class);
    @Mock
    SelectionKey key2 = mock(SelectionKey.class);

    @BeforeAll
    static void setup() throws IOException {
        logger = new SpotifyLogger(LOGGER_FILE_PATH);
        createUsersFile();
    }

    @AfterAll
    static void clearLogger() {
        logger.close();
        Path.of(LOGGER_FILE_PATH + ".lck").toFile().delete();
        Path.of(LOGGER_FILE_PATH).toFile().delete();
    }

    private static void createUsersFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_CREDENTIALS_PATH.toString()))) {
            writer.write("admin,admin" + System.lineSeparator());
            writer.write("user@abv.bg,password" + System.lineSeparator());
        }
    }

    @BeforeEach
    void setupService() throws IOException {
        Path.of(LOGGER_FILE_PATH).toFile().createNewFile();
        this.service = new DefaultSpotifyService(TEST_CREDENTIALS_PATH, logger);
    }

    @AfterEach
    void clearUp() {
        Path.of(LOGGER_FILE_PATH).toFile().delete();
    }

    @Test
    void testRegisterExceptions() {
        assertThrows(InvalidRegisterCredentialsException.class, () -> this.service.register("invalidEmail", "somePassword"),
                "Attempting to register with invalid email should throw an exception");
        // admin already exists in test-users.csv
        assertThrows(UserAlreadyRegisteredException.class, () -> this.service.register("user@abv.bg", "somePassword"),
                "Attempting to register with an already registered email should throw an exception");
        // simulating unsuccessful writing in file
//        Files.setPosixFilePermissions(TEST_CREDENTIALS_PATH, PosixFilePermissions.fromString("r--r--r--"));
//        assertThrows(UnsuccessfulStoringOfCredentialsException.class, () -> this.service.register("unregistered-user@abv.bg", "password"));
//        Files.setPosixFilePermissions(TEST_CREDENTIALS_PATH, PosixFilePermissions.fromString("rw-rw-rw-"));
    }

    @Test
    void testRegisterSuccessful() throws SpotifyException, IOException {
        this.service.register("valid-email@abv.bg", "password");
        String result = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(TEST_CREDENTIALS_PATH.toString()))) {
            result += reader.readLine();
            result += System.lineSeparator();
            result += reader.readLine();
            result += System.lineSeparator();
            result += reader.readLine();
            result += System.lineSeparator();
        }
        String expected = "admin,admin" + System.lineSeparator() +
                "user@abv.bg,password" + System.lineSeparator() +
                "valid-email@abv.bg,password" + System.lineSeparator();
        assertEquals(expected, result, "Registration with valid credentials should save the new user in the file");
    }

    @Test
    void testLogin() throws SpotifyException {
        this.service.login(this.key1, "admin", "admin");
        assertThrows(UserAlreadyLoggedInException.class, () -> this.service.login(key1, "admin", "admin"),
                "Method login() should throw a UserAlreadyLoggedInException, when attempting to login for a second time");
        assertThrows(UserDoesNotExistException.class, () -> this.service.login(key1, "Non-existent", "password"),
                "Method login() should throw a UserDoesNotExistException, when attempting to login with an unregistered email");
        assertThrows(IncorrectLoginCredentialsException.class, () -> this.service.login(key1, "admin", "wrong password"),
                "Method login() should throw a IncorrectLoginCredentialsException, when attempting to login with a wrong password");
    }

    @Test
    void testLogout() throws SpotifyException {
        this.service.login(this.key2, "admin", "admin");
        this.service.logout(key2);
        assertDoesNotThrow(() -> this.service.logout(key2),
                "Method logout() should throw a UserNotLoggedInException, when logging out for a second time with the same key");
    }

    @Test
    void testGetUsername() throws SpotifyException {
        assertNull(this.service.getUsername(key2),
                "When user is not logged in, getUsername() should return null");
        this.service.login(key2, "admin", "admin");
        assertEquals("admin", this.service.getUsername(key2),
                "Method getUsername() should work correctly for logged-in users");
        this.service.logout(key2);
    }
}

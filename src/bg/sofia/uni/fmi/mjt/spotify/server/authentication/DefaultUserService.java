package bg.sofia.uni.fmi.mjt.spotify.server.authentication;

import bg.sofia.uni.fmi.mjt.spotify.exceptions.IncorrectLoginCredentialsException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.InvalidRegisterCredentialsException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.SpotifyException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.UnsuccessfulStoringOfCredentialsException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.UserAlreadyLoggedInException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.UserAlreadyRegisteredException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.UserDoesNotExistException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.UserNotLoggedInException;
import bg.sofia.uni.fmi.mjt.spotify.logger.SpotifyLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DefaultUserService implements UserService {

    private static final Path CREDENTIALS_PATH = Path.of("resources", "authentication", "users.csv");
    private static final Path EMAIL_REGEX_PATH = Path.of("resources", "authentication", "email-regex.txt");
    private final Map<SelectionKey, String> loggedInUsers;
    private final Path credentialsPath;
    private final SpotifyLogger logger;
    private Map<String, String> users;
    private String regex;

    public DefaultUserService(SpotifyLogger logger) {
        this.logger = logger;
        this.credentialsPath = CREDENTIALS_PATH;
        retrieveUsers();
        retrieveEmailRegex();
        this.loggedInUsers = new HashMap<>();
    }

    public DefaultUserService(Path credentialsPath, SpotifyLogger logger) {
        this.logger = logger;
        this.credentialsPath = credentialsPath;
        retrieveUsers();
        retrieveEmailRegex();
        this.loggedInUsers = new HashMap<>();
    }

    private void retrieveEmailRegex() {
        try (BufferedReader reader = new BufferedReader(new FileReader(String.valueOf(EMAIL_REGEX_PATH)))) {
            this.regex = reader.readLine();
        } catch (IOException e) {
            if (this.logger != null) {
                this.logger.log("localhost", e);
            }
            // no-whitespace regex, so that registration would still be possible even without email verification
            this.regex = "^\\s*\\S+\\s*$";
        }
    }

    private void retrieveUsers() {
        try {
            this.users = Files.lines(this.credentialsPath)
                    .collect(Collectors.toMap(s -> s.split(",")[0], s -> s.split(",")[1]));
        } catch (IOException e) {
            if (this.logger != null) {
                this.logger.log("localhost", e);
            }
            this.users = new HashMap<>();
        }
    }

    @Override
    public void register(String email, String password) throws SpotifyException {
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);

        if (!email.matches(this.regex)) {
            throw new InvalidRegisterCredentialsException("You haven't specified a valid email for registration");
        }

        if (this.users.containsKey(email)) {
            throw new UserAlreadyRegisteredException("User is already registered");
        }

        this.users.put(email, password);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(String.valueOf(this.credentialsPath), true))) {
            writer.write(email + "," + password + System.lineSeparator());
        } catch (IOException e) {
            if (this.logger != null) {
                this.logger.log("localhost", e);
            }
            throw new UnsuccessfulStoringOfCredentialsException("Credentials could not be stored");
        }
    }

    @Override
    public void login(SelectionKey key, String email, String password) throws SpotifyException {
        Objects.requireNonNull(key);
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);

        if (!this.users.containsKey(email)) {
            throw new UserDoesNotExistException("User " + email + " does not exist");
        }

        if (!this.users.get(email).equals(password)) {
            throw new IncorrectLoginCredentialsException("Incorrect email or password. Please enter again");
        }

        if (this.loggedInUsers.containsKey(key)) {
            throw new UserAlreadyLoggedInException("You are already logged in as user " + getUsername(key));
        }

        this.loggedInUsers.put(key, email);
    }

    @Override
    public void logout(SelectionKey key) throws SpotifyException {
        Objects.requireNonNull(key);

        if (!this.loggedInUsers.containsKey(key)) {
            throw new UserNotLoggedInException("No user is logged in. Cannot log out");
        }

        this.loggedInUsers.remove(key);
    }

    @Override
    public boolean userIsNotLoggedIn(SelectionKey key) {
        return !this.loggedInUsers.containsKey(key);
    }

    @Override
    public String getUsername(SelectionKey key) {
        Objects.requireNonNull(key);

        if (this.loggedInUsers.containsKey(key)) {
            return this.loggedInUsers.get(key);
        }
        return null;
    }
}

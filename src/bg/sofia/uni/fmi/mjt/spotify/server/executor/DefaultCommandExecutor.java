package bg.sofia.uni.fmi.mjt.spotify.server.executor;

import bg.sofia.uni.fmi.mjt.spotify.exceptions.SpotifyException;
import bg.sofia.uni.fmi.mjt.spotify.logger.SpotifyLogger;
import bg.sofia.uni.fmi.mjt.spotify.server.DefaultSpotifyService;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifyService;

import java.nio.channels.SelectionKey;
import java.util.Arrays;

public class DefaultCommandExecutor implements CommandParser {

    private static final String REGISTER = "register";
    private static final String LOGIN = "login";
    private static final String DISCONNECT = "disconnect";
    private static final String SEARCH = "search";
    private static final String TOP = "top";
    private static final String CREATE_PLAYLIST = "create-playlist";
    private static final String ADD_SONG = "add-song-to";
    private static final String SHOW_PLAYLIST = "show-playlist";
    private static final String PLAY = "play";
    private static final String STOP = "stop";

    private final SpotifyService service;
    private final SpotifyLogger logger;

    public DefaultCommandExecutor(SpotifyLogger logger) {
        this.logger = logger;
        this.service = new DefaultSpotifyService(this.logger);
    }

    public DefaultCommandExecutor(SpotifyLogger logger, SpotifyService service) {
        this.logger = logger;
        this.service = service;
    }

    @Override
    public String parse(SelectionKey key, String input) {
        String[] arguments = input.split(" ");
        try {
            return switch (arguments[0]) {
                case REGISTER -> register(arguments[1], arguments[2]);
                case LOGIN -> login(key, arguments[1], arguments[2]);
                case DISCONNECT -> disconnect(key);
                case SEARCH -> search(arguments);
                case TOP -> top(arguments[1]);
                case CREATE_PLAYLIST -> createPlaylist(key, arguments[1].replaceAll("_", " "));
                case ADD_SONG -> addSongToPlaylist(key, arguments[1].replaceAll("_", " "),
                        arguments[2].replaceAll("_", " "));
                case SHOW_PLAYLIST -> showPlaylist(key, arguments[1].replaceAll("_", " "));
                case PLAY -> play(key, arguments[1].replaceAll("_", " "));
                case STOP -> stop(key);
                default -> ERROR_MESSAGE + " " + "Unknown command";
            };
        } catch (SpotifyException e) {
            // SpotifyExceptions are expected behaviour, representing incorrect usage
            // no need to log them
            return ERROR_MESSAGE + " " + e.getMessage();
        } catch (ArrayIndexOutOfBoundsException e) {
            // an expected behaviour because of incorrect user actions, no need to log
            return ERROR_MESSAGE + " " + "Invalid number of parameters for the command";
        } catch (Exception e) {
            // a result of a true error - we should log this
            if (this.logger != null) {
                this.logger.log(this.service.getUsername(key), e);
            }
            return ERROR_MESSAGE + " " + "A failure happened for unknown reasons";
        }
    }

    private String stop(SelectionKey key) throws SpotifyException {
        this.service.stopPlaying(key);
        return "No longer playing music";
    }

    private String play(SelectionKey key, String songName) throws SpotifyException {
        this.service.playSong(key, songName);
        return "Now playing " + songName;
    }

    private String showPlaylist(SelectionKey key, String playListName) throws SpotifyException {
        return this.service.showPlaylist(key, playListName);
    }

    private String addSongToPlaylist(SelectionKey key, String playlistName, String songName) throws SpotifyException {
        this.service.addSongToPlaylist(key, playlistName, songName);
        return "Song successfully added to playlist";
    }

    private String createPlaylist(SelectionKey key, String playlistName) throws SpotifyException {
        this.service.createPlaylist(key, playlistName);
        return "Playlist created successfully";
    }

    private String top(String numberOfTopSongs) throws SpotifyException {
        return this.service.top(Integer.parseInt(numberOfTopSongs));
    }

    private String search(String[] arguments) throws SpotifyException {
        return this.service.search(Arrays.stream(arguments).skip(1).toList());
    }

    private String disconnect(SelectionKey key) throws SpotifyException {
        this.service.logout(key);
        return "Disconnected";
    }

    private String login(SelectionKey key, String email, String password) throws SpotifyException {
        this.service.login(key, email, password);
        return "User successfully logged in";
    }

    private String register(String email, String password) throws SpotifyException {
        this.service.register(email, password);
        return String.format("User %s registered", email);
    }
}

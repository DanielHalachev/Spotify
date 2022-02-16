package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.exceptions.SpotifyException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.UserNotLoggedInException;
import bg.sofia.uni.fmi.mjt.spotify.logger.SpotifyLogger;
import bg.sofia.uni.fmi.mjt.spotify.server.authentication.DefaultUserService;
import bg.sofia.uni.fmi.mjt.spotify.server.authentication.UserService;
import bg.sofia.uni.fmi.mjt.spotify.server.media.DefaultMediaService;
import bg.sofia.uni.fmi.mjt.spotify.server.media.MediaService;

import java.nio.channels.SelectionKey;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class DefaultSpotifyService implements SpotifyService {

    private final UserService userService;
    private final MediaService mediaService;
    private final SpotifyLogger logger;

    public DefaultSpotifyService(Path credentialsPath, SpotifyLogger logger) {
        this.logger = logger;
        this.userService = new DefaultUserService(credentialsPath, this.logger);
        this.mediaService = new DefaultMediaService(this.logger);
    }

    public DefaultSpotifyService(Path credentialsPath, SpotifyLogger logger, MediaService mediaService) {
        this.logger = logger;
        this.userService = new DefaultUserService(credentialsPath, this.logger);
        this.mediaService = mediaService;
    }

    public DefaultSpotifyService(SpotifyLogger logger) {
        this.logger = logger;
        this.userService = new DefaultUserService(this.logger);
        this.mediaService = new DefaultMediaService(this.logger);
    }


    @Override
    public void register(String email, String password) throws SpotifyException {
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);

        this.userService.register(email, password);
    }

    @Override
    public void login(SelectionKey key, String email, String password) throws SpotifyException {
        Objects.requireNonNull(key);
        Objects.requireNonNull(email);
        Objects.requireNonNull(password);

        this.userService.login(key, email, password);
    }

    @Override
    public void logout(SelectionKey key) {
        Objects.requireNonNull(key);

        try {
            this.userService.logout(key);
        } catch (SpotifyException e) {
            // nothing to do here
        }

        try {
            this.mediaService.stopPlayingSong(key);
        } catch (SpotifyException e) {
            // nothing to do here either
        }

        this.mediaService.update();
    }

    @Override
    public String search(List<String> keywords) {
        Objects.requireNonNull(keywords);

        return this.mediaService.search(keywords);
    }

    @Override
    public String top(int numberOfTopSongs) {
        return this.mediaService.top(numberOfTopSongs);
    }

    @Override
    public void createPlaylist(SelectionKey key, String playlistName) throws SpotifyException {
        Objects.requireNonNull(key);
        Objects.requireNonNull(playlistName);

        if (this.userService.userIsNotLoggedIn(key)) {
            throw new UserNotLoggedInException("Please log in to perform this action");
        }

        this.mediaService.createPlaylist(this.userService.getUsername(key), playlistName);
    }

    @Override
    public void addSongToPlaylist(SelectionKey key, String playlistName, String songName) throws SpotifyException {
        Objects.requireNonNull(key);
        Objects.requireNonNull(playlistName);
        Objects.requireNonNull(songName);

        if (this.userService.userIsNotLoggedIn(key)) {
            throw new UserNotLoggedInException("Please log in to perform this action");
        }

        this.mediaService.addSongToPlaylist(this.userService.getUsername(key), playlistName, songName);
    }

    @Override
    public String showPlaylist(SelectionKey key, String playlistName) throws SpotifyException {
        Objects.requireNonNull(key);
        Objects.requireNonNull(playlistName);

        return this.mediaService.getPlaylist(this.userService.getUsername(key), playlistName).toReadableString();
    }

    @Override
    public void playSong(SelectionKey key, String songName) throws SpotifyException {
        Objects.requireNonNull(key);
        Objects.requireNonNull(songName);

        if (this.userService.userIsNotLoggedIn(key)) {
            throw new UserNotLoggedInException("Please log in to perform this action");
        }

        this.mediaService.playSong(key, songName);
    }

    @Override
    public void stopPlaying(SelectionKey key) throws SpotifyException {
        Objects.requireNonNull(key);

        if (this.userService.userIsNotLoggedIn(key)) {
            throw new UserNotLoggedInException("Please log in to perform this action");
        }

        this.mediaService.stopPlayingSong(key);
    }

    @Override
    public String getUsername(SelectionKey key) {
        Objects.requireNonNull(key);

        return this.userService.getUsername(key);
    }
}

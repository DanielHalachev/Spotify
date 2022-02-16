package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.exceptions.SpotifyException;

import java.nio.channels.SelectionKey;
import java.util.List;

public interface SpotifyService {

    void register(String email, String password) throws SpotifyException;

    void login(SelectionKey key, String email, String password) throws SpotifyException;

    void logout(SelectionKey key);

    String search(List<String> keywords);

    String top(int numberOfTopSongs);

    void createPlaylist(SelectionKey key, String playlistName) throws SpotifyException;

    void addSongToPlaylist(SelectionKey key, String playlistName, String songName) throws SpotifyException;

    String showPlaylist(SelectionKey key, String playlistName) throws SpotifyException;

    void playSong(SelectionKey key, String songName) throws SpotifyException;

    void stopPlaying(SelectionKey key) throws SpotifyException;

    String getUsername(SelectionKey key);
}

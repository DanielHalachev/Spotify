package bg.sofia.uni.fmi.mjt.spotify.server.media;

import bg.sofia.uni.fmi.mjt.spotify.exceptions.SongNotFoundException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.SpotifyException;
import bg.sofia.uni.fmi.mjt.spotify.objects.Playlist;
import bg.sofia.uni.fmi.mjt.spotify.objects.Song;

import java.nio.channels.SelectionKey;
import java.util.List;

public interface MediaService {

    Song getSong(String songName) throws SongNotFoundException;

    String search(List<String> keywords);

    String top(int numberOfTopSongs);

    void createPlaylist(String email, String playlistName) throws SpotifyException;

    void addSongToPlaylist(String email, String playlistName, String songName) throws SpotifyException;

    Playlist getPlaylist(String email, String playlistName) throws SpotifyException;

    void playSong(SelectionKey key, String songName) throws SpotifyException;

    void stopPlayingSong(SelectionKey key) throws SpotifyException;

    void update();
}

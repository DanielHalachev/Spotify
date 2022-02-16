package bg.sofia.uni.fmi.mjt.spotify.objects;

import bg.sofia.uni.fmi.mjt.spotify.exceptions.SongAlreadyExistsException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Playlist {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";

    private final String name;
    private final Map<String, Song> songs;

    public Playlist(String playlistName) {
        this.name = playlistName;
        this.songs = new HashMap<>();
    }

    public void addSong(Song song) throws SongAlreadyExistsException {
        if (this.songs.containsKey(song.getName())) {
            throw new SongAlreadyExistsException("Song " + song.getName() + " already exists in this playlist");
        }
        this.songs.put(song.getName(), song);
    }

    public String toReadableString() {
        StringBuilder builder = new StringBuilder(ANSI_RED + this.name + ANSI_RESET + System.lineSeparator());
        int counter = 1;
        for (Song song : this.songs.values()) {
            builder.append(counter).append(". ").append(song.toReadableString()).append(System.lineSeparator());
            counter++;
        }
        return builder.toString();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Playlist playlist = (Playlist) o;
        return getName().equals(playlist.getName()) && songs.equals(playlist.songs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), songs);
    }
}

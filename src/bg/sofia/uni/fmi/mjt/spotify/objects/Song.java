package bg.sofia.uni.fmi.mjt.spotify.objects;

import java.nio.file.Path;
import java.util.Objects;

public class Song {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";

    private final String name;
    private final String artist;
    private final int duration;
    private int listeningTimes = 0;

    public Song(String name, String artist, int duration) {
        this.name = name;
        this.artist = artist;
        this.duration = duration;
    }

    public Path getPath() {
        return Path.of("resources", "songs", this.artist + " - " + this.name + " - " + this.duration + ".wav");
    }

    public int getNumberOfListens() {
        return this.listeningTimes;
    }

    public void play() {
        this.listeningTimes++;
    }

    public String toReadableString() {
        return String.format("%s%s%s - %s%s%s", ANSI_RED, this.artist, ANSI_RESET, ANSI_RED, this.name, ANSI_RESET);
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public int getDuration() {
        return duration;
    }

    public int getListeningTimes() {
        return listeningTimes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return getDuration() == song.getDuration()
                && getName().equals(song.getName())
                && getArtist().equals(song.getArtist());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getArtist(), getDuration());
    }
}

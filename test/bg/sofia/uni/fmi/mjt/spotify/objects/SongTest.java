package bg.sofia.uni.fmi.mjt.spotify.objects;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SongTest {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private final Song testSong = new Song("Name", "Artist", 120);

    @Test
    void testSong() {
        this.testSong.play();
        assertEquals(1, testSong.getNumberOfListens(),
                "Method play() should increase the listening times");
        // technically testing special getters, but they are important
        assertEquals(Path.of("resources", "songs", "Artist - Name - 120.wav"), this.testSong.getPath(),
                "Method getPath() should return the correct path to the audio file");
        assertEquals(ANSI_RED + this.testSong.getArtist() + ANSI_RESET +
                        " - " +
                        ANSI_RED + this.testSong.getName() + ANSI_RESET,
                this.testSong.toReadableString(),
                "Method toReadableString() should return a color string of the song's artist and name");
    }
}

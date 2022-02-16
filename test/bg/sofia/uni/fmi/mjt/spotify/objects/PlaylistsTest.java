package bg.sofia.uni.fmi.mjt.spotify.objects;

import bg.sofia.uni.fmi.mjt.spotify.exceptions.SongAlreadyExistsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PlaylistsTest {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private final Playlist testPlaylist = new Playlist("MyPlaylist");

    @Test
    void testPlaylist() throws SongAlreadyExistsException {
        Song testSong = new Song("Name", "Artist", 120);
        final String header = ANSI_RED + this.testPlaylist.getName() + ANSI_RESET + System.lineSeparator();
        // indirectly testing toReadableString() through all these assertions
        assertEquals(header, this.testPlaylist.toReadableString(),
                "Method toReadableString() should return the proper format of the playlist");
        String resultingString = header + "1. " + testSong.toReadableString() + System.lineSeparator();
        // test addSong() with new song
        this.testPlaylist.addSong(testSong);
        assertEquals(resultingString, this.testPlaylist.toReadableString(),
                "Method addSong should add the song, if it's already in the playlist");
        // test addSong() with already added song
        assertThrows(SongAlreadyExistsException.class, () -> this.testPlaylist.addSong(testSong),
                "Adding an already existing song should throw a SongAlreadyExistsException");
        assertEquals(resultingString, this.testPlaylist.toReadableString(),
                "Method addSong() should add the song in the playlist, if it's already there");
    }
}

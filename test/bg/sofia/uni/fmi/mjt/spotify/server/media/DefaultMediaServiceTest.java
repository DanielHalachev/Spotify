package bg.sofia.uni.fmi.mjt.spotify.server.media;

import bg.sofia.uni.fmi.mjt.spotify.exceptions.NoSongPlayingException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.PlaylistAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.PlaylistNotFoundException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.SongAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.SongNotFoundException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.SpotifyException;
import bg.sofia.uni.fmi.mjt.spotify.logger.SpotifyLogger;
import bg.sofia.uni.fmi.mjt.spotify.objects.Playlist;
import bg.sofia.uni.fmi.mjt.spotify.objects.Song;
import bg.sofia.uni.fmi.mjt.spotify.server.DefaultSpotifyService;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifyService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

// testing the MediaService class through the SpotifyService class, because it's basically a Wrapper
public class DefaultMediaServiceTest {

    private static final Path STOPWORDS_PATH = Path.of("resources", "stopwords.txt");
    private static final Path TEST_CREDENTIALS_PATH = Path.of("test-resources", "users", "test-users.csv");
    private static final Path TEST_SONGS_PATH = Path.of("test-resources", "storage", "test-songs.txt");
    private static final Path TEST_PLAYLISTS_PATH = Path.of("test-resources", "storage", "test-playlists.txt");
    private static final Path TEST_INDEX_PATH = Path.of("test-resources", "storage", "test-index.txt");
    private static final Path PERMANENT_PLAYLISTS_PATH = Path.of("test-resources", "storage", "permanent-playlists.txt");
    private static final Path EXPECTED_PLAYLISTS_PATH = Path.of("test-resources", "storage", "expected-playlists.txt");
    private static final Song[] songs = new Song[5];
    private static final String LOGGER_FILE_PATH = "mediaServiceLogs.log";
    private static SpotifyLogger logger;
    private SpotifyService service;
    @Mock
    private SelectionKey key = mock(SelectionKey.class);

    @BeforeAll
    static void setup() throws IOException {
        logger = new SpotifyLogger(LOGGER_FILE_PATH);
        // song 4 - 4 listens
        songs[4] = new Song("Smooth", "Santana", 143);
        for (int i = 0; i < 4; i++) {
            songs[4].play();
        }
        // song 3 - 3 listens
        songs[3] = new Song("Into The Night", "Santana", 140);
        for (int i = 0; i < 3; i++) {
            songs[3].play();
        }
        // song 2 - 2 listens
        songs[2] = new Song("Unchain My Heart", "Joe Cocker", 205);
        for (int i = 0; i < 2; i++) {
            songs[2].play();
        }
        // song 1 - 1 listens
        songs[1] = new Song("Morirò da Re", "Måneskin", 111);
        songs[1].play();
        // song 0 - 0 listens
        songs[0] = new Song("Losing My Religion", "R.E.M.", 210);

        restoreOriginalPlaylists();
    }

    private static void restoreOriginalPlaylists() throws IOException {
        // transfer the starting data, in case the wrong data has remained from previous tests
        // only the playlist file can change for these tests
        File correctPlaylists = PERMANENT_PLAYLISTS_PATH.toFile();
        File currentPlaylists = TEST_PLAYLISTS_PATH.toFile();
        FileChannel source = new FileInputStream(correctPlaylists).getChannel();
        FileChannel destination = new FileOutputStream(currentPlaylists).getChannel();
        destination.transferFrom(source, 0, source.size());
    }

    @AfterAll
    static void clearLogger() {
        logger.close();
        Path.of(LOGGER_FILE_PATH + ".lck").toFile().delete();
        Path.of(LOGGER_FILE_PATH).toFile().delete();
    }

    @AfterEach
    void clearUp() {
        Path.of(LOGGER_FILE_PATH).toFile().delete();
    }

    @BeforeEach
    void setupService() throws SpotifyException {
        MediaService mediaService = new DefaultMediaService(logger, TEST_SONGS_PATH, TEST_PLAYLISTS_PATH, TEST_INDEX_PATH);
        this.service = new DefaultSpotifyService(TEST_CREDENTIALS_PATH, logger, mediaService);
        this.service.login(key, "admin", "admin");
    }

    @Test
    void testConstructors() throws IOException {
        assertDoesNotThrow(() -> new DefaultMediaService(logger, null, null, null));
        Files.setPosixFilePermissions(STOPWORDS_PATH, PosixFilePermissions.fromString("r--r--r--"));
        assertDoesNotThrow(() -> new DefaultMediaService(logger),
                "Problems reading the stopwords should not throw an exception");
        Files.setPosixFilePermissions(STOPWORDS_PATH, PosixFilePermissions.fromString("rw-rw-rw-"));
    }

    @Test
    void testSearchFailure() {
        String positiveResults = songs[2].toReadableString() + System.lineSeparator();
        assertEquals(positiveResults, this.service.search(Arrays.stream(new String[]{"Joe", "Cocker"}).toList()),
                "Method search() should return accurate results");
    }

    @Test
    void testSearchSuccess() {
        String negativeResults = "We couldn't find any songs based on your query";
        assertEquals(negativeResults, this.service.search(Arrays.stream(new String[]{"I", "hate", "life"}).toList()),
                "Method search() should return an explanation string, when there are no songs that match the query");
    }

    @Test
    void testTopFailure() {
        String negativeResult = "Cannot find a negative number of top songs";
        assertEquals(negativeResult, this.service.top(-1),
                "Requesting negative number of tip songs should throw an exception");
    }

    @Test
    void testTopSuccess() {
        String builder = songs[4].toReadableString() + System.lineSeparator() +
                songs[3].toReadableString() + System.lineSeparator() +
                songs[2].toReadableString() + System.lineSeparator() +
                songs[1].toReadableString() + System.lineSeparator() +
                songs[0].toReadableString() + System.lineSeparator();
        assertEquals(builder, this.service.top(10),
                "Method top should return the songs, ordered by listening times");
    }

    @Test
    void testCreatePlaylist() throws SpotifyException {
        assertThrows(PlaylistAlreadyExistsException.class, () -> this.service.createPlaylist(key, "MySongs"),
                "Attempting to create an already existing playlist should throw a PlaylistAlreadyExistsException");
        this.service.createPlaylist(key, "NewPlaylist");
        // if we add a playlist successfully, adding it the second time should throw an exception
        assertThrows(PlaylistAlreadyExistsException.class, () -> this.service.createPlaylist(key, "NewPlaylist"),
                "Adding a playlist should work correctly at least in the dynamic memory (static files not counted)");
    }

    @Test
    void testAddSongToPlaylistFailure() {
        assertThrows(PlaylistNotFoundException.class, () -> this.service.addSongToPlaylist(key, "MySongs3", "Smooth"),
                "Method addSongToPlaylist() should throw a PlaylistNotFoundException if the specified playlist doesn't exist");
        assertThrows(SongNotFoundException.class, () -> this.service.addSongToPlaylist(key, "MySongs", "InvalidSong"),
                "Method addSongToPlaylist() should throw a SongNotFoundException, when the specified song does not exist");
    }

    @Test
    void testAddSongToPlaylistSuccess() throws SpotifyException {
        this.service.addSongToPlaylist(key, "MySongs", "Losing My Religion");
        // if we add a song successfully, adding it the second time should throw an exception
        assertThrows(SongAlreadyExistsException.class, () -> this.service.addSongToPlaylist(key, "MySongs", "Losing My Religion"),
                "Adding a song to a playlist should work correctly at least in the dynamic memory (static files not counted)");
    }

    @Test
    void testShowPlaylist() throws SpotifyException {
        Playlist expected1 = new Playlist("MySongs");
        expected1.addSong(songs[2]);
        expected1.addSong(songs[4]);
        Playlist expected2 = new Playlist("MySongs2");
        expected2.addSong(songs[0]);
        expected2.addSong(songs[2]);
        assertThrows(PlaylistNotFoundException.class, () -> this.service.showPlaylist(key, "Non-existentPlaylist"));
        assertEquals(expected1.toReadableString(), this.service.showPlaylist(key, "MySongs"),
                "When the same user has created a playlist with the specified name, getPlaylist() should return that playlist");
        assertEquals(expected2.toReadableString(), this.service.showPlaylist(key, "MySongs2"),
                "When the searching user hasn't created a playlist with the specified name, the method should return any other playlist with that name");
    }

    @Test
    void testPlaySong() throws IOException {
        assertThrows(SongNotFoundException.class, () -> this.service.playSong(key, "Non-existentSong"),
                "Attempting to play a non-existent song should throw a SongNotFoundException");
        Path source = Path.of("resources", "songs", "Santana - Smooth - 143.wav");
        Files.move(source, source.resolveSibling("temp.wav"));
        assertThrows(SongNotFoundException.class, () -> this.service.playSong(key, "Smooth"),
                "Attempting to play a song with a missing .wav file should throw a SongNotFoundException");
        Path temp = Path.of("resources", "songs", "temp.wav");
        Files.move(temp, temp.resolveSibling("Santana - Smooth - 143.wav"), StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    void testStopPlaying() {
        assertThrows(NoSongPlayingException.class, () -> this.service.stopPlaying(key),
                "Trying to stop a song, when it's not playing should throw a NoSongPlayingException");
    }

    @Test
    void testUpdate() throws IOException, SpotifyException {
        this.service.addSongToPlaylist(key, "MySongs", "Losing My Religion");
        this.service.createPlaylist(key, "NewPlaylist");
        this.service.logout(key);
        // a song only updates, if it is played
        // the index updates only if a song is updated
        // these tests do not perform song playing
        // the playlists update whenever a new playlist is added or a song is added to one of them though
        // therefore only the playlists file will differ and this is what this test checks
        String expected, result;
        try (BufferedReader reader = new BufferedReader(new FileReader(EXPECTED_PLAYLISTS_PATH.toString()))) {
            expected = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(TEST_PLAYLISTS_PATH.toString()))) {
            result = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        assertEquals(expected, result, "Method update() should update the files");
        restoreOriginalPlaylists();
    }
}

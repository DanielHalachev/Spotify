package bg.sofia.uni.fmi.mjt.spotify.server.media;

import bg.sofia.uni.fmi.mjt.spotify.exceptions.NoSongPlayingException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.PlaylistAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.PlaylistNotFoundException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.SongNotFoundException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.SpotifyException;
import bg.sofia.uni.fmi.mjt.spotify.logger.SpotifyLogger;
import bg.sofia.uni.fmi.mjt.spotify.objects.Playlist;
import bg.sofia.uni.fmi.mjt.spotify.objects.Song;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.SelectionKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultMediaService implements MediaService {

    private static final Path PLAYLISTS_PATH = Path.of("resources", "media", "playlists.txt");
    private static final Path SONGS_PATH = Path.of("resources", "media", "songs.txt");
    private static final Path INDEX_PATH = Path.of("resources", "media", "index.txt");
    private static final Path STOPWORDS_PATH = Path.of("resources", "stopwords.txt");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private boolean changeHasOccurred = false;
    private final SpotifyLogger logger;

    // a map of <UserName, <Name of playlist, Playlist>> for fast access

    private Map<String, Map<String, Playlist>> playlists;
    // all songs, mapped by their name (two songs are identical if their name, artist and duration are identical)
    private Map<String, Set<Song>> songs;
    // an index of keywords and the songs that contain them
    private Map<String, Set<Song>> index;
    private Set<String> stopwords;
    private final Map<SelectionKey, SongThread> currentlyPlaying;
    private final Path songsPath;
    private final Path playlistPath;
    private final Path indexPath;

    public DefaultMediaService(SpotifyLogger logger) {
        this(logger, SONGS_PATH, PLAYLISTS_PATH, INDEX_PATH);
    }

    public DefaultMediaService(SpotifyLogger logger, Path songsPath, Path playlistsPath, Path indexPath) {
        this.logger = logger;
        this.songs = new HashMap<>();
        this.playlists = new HashMap<>();
        this.index = new HashMap<>();
        this.songsPath = songsPath;
        this.playlistPath = playlistsPath;
        this.indexPath = indexPath;
        try {
            Type songType = new TypeToken<Map<String, Set<Song>>>() {
            }.getType();
            Type playListType = new TypeToken<Map<String, Map<String, Playlist>>>() {
            }.getType();
            Type indexType = new TypeToken<Map<String, Set<Song>>>() {
            }.getType();
            this.songs = GSON.fromJson(new FileReader(String.valueOf(this.songsPath)), songType);
            this.playlists = GSON.fromJson(new FileReader(String.valueOf(this.playlistPath)), playListType);
            this.index = GSON.fromJson(new FileReader(String.valueOf(this.indexPath)), indexType);
        } catch (FileNotFoundException e) {
            if (this.logger != null) {
                logger.log("localhost", e);
            }
        }
        this.currentlyPlaying = new HashMap<>();
        try {
            this.stopwords = Files.lines(STOPWORDS_PATH).collect(Collectors.toSet());
        } catch (IOException e) {
            if (this.logger != null) {
                this.logger.log("localhost", e);
            }
            this.stopwords = new HashSet<>();
        }
    }

    @Override
    public Song getSong(String songName) throws SongNotFoundException {
        Objects.requireNonNull(songName);
        if (!this.songs.containsKey(songName) || this.songs.get(songName).isEmpty()) {
            throw new SongNotFoundException("The song " + songName + " was not found");
        }
        return this.songs.get(songName).iterator().next();
    }

    @Override
    public String search(List<String> keywords) {
        Objects.requireNonNull(keywords);
        Set<String> filteredKeywords = keywords.stream()
                .map(String::toLowerCase)
                .filter(s -> !this.stopwords.contains(s))
                .collect(Collectors.toSet());
        List<Song> songsByGivenKeywords = new ArrayList<>();
        for (String word : filteredKeywords) {
            if (this.index.containsKey(word)) {
                songsByGivenKeywords.addAll(this.index.get(word));
            }
        }
        if (songsByGivenKeywords.isEmpty()) {
            return "We couldn't find any songs based on your query";
        }
        StringBuilder builder = new StringBuilder();
        songsByGivenKeywords.stream()
                .collect(Collectors.groupingBy(boardGame -> boardGame, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Song, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .forEach(song -> builder.append(song.toReadableString()).append(System.lineSeparator()));
        return builder.toString();
    }

    @Override
    public String top(int numberOfTopSongs) {
        if (numberOfTopSongs <= 0) {
            return "Cannot find a negative number of top songs";
        }
        StringBuilder builder = new StringBuilder();
        this.songs.values().stream()
                .flatMap(Set::stream)
                .sorted(Comparator.comparingInt(Song::getNumberOfListens).reversed())
                .limit(numberOfTopSongs)
                .forEach(song -> builder.append(song.toReadableString()).append(System.lineSeparator()));
        return builder.toString();
    }

    @Override
    public void createPlaylist(String email, String playlistName) throws SpotifyException {
        Objects.requireNonNull(email);
        Objects.requireNonNull(playlistName);
        this.playlists.putIfAbsent(email, new HashMap<>());
        if (this.playlists.get(email).containsKey(playlistName)) {
            throw new PlaylistAlreadyExistsException("Playlist with this name already exists. " +
                    "Please choose another name or add songs to the already existing playlist");
        }
        this.playlists.get(email).put(playlistName, new Playlist(playlistName));
        this.changeHasOccurred = true;
    }

    @Override
    public void addSongToPlaylist(String email, String playlistName, String songName) throws SpotifyException {
        Objects.requireNonNull(email);
        Objects.requireNonNull(playlistName);
        Objects.requireNonNull(songName);
        this.playlists.putIfAbsent(email, new HashMap<>());
        if (!this.playlists.get(email).containsKey(playlistName)) {
            throw new PlaylistNotFoundException("No playlist with this name was found. " +
                    "Please choose another playlist or create it before adding a song to it");
        }
        Song song = getSong(songName);
        this.playlists.get(email).get(playlistName).addSong(song);
        this.changeHasOccurred = true;
    }

    @Override
    public Playlist getPlaylist(String email, String playlistName) throws SpotifyException {
        Objects.requireNonNull(email);
        Objects.requireNonNull(playlistName);
        // there may be playlists with the same name by different users
        // first try to return the playlist with the same name by the same user
        if (this.playlists.get(email).containsKey(playlistName)) {
            return this.playlists.get(email).get(playlistName);
        }

        // if the user doesn't have such a playlist, then return any playlist with the same name
        for (Map<String, Playlist> map : this.playlists.values()) {
            if (map.containsKey(playlistName)) {
                return map.get(playlistName);
            }
        }

        // in case there is no playlist in the system with that name
        throw new PlaylistNotFoundException("No playlist with this name was found. " +
                "Please choose another playlist or create it before adding a song to it");
    }

    @Override
    public void playSong(SelectionKey key, String songName) throws SpotifyException {
        Objects.requireNonNull(key);
        Objects.requireNonNull(songName);
        Song songToPlay;
        try {
            songToPlay = this.songs.get(songName).iterator().next();
        } catch (NullPointerException e) {
            throw new SongNotFoundException("Song" + songName + " was not found");
        }
        File songFile = songToPlay.getPath().toFile();
        if (!songFile.exists()) {
            throw new SongNotFoundException("We know about the requested song, but it doesn't exist in our database. " +
                    "It cannot be played.");
        }
        // check whether the song is already running is performed in the client
        SongThread songThread = new SongThread(songToPlay, key, this.logger);
        this.songs.get(songName).iterator().next().play();
        this.currentlyPlaying.put(key, songThread);
        songThread.start();
        this.changeHasOccurred = true;
    }

    @Override
    public void stopPlayingSong(SelectionKey key) throws SpotifyException {
        Objects.requireNonNull(key);
        if (!this.currentlyPlaying.containsKey(key)) {
            throw new NoSongPlayingException("There is currently no song playing. Nothing to stop");
        }
        this.currentlyPlaying.get(key).stopPlaying();
        try {
            this.currentlyPlaying.get(key).join();
        } catch (InterruptedException e) {
            throw new SpotifyException("Something went wrong with the player");
        } finally {
            this.currentlyPlaying.remove(key);
        }
    }

    @Override
    public void update() {
        if (changeHasOccurred) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(String.valueOf(this.playlistPath), false))) {
                writer.write(GSON.toJson(this.playlists));
            } catch (IOException e) {
                if (this.logger != null) {
                    this.logger.log("localhost", e);
                }
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(String.valueOf(this.songsPath), false))) {
                writer.write(GSON.toJson(this.songs));
            } catch (IOException e) {
                if (this.logger != null) {
                    this.logger.log("localhost", e);
                }
            }
            this.index.clear();
            for (Song song : this.songs.values().stream().flatMap(Collection::stream).collect(Collectors.toSet())) {
                addSongToIndex(song);
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(String.valueOf(this.indexPath), false))) {
                writer.write(GSON.toJson(this.index));
            } catch (IOException e) {
                if (this.logger != null) {
                    this.logger.log("localhost", e);
                }
            }
        }
    }

    private void addSongToIndex(Song song) {
        Objects.requireNonNull(song);
        for (String str : Arrays.stream(song.getName()
                        .split(" "))
                .map(String::toLowerCase)
                .filter(s -> !this.stopwords.contains(s))
                .toList()) {
            if (!index.containsKey(str)) {
                index.put(str, new HashSet<>());
            }
            index.get(str).add(song);
        }
        for (String str : Arrays.stream(song.getArtist()
                        .split(" "))
                .map(String::toLowerCase)
                .filter(s -> !this.stopwords.contains(s))
                .toList()) {
            if (!index.containsKey(str)) {
                index.put(str, new HashSet<>());
            }
            index.get(str).add(song);
        }
    }
}

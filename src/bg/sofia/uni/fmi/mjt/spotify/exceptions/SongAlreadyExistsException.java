package bg.sofia.uni.fmi.mjt.spotify.exceptions;

public class SongAlreadyExistsException extends SpotifyException {

    public SongAlreadyExistsException(String message) {
        super(message);
    }
}

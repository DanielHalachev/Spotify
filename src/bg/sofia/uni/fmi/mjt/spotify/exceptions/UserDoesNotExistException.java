package bg.sofia.uni.fmi.mjt.spotify.exceptions;

public class UserDoesNotExistException extends SpotifyException {

    public UserDoesNotExistException(String message) {
        super(message);
    }
}

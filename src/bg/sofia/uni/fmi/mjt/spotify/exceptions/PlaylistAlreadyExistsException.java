package bg.sofia.uni.fmi.mjt.spotify.exceptions;

public class PlaylistAlreadyExistsException extends SpotifyException {

    public PlaylistAlreadyExistsException(String errorMessage) {
        super(errorMessage);
    }
}

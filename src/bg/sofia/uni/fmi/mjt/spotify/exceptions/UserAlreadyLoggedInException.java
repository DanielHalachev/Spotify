package bg.sofia.uni.fmi.mjt.spotify.exceptions;

public class UserAlreadyLoggedInException extends SpotifyException {

    public UserAlreadyLoggedInException(String message) {
        super(message);
    }
}

package bg.sofia.uni.fmi.mjt.spotify.exceptions;

public class UserNotLoggedInException extends SpotifyException {

    public UserNotLoggedInException(String errorMessage) {
        super(errorMessage);
    }
}

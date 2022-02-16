package bg.sofia.uni.fmi.mjt.spotify.server.authentication;

import bg.sofia.uni.fmi.mjt.spotify.exceptions.SpotifyException;

import java.nio.channels.SelectionKey;

public interface UserService {

    void register(String email, String password) throws SpotifyException;

    void login(SelectionKey key, String email, String password) throws SpotifyException;

    void logout(SelectionKey key) throws SpotifyException;

    boolean userIsNotLoggedIn(SelectionKey key);

    String getUsername(SelectionKey key);
}

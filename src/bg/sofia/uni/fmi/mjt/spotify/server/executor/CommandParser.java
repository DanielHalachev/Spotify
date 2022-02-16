package bg.sofia.uni.fmi.mjt.spotify.server.executor;

import java.nio.channels.SelectionKey;

public interface CommandParser {

    String ERROR_MESSAGE = "Error!";

    String parse(SelectionKey key, String input);
}

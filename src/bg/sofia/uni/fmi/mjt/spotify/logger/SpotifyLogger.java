package bg.sofia.uni.fmi.mjt.spotify.logger;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SpotifyLogger {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public SpotifyLogger(String loggerFilePath) throws IOException {
        Logger myLogger = Logger.getLogger("default");

        myLogger.setUseParentHandlers(false);

        LOGGER.setLevel(Level.SEVERE);

        FileHandler textFileHandler = new FileHandler(loggerFilePath);
        SimpleFormatter simpleFormatter = new SimpleFormatter();
        textFileHandler.setFormatter(simpleFormatter);
        myLogger.addHandler(textFileHandler);
    }

    public void log(String user, Exception e) {
        Logger myLogger = Logger.getLogger("default");
        myLogger.log(Level.SEVERE, user, e);
    }

    public void close() {
        for (Handler h : Logger.getLogger("default").getHandlers()) {
            h.close();   //must call h.close or a .LCK file will remain.
        }
    }
}

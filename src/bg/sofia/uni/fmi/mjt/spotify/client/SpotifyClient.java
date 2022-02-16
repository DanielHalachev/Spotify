package bg.sofia.uni.fmi.mjt.spotify.client;


import bg.sofia.uni.fmi.mjt.spotify.logger.SpotifyLogger;
import bg.sofia.uni.fmi.mjt.spotify.server.executor.CommandParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SpotifyClient {

    public static final String PROMPT_MESSAGE = "\u27a4\t";
    private static final String ANSI_CLEAR_CONSOLE = "\033[H\033[2J";
    private static final String ANSI_RED = "\033[1;91m";
    private static final String ANSI_ITALICS = "\033[3m";
    private static final String ANSI_RESET = "\033[0m";
    private static final String PLAY_COMMAND = "play";
    private static final String STOP_COMMAND = "stop";
    private static final String DISCONNECT_COMMAND = "disconnect";
    private static final String LOCALHOST = "localhost";

    private static final String LOGGER_FILE_NAME = "client.log";
    private static final String ADDRESS = "localhost";
    private static final int SERVER_PORT = 7777;
    private static SpotifyLogger logger;

    public static void main(String[] args) {
        try {
            logger = new SpotifyLogger(LOGGER_FILE_NAME);
        } catch (IOException e) {
            printToConsole("The logging files could not be created and errors will not be saved.");
        }

        displayGreetingMessage();

        try (SocketChannel socketChannel = SocketChannel.open();
             BufferedReader reader = new BufferedReader(Channels.newReader(socketChannel, StandardCharsets.UTF_8));
             PrintWriter writer =
                     new PrintWriter(Channels.newWriter(socketChannel, StandardCharsets.UTF_8), true)) {

            Scanner scanner = new Scanner(System.in);
            socketChannel.connect(new InetSocketAddress(ADDRESS, SERVER_PORT));
            MediaPlayer mediaPlayer = null;

            while (true) {
                System.out.print(PROMPT_MESSAGE);
                String message = scanner.nextLine();

                if (mediaPlayer != null && mediaPlayer.isAlive()) {
                    if (message.equals(STOP_COMMAND)) {
                        writer.println(message);
                        mediaPlayer.join();
                        // remove prompt symbol in case of unneeded adding
                        System.out.print("\b\b");
                    } else {
                        // if a song is playing, sending any message to the server would cause the streaming to fail
                        printToConsole("A song is currently playing. Please stop it before requesting a service");
                    }
                } else {
                    // play a song
                    if (message.startsWith(PLAY_COMMAND)) {
                        writer.println(message);
                        if (receiveValidResponse(reader)) {
                            try {
                                mediaPlayer = new MediaPlayer(socketChannel, logger);
                                mediaPlayer.start();
                            } catch (Exception e) {
                                // there are too many exception types that can occur here,
                                // but they all result in failure - catching them separately can miss some of them
                                printToConsole("There is a problem with the player. Cannot play");
                                if (logger != null) {
                                    logger.log(LOCALHOST, e);
                                }
                            }
                        }
                    } else if (message.startsWith(STOP_COMMAND)) {
                        // sending a stop message to the server
                        // without a song playing can potentially ruin any future streaming
                        printToConsole("No song is playing. Nothing to stop");
                    } else {
                        // if a song isn't playing, and we don't want to play, handle the response
                        writer.println(message);
                        receiveValidResponse(reader);
                        if (message.equals(DISCONNECT_COMMAND)) {
                            break;
                        }
                    }
                }
            }
            if (logger != null) {
                logger.close();
            }
        } catch (InterruptedException e) {
            // it's expected - no need to spam the logger
            // no action is required
        } catch (IOException e) {
            if (logger != null) {
                logger.log(LOCALHOST, e);
            }
            printToConsole("There is a problem with the network communication");
            logger.close();
        } catch (Exception e) {
            if (logger != null) {
                logger.log(LOCALHOST, e);
            }
            printToConsole("An error of unknown type occurred");
            logger.close();
        }
    }

    private static boolean receiveValidResponse(BufferedReader reader) throws IOException {
        String reply;
        do {
            reply = reader.readLine();
            printToConsole(reply);
        } while (reader.ready());

        return reply != null && !reply.startsWith(CommandParser.ERROR_MESSAGE);
    }

    private static void displayGreetingMessage() {
        System.out.println("Welcome to the Spotify client! Search for music and login or register to play it. " +
                "For songs or artists of more than one word, please enter them with \"_\". " + System.lineSeparator() +
                "Example: " + ANSI_RED + "play Unchain_My_Heart" + ANSI_RESET);
        System.out.print("Press Enter key to continue...");

        try {
            // accept Enter
            new Scanner(System.in).nextLine();
            // clear console - works only in a console/terminal but not in an IDE
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print(ANSI_CLEAR_CONSOLE);
                System.out.flush();
            }
        } catch (IOException | InterruptedException e) {
            // the user will simply have to live with an ugly console
            // no need to make it even uglier by showing a useless message
            if (logger != null) {
                logger.log(LOCALHOST, e);
            }
        }
    }

    private static void printToConsole(String message) {
        System.out.println(ANSI_ITALICS + message + ANSI_RESET);
    }
}
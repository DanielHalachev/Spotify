package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.logger.SpotifyLogger;
import bg.sofia.uni.fmi.mjt.spotify.server.executor.CommandParser;
import bg.sofia.uni.fmi.mjt.spotify.server.executor.DefaultCommandExecutor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SpotifyServer {

    private static final String LOGGER_FILE_NAME = "server.log";
    private static final String STOP_COMMAND = "stop";
    private static final String DISCONNECT_COMMAND = "disconnect";
    private static final String SERVER_HOST = "localhost";
    private static final int PORT = 7777;
    private static final int BUFFER_SIZE = 1024;
    private final int port;
    private boolean isOperating;
    private Selector selector;
    private SpotifyLogger logger = null;

    public SpotifyServer(int port) {
        this.port = port;
        this.isOperating = true;
    }

    public void start() {
        CommandParser commandParser;
        try {
            this.logger = new SpotifyLogger(LOGGER_FILE_NAME);
        } catch (IOException e) {
            System.err.println("Initializing the logger has failed. The system will continue working without logging");
        } finally {
            commandParser = new DefaultCommandExecutor(this.logger);
        }
        try (ServerSocketChannel server = ServerSocketChannel.open()) {

            server.bind(new InetSocketAddress(SERVER_HOST, this.port));
            server.configureBlocking(false);

            this.selector = Selector.open();
            server.register(this.selector, SelectionKey.OP_ACCEPT);

            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            while (this.isOperating) {
                int readyChannels = this.selector.select();
                if (readyChannels == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> selectionKeyIterator = selectedKeys.iterator();

                while (selectionKeyIterator.hasNext()) {
                    SelectionKey key = selectionKeyIterator.next();
                    if (key.isReadable()) {
                        SocketChannel sc = (SocketChannel) key.channel();
                        buffer.clear();
                        int bytesRead = sc.read(buffer);
                        if (bytesRead <= 0) {
                            sc.close();
                            break;
                        }

                        buffer.flip();
                        byte[] destination = new byte[bytesRead - 1];
                        buffer.get(destination, 0, bytesRead - 1);
                        String message = new String(destination);
                        String response = commandParser.parse(key, message) + System.lineSeparator();

                        System.out.println(message + ":\t" + response);
                        // sending a stop response to a client is pointless and can fail future song streaming
                        if (!message.startsWith(STOP_COMMAND)) {
                            sendResponse(buffer, sc, response);
                        }

                        if (message.startsWith(DISCONNECT_COMMAND)) {
                            sc.close();
                        }
                    } else if (key.isAcceptable()) {
                        accept(key);
                    }

                    selectionKeyIterator.remove();
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            this.logger.log("localhost", e);
        }
        logger.close();
    }

    private void sendResponse(ByteBuffer buffer, SocketChannel sc, String response) throws IOException {
        buffer.clear();
        buffer.put(response.getBytes());
        buffer.flip();
        sc.write(buffer);
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel sockChannel = (ServerSocketChannel) key.channel();
        SocketChannel accept = sockChannel.accept();
        accept.configureBlocking(false);
        accept.register(selector, SelectionKey.OP_READ);
    }

    public void stop() {
        logger.close();
        isOperating = false;
        selector.wakeup();
    }

    public static void main(String[] args) {
        SpotifyServer spotifyServer = new SpotifyServer(PORT);
        spotifyServer.start();
    }
}
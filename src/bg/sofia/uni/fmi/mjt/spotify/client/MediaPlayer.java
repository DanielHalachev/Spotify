package bg.sofia.uni.fmi.mjt.spotify.client;

import bg.sofia.uni.fmi.mjt.spotify.logger.SpotifyLogger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MediaPlayer extends Thread {

    private static final String ANSI_ITALICS = "\033[3m";
    private static final String ANSI_RESET = "\033[0m";

    private static final int BUFFER_SIZE = 4096;
    private static final int ENCODING_POSITION = 0;
    private static final int SAMPLE_RATE_POSITION = 1;
    private static final int SAMPLE_SIZE_POSITION = 2;
    private static final int CHANNELS_POSITION = 3;
    private static final int FRAME_SIZE_POSITION = 4;
    private static final int FRAME_RATE_POSITION = 5;
    private static final int ENDIAN_POSITION = 6;
    private static final String DONE = "Done";

    private final SocketChannel socketChannel;
    private final SpotifyLogger logger;

    MediaPlayer(SocketChannel socketChannel, SpotifyLogger logger) {
        this.socketChannel = socketChannel;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            int bytesRead;
            byte[] byteBuffer = new byte[BUFFER_SIZE];
            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            bytesRead = socketChannel.read(buffer);
            byte[] destination = new byte[bytesRead];
            buffer.flip();
            buffer.get(destination, 0, bytesRead);
            String contents = new String(destination);
            String[] arguments = contents.split("\\R");

            AudioFormat.Encoding encoding = new AudioFormat.Encoding(arguments[ENCODING_POSITION]);
            float sampleRate = Float.parseFloat(arguments[SAMPLE_RATE_POSITION]);
            int sampleSizeInBits = Integer.parseInt(arguments[SAMPLE_SIZE_POSITION]);
            int channels = Integer.parseInt(arguments[CHANNELS_POSITION]);
            int frameSize = Integer.parseInt(arguments[FRAME_SIZE_POSITION]);
            float frameRate = Float.parseFloat(arguments[FRAME_RATE_POSITION]);
            boolean bigEndian = Boolean.parseBoolean(arguments[ENDIAN_POSITION]);

            AudioFormat format = new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize,
                    frameRate, bigEndian);

            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine audioLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);

            audioLine.open(format);
            audioLine.start();

            while ((bytesRead = socketChannel.read(buffer)) != -1) {
                buffer.flip();
                buffer.get(byteBuffer, 0, bytesRead);
                contents = new String(byteBuffer);
                if (contents.contains(DONE)) {
                    break;
                }

                audioLine.write(byteBuffer, 0, bytesRead);
                buffer.clear();
            }

            audioLine.drain();
            audioLine.close();

            System.out.print("\b\b\b"); // remove prompt symbol
            printToConsole("Playing ended");
            System.out.print(SpotifyClient.PROMPT_MESSAGE); // put prompt symbol again
        } catch (LineUnavailableException | IOException e) {
            if (this.logger != null) {
                this.logger.log("localhost", e);
            }
            printToConsole("Song streaming was interrupted. Cannot continue");
        }
    }

    static void printToConsole(String message) {
        System.out.println(ANSI_ITALICS + message + ANSI_RESET);
    }
}

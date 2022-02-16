package bg.sofia.uni.fmi.mjt.spotify.server.media;

import bg.sofia.uni.fmi.mjt.spotify.logger.SpotifyLogger;
import bg.sofia.uni.fmi.mjt.spotify.objects.Song;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class SongThread extends Thread {

    private static final int BUFFER_SIZE = 4096;
    private static final int INTERRUPTION_PREVENTION_TIME = 20;
    private static final String DONE = "Done";
    private final SpotifyLogger logger;
    private final Song song;
    private final SelectionKey key;
    private boolean isRunning;

    public SongThread(Song song, SelectionKey key, SpotifyLogger logger) {
        this.song = song;
        this.key = key;
        this.isRunning = false;
        this.logger = logger;
    }

    @Override
    public void run() {
        isRunning = true;
        File file = new File(String.valueOf(song.getPath()));
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            AudioFormat audioFormat = audioStream.getFormat();
            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            String format = audioFormat.getEncoding().toString() + System.lineSeparator()
                    + audioFormat.getSampleRate() + System.lineSeparator()
                    + audioFormat.getSampleSizeInBits() + System.lineSeparator()
                    + audioFormat.getChannels() + System.lineSeparator()
                    + audioFormat.getFrameSize() + System.lineSeparator()
                    + audioFormat.getFrameRate() + System.lineSeparator()
                    + audioFormat.isBigEndian() + System.lineSeparator();

            buffer.clear();
            buffer.put(format.getBytes(StandardCharsets.UTF_8));
            buffer.flip();

            SocketChannel socketChannel = (SocketChannel) key.channel();
            socketChannel.write(buffer);

            byte[] byteBuffer = new byte[BUFFER_SIZE];

            while (isRunning && audioStream.read(byteBuffer) != -1) {
                buffer.clear();
                buffer.put(byteBuffer);
                buffer.flip();
                socketChannel.write(buffer);
                Thread.sleep(INTERRUPTION_PREVENTION_TIME);
            }

            buffer.clear();
            buffer.put(DONE.getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            socketChannel.write(buffer);

            audioStream.close();
        } catch (UnsupportedAudioFileException | IOException | InterruptedException e) {
            if (this.logger != null) {
                this.logger.log("localhost", e);
            }
            System.out.println(e.getMessage());
        }
        isRunning = false;
    }

    public void stopPlaying() {
        isRunning = false;
    }
}

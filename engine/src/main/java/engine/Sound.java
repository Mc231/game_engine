package engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * A single 16-bit PCM WAV sound loaded from the classpath into an OpenAL buffer,
 * with a dedicated source to play it. Requires a current {@link Audio} context.
 */
public class Sound implements Disposable {

    private final int buffer;
    private final int source;

    /** @param resourcePath classpath-relative path, e.g. "sounds/jump.wav". */
    public Sound(String resourcePath) {
        ByteBuffer wav = readResource(resourcePath).order(ByteOrder.LITTLE_ENDIAN);

        // RIFF header: "RIFF" <size:int> "WAVE".
        if (wav.getInt(0) != riffId('R', 'I', 'F', 'F') || wav.getInt(8) != riffId('W', 'A', 'V', 'E')) {
            throw new RuntimeException("Not a RIFF/WAVE file: " + resourcePath);
        }

        int channels = 0;
        int sampleRate = 0;
        int bitsPerSample = 0;
        ByteBuffer pcm = null;

        // Walk the chunks after the 12-byte RIFF/WAVE header.
        int pos = 12;
        while (pos + 8 <= wav.capacity()) {
            int chunkId = wav.getInt(pos);
            int chunkSize = wav.getInt(pos + 4);
            int body = pos + 8;

            if (chunkId == riffId('f', 'm', 't', ' ')) {
                channels = Short.toUnsignedInt(wav.getShort(body + 2));
                sampleRate = wav.getInt(body + 4);
                bitsPerSample = Short.toUnsignedInt(wav.getShort(body + 14));
            } else if (chunkId == riffId('d', 'a', 't', 'a')) {
                pcm = wav.slice(body, chunkSize);
            }

            // Chunks are word-aligned: an odd size is padded by one byte.
            pos = body + chunkSize + (chunkSize & 1);
        }

        if (pcm == null || channels == 0 || bitsPerSample == 0) {
            throw new RuntimeException("Missing fmt/data chunk in WAV: " + resourcePath);
        }

        int format = alFormat(channels, bitsPerSample, resourcePath);

        buffer = alGenBuffers();
        // OpenAL wants native (off-heap) memory; copy the PCM bytes into it.
        ByteBuffer nativePcm = memAlloc(pcm.remaining());
        try {
            nativePcm.put(pcm).flip();
            alBufferData(buffer, format, nativePcm, sampleRate);
        } finally {
            memFree(nativePcm);
        }

        source = alGenSources();
        alSourcei(source, AL_BUFFER, buffer);
    }

    /** Play from the start, restarting if already playing. */
    public void play() {
        alSourceStop(source);
        alSourcePlay(source);
    }

    /** When looping, playback repeats until stopped. */
    public void setLooping(boolean loop) {
        alSourcei(source, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
    }

    @Override
    public void dispose() {
        alDeleteSources(source);
        alDeleteBuffers(buffer);
    }

    private static int alFormat(int channels, int bitsPerSample, String resourcePath) {
        if (bitsPerSample != 16) {
            throw new RuntimeException("Only 16-bit PCM WAV is supported (" + resourcePath + ")");
        }
        if (channels == 1) {
            return AL_FORMAT_MONO16;
        }
        if (channels == 2) {
            return AL_FORMAT_STEREO16;
        }
        throw new RuntimeException("Unsupported channel count " + channels + " in " + resourcePath);
    }

    /** Little-endian four-character chunk id packed into an int, matching getInt. */
    private static int riffId(char a, char b, char c, char d) {
        return a | (b << 8) | (c << 16) | (d << 24);
    }

    private static ByteBuffer readResource(String path) {
        try (InputStream in = Sound.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Sound resource not found on classpath: " + path);
            }
            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read sound resource: " + path, e);
        }
    }
}

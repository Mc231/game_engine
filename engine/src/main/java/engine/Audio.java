package engine;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;

/**
 * Owns the OpenAL device + context for the whole application. Create one at
 * startup, keep it alive while any {@link Sound} is used, and {@link #destroy}
 * it on shutdown. Requires an audio device to be present.
 */
public class Audio {

    private final long device;
    private final long context;

    /** Open the default output device and make a current OpenAL context on it. */
    public Audio() {
        device = ALC10.alcOpenDevice((java.nio.ByteBuffer) null);
        if (device == 0L) {
            throw new RuntimeException("Failed to open the default OpenAL device");
        }

        context = ALC10.alcCreateContext(device, (java.nio.IntBuffer) null);
        if (context == 0L) {
            ALC10.alcCloseDevice(device);
            throw new RuntimeException("Failed to create an OpenAL context");
        }

        ALC10.alcMakeContextCurrent(context);
        // Wire up the LWJGL function pointers for this device/context.
        ALCCapabilities alcCaps = ALC.createCapabilities(device);
        AL.createCapabilities(alcCaps);
    }

    /** Release the context and close the device. Call once at shutdown. */
    public void destroy() {
        ALC10.alcMakeContextCurrent(0L);
        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(device);
    }
}

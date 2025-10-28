package org.example.tunesfx;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.example.tunesfx.utils.Utils;
import java.util.function.Supplier;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

public class Audio extends Thread {
    private volatile boolean initialized = false; // Usar volatile para acceso entre hilos

    boolean isInitialized() {
        return initialized;
    }

    static final int BUFFER_SIZE = 512; // 1024?
    static final int BUFFER_COUNT = 8;

    private final Supplier<short[]> bufferSupplier;
    private final int[] buffers = new int[BUFFER_COUNT];

    private long device;
    private long context;
    private int source;

    private int bufferIndex;
    private volatile boolean closed;
    private volatile boolean running;

    Audio(Supplier<short[]> bufferSupplier) {
        this.bufferSupplier = bufferSupplier;
        start();
    }

    private void initAudio() {
        device = alcOpenDevice(alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER));
        context = alcCreateContext(device, new int[1]);
        alcMakeContextCurrent(context);

        AL.createCapabilities(ALC.createCapabilities(device));

        source = alGenSources();
        catchInternalException();

        alGenBuffers(buffers);
        catchInternalException();

        for (int i = 0; i < BUFFER_COUNT; i++) {
            int buf = buffers[i];
            alBufferData(buf, AL_FORMAT_MONO16, new short[0], Sintetizador.AudioInfo.SAMPLE_RATE);
            alSourceQueueBuffers(source, buf);
        }
        bufferIndex = 0;

        alSourcePlay(source);
        catchInternalException();
        initialized = true;
    }

    boolean isRunning() {
        return running;
    }

    @Override
    public synchronized void run() {
        initAudio();
        while (!closed) {
            while (!running) {
                Utils.handleProcedure(this::wait,true);
            }
            int processedBufs = alGetSourcei(source,AL_BUFFERS_PROCESSED);
            for (int i = 0; i < processedBufs; ++i) {
                short[] samples = bufferSupplier.get();
                if (samples == null) {
                    running = false;
                    break;
                }
                int oldBuffer = alSourceUnqueueBuffers(source);
                alBufferData(oldBuffer, AL_FORMAT_MONO16, samples, Sintetizador.AudioInfo.SAMPLE_RATE);
                alSourceQueueBuffers(source, oldBuffer);
                catchInternalException();
            }
            if (alGetSourcei(source,AL_SOURCE_STATE) != AL_PLAYING) {
                alSourcePlay(source);
            }
            catchInternalException();
        }
        alDeleteSources(source);
        alDeleteBuffers(buffers);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    synchronized void triggerPlayBack() {
        running = true;
        notify();
    }

    void close() {
        closed = true;
        triggerPlayBack();
    }

    private void catchInternalException() {
        int err = alGetError();
        if (err != AL_NO_ERROR) {
            throw new OpenALException(err);
        }
    }
}
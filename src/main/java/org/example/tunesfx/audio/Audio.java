package org.example.tunesfx.audio;

import org.example.tunesfx.synth.Sintetizador;
import org.example.tunesfx.utils.OpenALException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.EXTDisconnect;
import org.example.tunesfx.utils.Utils;

import java.util.function.Supplier;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

public class Audio extends Thread {
    private volatile boolean initialized = false;

    public boolean isInitialized() {
        return initialized;
    }

    public static final int BUFFER_SIZE = 512;
    static final int BUFFER_COUNT = 8;

    private final Supplier<short[]> bufferSupplier;
    private final int[] buffers = new int[BUFFER_COUNT];

    // Recursos estáticos
    private static long device;
    private static long context;
    private static volatile boolean openALInitialized = false;

    private int source;
    private volatile boolean closed;
    private volatile boolean running;

    public Audio(Supplier<short[]> bufferSupplier) {
        this.bufferSupplier = bufferSupplier;
        start();
    }

    private void initAudio() {
        synchronized (Audio.class) {
            if (!openALInitialized) {
                // Abrimos el dispositivo por defecto actual
                device = alcOpenDevice(alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER));
                context = alcCreateContext(device, new int[1]);
                alcMakeContextCurrent(context);

                AL.createCapabilities(ALC.createCapabilities(device));
                openALInitialized = true;
            }
        }

        source = alGenSources();
        catchInternalException();

        alGenBuffers(buffers);
        catchInternalException();

        for (int i = 0; i < BUFFER_COUNT; i++) {
            int buf = buffers[i];
            AL10.alBufferData(buf, AL_FORMAT_MONO16, new short[0], Sintetizador.AudioInfo.SAMPLE_RATE);
            alSourceQueueBuffers(source, buf);
        }

        alSourcePlay(source);
        catchInternalException();
        initialized = true;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() { // <-- Quitamos el synchronized global
        initAudio();

        while (!closed) {
            try {
                // Bloqueamos solo la condición de espera
                synchronized (this) {
                    while (!running && !closed) {
                        Utils.handleProcedure(this::wait, true);
                    }
                }
                if (closed) break;

                // --- 1. DETECTAR DESCONEXIÓN ACTIVAMENTE ---
                if (openALInitialized && !isDeviceConnected()) {
                    System.out.println("Dispositivo de audio desconectado. Reconectando...");
                    reconnectAudio();
                    continue; // Volvemos al inicio del bucle tras recuperar
                }

                // Procesamiento de buffers normal
                int processedBufs = alGetSourcei(source, AL_BUFFERS_PROCESSED);
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

                if (alGetSourcei(source, AL_SOURCE_STATE) != AL_PLAYING) {
                    alSourcePlay(source);
                }
                catchInternalException();

            } catch (OpenALException e) {
                // --- 2. RECUPERAR HILO SI HAY ERROR FATAL ---
                // Si falla en medio de un proceso por culpa del OS, capturamos el error
                // para evitar que el Thread muera y forzamos la reconexión.
                System.err.println("Error del motor de audio (posible cambio de salida): " + e.getMessage());
                reconnectAudio();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        cleanupResources();
    }

    // Comprueba mediante la extensión de LWJGL si el hardware sigue físicamente ahí
    private boolean isDeviceConnected() {
        try {
            int[] connected = new int[1];
            alcGetIntegerv(device, EXTDisconnect.ALC_CONNECTED, connected);
            return connected[0] != ALC_FALSE;
        } catch (Exception e) {
            return false; // Ante la duda, forzamos reinicio
        }
    }

    // Realiza un reseteo completo en caliente (Hot Swap)
    private void reconnectAudio() {
        initialized = false;

        // Limpiamos todo rastro del dispositivo roto
        cleanupResources();
        shutdownOpenAL();

        // Pausa clave: le damos tiempo al SO para que asigne
        // los altavoces o auriculares nuevos como salida principal
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        // Volvemos a inicializar con la nueva salida
        initAudio();
        System.out.println("Audio reconectado con éxito.");
    }

    private void cleanupResources() {
        if (source != 0) {
            alDeleteSources(source);
            source = 0;
        }
        if (buffers[0] != 0) {
            alDeleteBuffers(buffers);
            for (int i = 0; i < BUFFER_COUNT; i++) buffers[i] = 0;
        }
    }

    public synchronized void triggerPlayBack() {
        running = true;
        notifyAll(); // Usar notifyAll es más seguro en hilos
    }

    public synchronized void close() {
        closed = true;
        running = true;
        notifyAll();
    }

    public static synchronized void shutdownOpenAL() {
        if (openALInitialized) {
            alcMakeContextCurrent(0L); // Importante: desvincular contexto primero
            if (context != 0L) alcDestroyContext(context);
            if (device != 0L) alcCloseDevice(device);
            openALInitialized = false;
            context = 0L;
            device = 0L;
            System.out.println("Contexto OpenAL destruido.");
        }
    }

    private void catchInternalException() {
        int err = alGetError();
        if (err != AL_NO_ERROR) {
            throw new OpenALException(err);
        }
    }
}
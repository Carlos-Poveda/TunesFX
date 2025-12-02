package org.example.tunesfx;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

public class Audio extends Thread {

    // SINGLETON (Para acceder desde cualquier lado)
    private static Audio instance;

    // CONFIGURACIÓN
    static final int BUFFER_SIZE = 512;
    static final int BUFFER_COUNT = 8;
    // Número de voces simultáneas para samples (batería, efectos)
    private static final int MAX_SAMPLE_SOURCES = 16;

    private final Supplier<short[]> synthBufferSupplier;
    private final int[] synthBuffers = new int[BUFFER_COUNT];

    // Variables de OpenAL
    private long device;
    private long context;

    // Fuente del Sintetizador (Stream)
    private int synthSource;

    // Pool de Fuentes para Samples (One-Shot)
    private final int[] sampleSources = new int[MAX_SAMPLE_SOURCES];
    private int sampleSourceIndex = 0; // Para rotar las voces (Round Robin)

    // Cola de reproducción (Thread-Safe)
    private final ConcurrentLinkedQueue<Sample> sampleQueue = new ConcurrentLinkedQueue<>();

    private volatile boolean initialized = false;
    private volatile boolean closed = false;
    private volatile boolean running = false;

    // Constructor privado ahora, usar getInstance
    private Audio(Supplier<short[]> bufferSupplier) {
        this.synthBufferSupplier = bufferSupplier;
    }

    // Método estático para iniciar el sistema
    public static synchronized void startAudioSystem(Supplier<short[]> bufferSupplier) {
        if (instance == null) {
            instance = new Audio(bufferSupplier);
            instance.start();
        }
    }

    public static Audio getInstance() {
        return instance;
    }

    // MÉTODO PÚBLICO: Disparar un sample (Lo mete en la cola para el hilo de audio)
    public void playSample(Sample sample) {
        if (sample != null && initialized) {
            sampleQueue.offer(sample);
            // Si el motor estaba "dormido" (wait), lo despertamos
            triggerPlayBack();
        }
    }

    private void initOpenAL() {
        // Inicialización del Dispositivo y Contexto
        device = alcOpenDevice(alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER));
        int[] attributes = {0};
        context = alcCreateContext(device, attributes);
        alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));

        System.out.println("OpenAL Inicializado. Hilo: " + Thread.currentThread().getName());

        // 1. Configurar Fuente del Sintetizador (Streaming)
        synthSource = alGenSources();
        alGenBuffers(synthBuffers);

        // Llenar buffers iniciales con silencio
        for (int buf : synthBuffers) {
            alBufferData(buf, AL_FORMAT_MONO16, new short[BUFFER_SIZE], Sintetizador.AudioInfo.SAMPLE_RATE);
            alSourceQueueBuffers(synthSource, buf);
        }
        alSourcePlay(synthSource);

        // 2. Configurar Fuentes de Samples (Pool)
        for (int i = 0; i < MAX_SAMPLE_SOURCES; i++) {
            sampleSources[i] = alGenSources();
        }

        initialized = true;
    }

    @Override
    public void run() {
        initOpenAL();

        while (!closed) {
            // A. GESTIÓN DEL SINTETIZADOR (Stream)
            // -------------------------------------
            if (running) {
                int processed = alGetSourcei(synthSource, AL_BUFFERS_PROCESSED);
                while (processed > 0) {
                    short[] samples = synthBufferSupplier.get();

                    // Si el sinte devuelve null, significa que no hay teclas pulsadas -> Silencio o Pausa
                    if (samples == null) {
                        // Opcional: Podrías poner running = false aquí si quieres parar el loop,
                        // pero para que los samples sigan sonando, mejor alimentamos silencio.
                        samples = new short[BUFFER_SIZE];
                        running = false; // O lo pausamos
                    }

                    int buffer = alSourceUnqueueBuffers(synthSource);
                    alBufferData(buffer, AL_FORMAT_MONO16, samples, Sintetizador.AudioInfo.SAMPLE_RATE);
                    alSourceQueueBuffers(synthSource, buffer);
                    processed--;
                }

                // Reiniciar si se detuvo por falta de datos (underrun)
                if (alGetSourcei(synthSource, AL_SOURCE_STATE) != AL_PLAYING) {
                    alSourcePlay(synthSource);
                }
            } else {
                // Si el sinte no está sonando, dormimos un poco para no quemar CPU,
                // PERO revisamos la cola de samples frecuentemente.
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }

            // B. GESTIÓN DE SAMPLES (One-Shot)
            // --------------------------------
            processSampleQueue();
        }

        cleanup();
    }

    private void processSampleQueue() {
        Sample s;
        while ((s = sampleQueue.poll()) != null) {
            // 1. Subir a memoria si es nuevo
            if (s.getOpenALBufferId() == AL_NONE) {
                int newBufferId = alGenBuffers();
                alBufferData(newBufferId, AL_FORMAT_MONO16, s.getData(), Sintetizador.AudioInfo.SAMPLE_RATE);
                s.setOpenALBufferId(newBufferId);
            }

            // 2. Buscar una fuente libre (o robar la siguiente en ciclo)
            int sourceId = sampleSources[sampleSourceIndex];

            // Detener si estaba sonando algo anterior
            alSourceStop(sourceId);

            // Asignar el buffer y tocar
            alSourcei(sourceId, AL_BUFFER, s.getOpenALBufferId());
            alSourcePlay(sourceId);

            // Avanzar índice (Round Robin)
            sampleSourceIndex = (sampleSourceIndex + 1) % MAX_SAMPLE_SOURCES;
        }
    }

    synchronized void triggerPlayBack() {
        running = true;
        // notify(); // Ya no usamos wait/notify estricto para permitir que los samples suenen sin sinte
    }

    boolean isRunning() {
        return running;
    }

    boolean isInitialized() {
        return initialized;
    }

    void close() {
        closed = true;
    }

    public static void shutdownOpenAL() {
        if (instance != null) {
            instance.close();
        }
    }

    private void cleanup() {
        alDeleteSources(synthSource);
        alDeleteBuffers(synthBuffers);

        // Limpiar fuentes de samples
        for (int src : sampleSources) {
            alDeleteSources(src);
        }
        // Nota: Los buffers de los Samples (Sample.openALBufferId) deberían limpiarse también,
        // pero para este ejemplo simple lo dejaremos al SO al cerrar.

        alcDestroyContext(context);
        alcCloseDevice(device);
        System.out.println("Audio System Cerrado.");
    }
}
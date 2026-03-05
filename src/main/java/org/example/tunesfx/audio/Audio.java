package org.example.tunesfx.audio;

import org.example.tunesfx.synth.Sintetizador;
import org.example.tunesfx.utils.OpenALException;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTDisconnect;
import org.example.tunesfx.utils.Utils;

import java.util.function.Supplier;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

public class Audio extends Thread {
    private volatile boolean initialized = false;
    // Reconexión automática: bandera para reiniciar desde el hilo de audio
    private volatile boolean needsReinit = false;

    // API para forzar reinicialización externa si fuera necesario
    public synchronized void requestReinit() {
        needsReinit = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public static final int BUFFER_SIZE = 512;
    static final int BUFFER_COUNT = 8;
    private final Supplier<short[]> bufferSupplier;
    private final int[] buffers = new int[BUFFER_COUNT];
    private int source;
    private volatile boolean closed;
    private volatile boolean running;

    public Audio(Supplier<short[]> bufferSupplier) {
        this.bufferSupplier = bufferSupplier;
        start();
    }

    private void initAudio() {
        source = alGenSources();
        catchInternalException();

        alGenBuffers(buffers);
        catchInternalException();

        for (int i = 0; i < BUFFER_COUNT; i++) {
            int buf = buffers[i];
            // Cargamos buffers vacíos iniciales
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
    public void run() {
        initAudio();

        while (!closed) {
            // Detección de reconexión: si AudioCoordinator ha recibido una petición
            if (AudioCoordinator.pollReconnectionFlag()) {
                performReinitSequence();
                continue;
            }
            // Si hay una petición local, también la maneja
            if (needsReinit) {
                performReinitSequence();
                continue;
            }

            try {
                // Bloqueamos solo la condición de espera
                synchronized (this) {
                    while (!running && !closed) {
                        Utils.handleProcedure(this::wait, true);
                    }
                }
                if (closed) break;

                // --- 1. DETECTAR DESCONEXIÓN ACTIVAMENTE ---
                if (AudioEngine.isInitialized() && !isDeviceConnected()) {
                    System.out.println("Dispositivo de audio desconectado. Reconectando automáticamente...");
                    AudioCoordinator.requestReconnection("Dispositivo desconectado detectado.");
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
                // Si falla en medio de un proceso por culpa del OS, capturamos el error
                // para evitar que el Thread muera y forzamos la reconexión.
                System.err.println("Error del motor de audio (posible cambio de salida): " + e.getMessage());
                AudioCoordinator.requestReconnection("OpenALException: " + e.getMessage());
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
            alcGetIntegerv(AudioEngine.getDevice(), EXTDisconnect.ALC_CONNECTED, connected);
            return connected[0] != ALC_FALSE;
        } catch (Exception e) {
            return false;
        }
    }

    // Re-inicialización segura ejecutada desde el hilo de audio
    private void performReinitSequence() {
        System.out.println("[Audio] Re-inicialización iniciada (recuperación de dispositivo).");
        try {
            needsReinit = false;
            cleanupResources();
            AudioEngine.destroy();
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            AudioEngine.init();
            initAudio();
            initialized = true;
            System.out.println("[Audio] Re-inicialización completada (Audio thread).");
        } catch (Exception e) {
            System.err.println("[Audio] Error durante la re-inicialización: " + e.getMessage());
        }
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
        notifyAll();
    }

    private void catchInternalException() {
        int err = alGetError();
        if (err != AL_NO_ERROR) {
            throw new OpenALException(err);
        }
    }
}
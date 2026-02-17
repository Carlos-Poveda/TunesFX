package org.example.tunesfx.audio;

import org.example.tunesfx.synth.Sintetizador;
import org.example.tunesfx.utils.AudioDSP;
import org.lwjgl.openal.AL10;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.openal.AL10.*;

public class SamplePlayer {

    /**
     * Reproduce un Sample (one-shot) en un nuevo hilo.
     * Reutiliza el contexto de OpenAL que 'Audio.java' ya inicializó.
     * @param sample El sample a reproducir.
     */

    private static final Map<Sample, List<Integer>> activeSources = new ConcurrentHashMap<>();

    public static void playStep(Sample sample, StepData stepData, double stepDurationMillis) {
        if (sample == null || sample.getLength() == 0) return;

        new Thread(() -> {
            // --- LÓGICA DE DELAY ---
            if (stepData.getDelay() > 0) {
                try {
                    // Calculamos el tiempo de espera: % del paso * duración del paso
                    long delayMillis = (long) (stepData.getDelay() * stepDurationMillis);

                    // Limitamos para evitar errores si el delay es extremo
                    if (delayMillis > 0) {
                        Thread.sleep(delayMillis);
                    }
                } catch (InterruptedException e) {
                    return; // Si el hilo se interrumpe, salimos
                }
            }

            // Generar buffer y procesar DSP
            int buffer = alGenBuffers();
            short[] audioData = (stepData.getAttack() > 0 || stepData.getRelease() > 0 || stepData.getVolume() < 1.0 || stepData.getDurationFactor() < 1.0)
                    ? AudioDSP.applyEnvelope(sample.getData(), stepData.getAttack(), stepData.getRelease(), stepData.getVolume(), stepData.getDurationFactor())
                    : sample.getData();

            AL10.alBufferData(buffer, AL_FORMAT_MONO16, audioData, Sintetizador.AudioInfo.SAMPLE_RATE);

            int source = alGenSources();

            // --- REGISTRO DE LA FUENTE ---
            activeSources.computeIfAbsent(sample, k -> new CopyOnWriteArrayList<>()).add(source);

            alSource3f(source, AL_POSITION, (float) stepData.getPan(), 0f, 0f);
            alSourcef(source, AL_PITCH, stepData.getPitchMultiplier());
            alSourcei(source, AL_BUFFER, buffer);

            alSourcePlay(source);

            // Bucle de espera
            while (alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING) {
                try { Thread.sleep(10); } catch (InterruptedException ignored) {
                    // Si el hilo se interrumpe, forzamos stop
                    alSourceStop(source);
                    break;
                }
            }

            // --- LIMPIEZA ---
            alDeleteSources(source);
            alDeleteBuffers(buffer);

            // Eliminar de la lista de activos
            List<Integer> sources = activeSources.get(sample);
            if (sources != null) {
                sources.remove(Integer.valueOf(source));
            }

        }).start();
    }

    public static void stopSample(Sample sample) {
        if (sample == null) return;

        List<Integer> sources = activeSources.get(sample);
        if (sources != null) {
            for (Integer sourceId : sources) {
                // Comprobamos si la fuente sigue siendo válida
                if (alIsSource(sourceId)) {
                    alSourceStop(sourceId);
                    // Al hacer stop, el bucle 'while' del hilo de playStep
                    // terminará automáticamente y limpiará los recursos.
                }
            }
            sources.clear(); // Vaciamos la lista de esa muestra
        }
    }

    private static void catchInternalException() {
        int err = alGetError();
        if (err != AL_NO_ERROR) {
            System.err.println("Error de OpenAL en SamplePlayer: " + err);
        }
    }
}
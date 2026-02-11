package org.example.tunesfx.audio;

import org.example.tunesfx.synth.Sintetizador;
import org.example.tunesfx.utils.AudioDSP;
import org.lwjgl.openal.AL10;

import static org.lwjgl.openal.AL10.*;

public class SamplePlayer {

    /**
     * Reproduce un Sample (one-shot) en un nuevo hilo.
     * Reutiliza el contexto de OpenAL que 'Audio.java' ya inicializó.
     * @param sample El sample a reproducir.
     */
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

            int buffer = alGenBuffers();

            // --- PROCESAMIENTO DSP ---
            // Si el step tiene modificaciones, procesamos el audio.
            // Si no, usamos el original para ahorrar CPU.
            short[] audioData;
            if (stepData.getAttack() > 0 || stepData.getRelease() > 0 || stepData.getVolume() < 1.0 || stepData.getDurationFactor() < 1.0) {
                audioData = AudioDSP.applyEnvelope(
                        sample.getData(),
                        stepData.getAttack(),
                        stepData.getRelease(),
                        stepData.getVolume(),
                        stepData.getDurationFactor()
                );
            } else {
                audioData = sample.getData();
            }
            // -------------------------

            AL10.alBufferData(buffer, AL_FORMAT_MONO16, audioData, Sintetizador.AudioInfo.SAMPLE_RATE);

            int source = alGenSources();

            // --- LÓGICA DE PANNING ---
            // Movemos la fuente en el eje X según el valor de pan (-1.0 a 1.0)
            // X = Pan, Y = 0, Z = 0
            alSource3f(source, AL_POSITION, (float) stepData.getPan(), 0f, 0f);

            alSourcef(source, AL_PITCH, stepData.getPitchMultiplier());

            alSourcei(source, AL_BUFFER, buffer);
            alSourcef(source, AL_PITCH, stepData.getPitchMultiplier()); // Usar pitch del stepData

            alSourcePlay(source);

            while (alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING) {
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            }

            alDeleteSources(source);
            alDeleteBuffers(buffer);

        }).start();
    }

    /**
     * Método de ayuda copiado de tu clase Audio.java para mantener la consistencia.
     */
    private static void catchInternalException() {
        int err = alGetError();
        if (err != AL_NO_ERROR) {
            // Podríamos lanzar la excepción, pero para un "one-shot"
            // es más seguro solo imprimirla y continuar.
            System.err.println("Error de OpenAL en SamplePlayer: " + err);
            // throw new OpenALException(err); // Opcional
        }
    }
}
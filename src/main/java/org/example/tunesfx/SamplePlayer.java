package org.example.tunesfx;

import org.example.tunesfx.utils.AudioDSP;

import static org.lwjgl.openal.AL10.*;

public class SamplePlayer {

    /**
     * Reproduce un Sample (one-shot) en un nuevo hilo.
     * Reutiliza el contexto de OpenAL que 'Audio.java' ya inicializó.
     * @param sample El sample a reproducir.
     */
    public static void playStep(Sample sample, StepData stepData) {
        if (sample == null || sample.getLength() == 0) return;

        new Thread(() -> {
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

            alBufferData(buffer, AL_FORMAT_MONO16, audioData, Sintetizador.AudioInfo.SAMPLE_RATE);

            int source = alGenSources();
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
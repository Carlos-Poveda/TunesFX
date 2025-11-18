package org.example.tunesfx;

import static org.lwjgl.openal.AL10.*;

public class SamplePlayer {

    /**
     * Reproduce un Sample (one-shot) en un nuevo hilo.
     * Reutiliza el contexto de OpenAL que 'Audio.java' ya inicializó.
     * @param sample El sample a reproducir.
     */
    public static void playSample(Sample sample, float pitchMultiplier) {
        if (sample == null || sample.getLength() == 0) {
            System.err.println("SamplePlayer: Intento de reproducir sample nulo o vacío.");
            return;
        }

        // Importante: La reproducción de audio no debe bloquear el hilo de JavaFX.
        new Thread(() -> {
            // 1. Generar un búfer de OpenAL
            int buffer = alGenBuffers();
            catchInternalException();

            // 2. Cargar nuestros datos de short[] en el búfer
            alBufferData(buffer, AL_FORMAT_MONO16, sample.getData(), Sintetizador.AudioInfo.SAMPLE_RATE);
            catchInternalException();

            // 3. Generar una fuente de audio (un "reproductor")
            int source = alGenSources();
            catchInternalException();

            // 4. Asignar nuestro búfer a la fuente
            alSourcei(source, AL_BUFFER, buffer);
            catchInternalException();

            alSourcef(source, AL_PITCH, pitchMultiplier);

            // 5. ¡Reproducir!
            alSourcePlay(source);
            catchInternalException();

            // 6. Esperar a que la fuente termine de reproducir
            //    (Esto es un "poll", comprobamos el estado repetidamente)
            while (alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING) {
                try {
                    // Dormimos el hilo para no consumir 100% de CPU
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }

            // 7. Limpieza: Una vez terminado, borramos la fuente y el búfer
            alDeleteSources(source);
            alDeleteBuffers(buffer);

        }).start(); // No olvides iniciar el hilo
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
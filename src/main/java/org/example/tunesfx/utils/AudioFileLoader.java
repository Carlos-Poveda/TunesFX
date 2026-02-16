package org.example.tunesfx.utils;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioFileLoader {

    /**
     * Carga un archivo de audio y lo convierte a short[] a 44100Hz, 16-bit, Mono.
     */
    public static short[] loadSample(File file) throws IOException, UnsupportedAudioFileException {
        // 1. Obtener el stream de audio original
        AudioInputStream sourceStream = AudioSystem.getAudioInputStream(file);
        AudioFormat sourceFormat = sourceStream.getFormat();

        // 2. Definir el formato objetivo (El que usa tu motor OpenAL)
        //    44100 Hz, 16 bits, Mono (1 canal), Signed, Little Endian
        AudioFormat targetFormat = new AudioFormat(
                44100, // Sample Rate
                16,    // Sample Size in bits
                1,     // Channels (Mono)
                true,  // Signed
                false  // BigEndian? No (Little Endian para PC estándar)
        );

        // 3. Crear un stream convertido (Java Sound hace la magia de resamplear y mezclar canales)
        AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);

        // 4. Leer todos los bytes
        byte[] audioBytes = convertedStream.readAllBytes();

        // 5. Convertir bytes a shorts (Little Endian)
        //    Cada short son 2 bytes.
        short[] audioData = new short[audioBytes.length / 2];
        ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioData);

        return audioData;
    }
}
package org.example.tunesfx.utils;

import org.example.tunesfx.PlaylistItem;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

public class AudioExporter {

    // Configuración estándar de CD: 44.1kHz, 16 bit, Estéreo
    private static final float SAMPLE_RATE = 44100.0f;
    private static final int CHANNELS = 2;

    public static void exportSong(File outputFile, List<PlaylistItem> playlist, double bpm, Map<String, File> patternPaths) throws Exception {

        // 1. Calcular duración total en Muestras (Samples)
        // Buscamos el bloque que termina más tarde
        int maxBar = 0;
        for (PlaylistItem item : playlist) {
            // Asumimos que cada patrón dura 1 compás (ajusta esto si tus patrones duran más)
            int endBar = item.getStartBar() + 1;
            if (endBar > maxBar) maxBar = endBar;
        }

        // Matemáticas de tiempo:
        // Segundos por compás = (60 / BPM) * 4 (para 4/4)
        double secondsPerBar = (60.0 / bpm) * 4.0;
        double totalSeconds = maxBar * secondsPerBar;

        // El tamaño del array buffer (en frames)
        int totalFrames = (int) (totalSeconds * SAMPLE_RATE);

        // Usamos float[] para mezclar porque es más fácil sumar decimales (-1.0 a 1.0) que bytes
        // Multiplicamos por CHANNELS (2) porque es estéreo (Izquierda, Derecha, Izquierda, Derecha...)
        float[] mixBuffer = new float[totalFrames * CHANNELS];

        // 2. Mezclar (Loop principal)
        for (PlaylistItem item : playlist) {
            File sampleFile = patternPaths.get(item.getPatternName());

            if (sampleFile != null && sampleFile.exists()) {
                // Calcular en qué posición del array empieza este sonido
                double startSeconds = (item.getStartBar() - 1) * secondsPerBar;
                int startFrame = (int) (startSeconds * SAMPLE_RATE);
                int startIndex = startFrame * CHANNELS;

                // Leer el archivo de audio del patrón y sumarlo al mixBuffer
                addToMix(mixBuffer, startIndex, sampleFile);
            }
        }

        // 3. Convertir floats de vuelta a bytes (PCM 16-bit)
        byte[] outputBytes = new byte[mixBuffer.length * 2]; // *2 porque 16bit son 2 bytes
        int byteIndex = 0;

        for (float sample : mixBuffer) {
            // Limitar el volumen (Clipping) para que no distorsione si se pasa de 1.0
            float clamped = Math.max(-1.0f, Math.min(1.0f, sample));

            // Escalar a rango de short (16 bit: -32768 a 32767)
            short s = (short) (clamped * 32767);

            // Convertir short a 2 bytes (Little Endian)
            outputBytes[byteIndex++] = (byte) (s & 0xFF);
            outputBytes[byteIndex++] = (byte) ((s >> 8) & 0xFF);
        }

        // 4. Guardar archivo WAV final
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(outputBytes);
        AudioInputStream audioStream = new AudioInputStream(bais, format, totalFrames);

        AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, outputFile);
        System.out.println("Exportación completada: " + outputFile.getAbsolutePath());
    }

    private static void addToMix(float[] mixBuffer, int startIndex, File audioFile) {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat format = ais.getFormat();

            // Leemos todos los bytes del archivo
            byte[] fileBytes = ais.readAllBytes();

            // Usamos ByteBuffer para convertir los bytes a números (Short) correctamente.
            // Esto gestiona automáticamente el signo (+/-) y el orden (Little Endian).
            ByteBuffer bb = ByteBuffer.wrap(fileBytes);

            // La mayoría de WAVs son Little Endian. Si el formato dice lo contrario, cambiamos el orden.
            if (format.isBigEndian()) {
                bb.order(ByteOrder.BIG_ENDIAN);
            } else {
                bb.order(ByteOrder.LITTLE_ENDIAN);
            }

            // Determinar si es estéreo o mono para saber cuánto avanzar
            boolean isStereo = format.getChannels() == 2;

            // Recorremos el buffer de 2 en 2 bytes (porque 16 bits = 2 bytes)
            // bb.remaining() nos dice cuántos bytes quedan por leer
            int i = 0;
            while (bb.remaining() >= 2) {
                // getShort() lee 2 bytes y nos da el número correcto entre -32768 y 32767
                short sampleShort = bb.getShort();

                // Convertimos a float (-1.0 a 1.0)
                float val = sampleShort / 32768.0f;

                // OPCIONAL: Reducir un poco el volumen para evitar saturación natural
                // si sumas muchos instrumentos. (Aquí bajamos al 80%)
                val = val * 0.8f;

                // Calcular posición en el array gigante de la canción
                // Si es estéreo, los samples vienen L, R, L, R...
                // Si es mono, el sample 0 va al canal L y al R.

                int mixIndex = startIndex + i;

                if (mixIndex >= mixBuffer.length) break;

                // Sumar al canal actual
                mixBuffer[mixIndex] += val;

                // Si el archivo original es MONO, pero la mezcla es ESTÉREO,
                // tenemos que sumar este mismo valor al canal derecho (mixIndex + 1)
                if (!isStereo && (mixIndex + 1 < mixBuffer.length)) {
                    mixBuffer[mixIndex + 1] += val;
                    // Importante: En el mixBuffer avanzamos 2 posiciones (L y R)
                    // pero en el archivo original solo hemos leído 1 sample.
                    // Para compensar el índice del bucle:
                    startIndex++;
                }

                i++;
            }

        } catch (Exception e) {
            System.err.println("Error leyendo sample: " + audioFile.getName());
            e.printStackTrace();
        }
    }
}
package org.example.tunesfx.utils;

import org.example.tunesfx.ChannelRackController;
import org.example.tunesfx.ChannelRackRowController;
import org.example.tunesfx.PlaylistItem;
import org.example.tunesfx.audio.Sample;
import org.example.tunesfx.audio.StepData;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

public class AudioExporter {

    private static final float SAMPLE_RATE = 44100.0f;
    private static final int CHANNELS = 2;

    public static void exportSong(File outputFile, List<PlaylistItem> playlist, double bpm, ChannelRackController rackController) throws Exception {

        // 1. Calcular duración total
        int maxBar = 0;
        for (PlaylistItem item : playlist) {
            int endBar = item.getStartBar() + 1; // Asumimos patrones de 1 compás
            if (endBar > maxBar) maxBar = endBar;
        }

        // Matemáticas de tiempo
        double secondsPerBar = (60.0 / bpm) * 4.0;
        double secondsPerStep = secondsPerBar / 16.0; // Asumiendo 16 pasos por compás visualmente
        // Si tu Channel Rack tiene 64 pasos pero duran 4 compases, ajusta esto.

        double totalSeconds = maxBar * secondsPerBar;
        int totalFrames = (int) (totalSeconds * SAMPLE_RATE);
        float[] mixBuffer = new float[totalFrames * CHANNELS];

        // 2. MEZCLAR (Recorrer la Playlist)
        for (PlaylistItem item : playlist) {

            // Buscar la fila (Row) correspondiente al nombre del patrón
            ChannelRackRowController row = findRowByName(rackController, item.getPatternName());

            if (row != null && row.getSample() != null) {

                // Obtener los datos de audio crudos del sample (float[])
                // Esto evita leer el disco mil veces. Usamos los datos ya cargados en memoria.
                float[] sourceAudio = convertSampleToFloat(row.getSample());
                if (sourceAudio == null) continue;

                // Calcular cuándo empieza este bloque en la canción (en segundos)
                double blockStartTime = (item.getStartBar() - 1) * secondsPerBar;

                // 3. SECUENCIADOR INTERNO (Recorrer los pasos de la fila)
                // Aquí aplicamos el ritmo que dibujaste
                int stepsInRow = row.getStepCount();

                // Ajuste: Si tienes 64 pasos, asumimos que eso llena el patrón completo.
                // Si el patrón visual en la playlist es de 1 compás, pero el rack tiene 64 pasos (4 compases),
                // deberías ajustar la duración del bloque. Por simplicidad, asumiremos 1 compás = 16 pasos estándar.

                for (int i = 0; i < stepsInRow; i++) {
                    // Usamos tu método getCombinedStepData para obtener Pitch y Volumen reales
                    StepData stepData = row.getCombinedStepData(i);

                    if (stepData != null && stepData.isActive()) {

                        // Calcular el tiempo exacto de este golpe (step)
                        double stepDelay = i * ((60.0 / bpm) / 4.0); // (60/BPM)/4 = duración de semicorchea
                        double absTime = blockStartTime + stepDelay;

                        // Posición en el array gigante
                        int startFrameIndex = (int) (absTime * SAMPLE_RATE);
                        int bufferIndex = startFrameIndex * CHANNELS;

                        // Calcular factor de velocidad (Pitch)
                        // 2^(semitonos/12). Ej: +12 semitonos = velocidad x2
                        float pitchFactor = (float) Math.pow(2, stepData.getSemitoneOffset() / 12.0);

                        // Aplicar volumen
                        float volume = (float) stepData.getVolume();

                        // 4. MEZCLAR CON RESAMPLING
                        mixSampleWithResampling(mixBuffer, bufferIndex, sourceAudio, pitchFactor, volume);
                    }
                }
            }
        }

        // 5. Guardar archivo (igual que antes)
        saveToFile(outputFile, mixBuffer, totalFrames);
    }

    // Método para cambiar el tono (Resampling simple - Nearest Neighbor/Linear)
    private static void mixSampleWithResampling(float[] mixBuffer, int startIndex, float[] source, float speed, float volume) {

        // Índice decimal para recorrer el sample original
        double readIndex = 0;

        // Recorremos el buffer de destino
        for (int i = startIndex; i < mixBuffer.length; i += 2) { // +=2 porque vamos L, R

            // Si el sample original se acaba, paramos
            if (readIndex >= source.length) break;

            // Interpolación Lineal (Mejor calidad que coger el pixel más cercano)
            int indexInt = (int) readIndex;
            double frac = readIndex - indexInt;

            // Evitar desbordamiento
            if (indexInt + 1 >= source.length) break;

            // Leer valor actual y siguiente
            float s1 = source[indexInt];
            float s2 = source[indexInt + 1]; // Ojo: si source es estéreo, la lógica cambia.
            // ASUMIMOS SOURCE MONO para simplificar la explicación de pitch.
            // Si tu Sample.java guarda estéreo, habría que leer de 2 en 2.

            // Interpolamos
            float val = (float) ((s1 + (s2 - s1) * frac) * volume);

            // Mezclar en estéreo
            mixBuffer[i] += val;       // Left
            if (i + 1 < mixBuffer.length) {
                mixBuffer[i + 1] += val; // Right
            }

            // Avanzamos el cursor de lectura según la velocidad (Pitch)
            readIndex += speed;
        }
    }

    // Auxiliar: Convierte el objeto Sample (short[]) a float[] para procesar fácil
    private static float[] convertSampleToFloat(Sample sample) {
        short[] data = sample.getData(); // Asumiendo que tienes un getter getData()
        if (data == null) return null;

        float[] floatData = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            floatData[i] = data[i] / 32768.0f;
        }
        return floatData;
    }

    private static ChannelRackRowController findRowByName(ChannelRackController rack, String name) {
        // Necesitas exponer las filas desde el controlador (getRows() o similar)
        // O usar el método público que creamos antes pero modificado para retornar la fila
        return rack.findRowController(name);
    }

    private static void saveToFile(File outputFile, float[] mixBuffer, int totalFrames) throws Exception {
        byte[] outputBytes = new byte[mixBuffer.length * 2];
        int byteIndex = 0;
        for (float sample : mixBuffer) {
            float clamped = Math.max(-1.0f, Math.min(1.0f, sample * 0.8f)); // Headroom
            short s = (short) (clamped * 32767);
            outputBytes[byteIndex++] = (byte) (s & 0xFF);
            outputBytes[byteIndex++] = (byte) ((s >> 8) & 0xFF);
        }
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(outputBytes);
        AudioInputStream audioStream = new AudioInputStream(bais, format, totalFrames);
        AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, outputFile);
    }
}
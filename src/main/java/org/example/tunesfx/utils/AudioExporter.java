package org.example.tunesfx.utils;

import org.example.tunesfx.controller.ChannelRackController;
import org.example.tunesfx.controller.ChannelRackRowController;
import org.example.tunesfx.audio.PlaylistItem;
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

        // --- CALIBRACIÓN DE TIEMPO ---
        // 1. Duración de un solo tiempo (una negra)
        double secondsPerBeat = 60.0 / bpm;

        // 2. ¡NUEVA ESCALA! Tu rejilla ahora representa compases enteros (4 negras)
        double secondsPerBar = secondsPerBeat * 4.0;

        // 3. Duración de un paso (Step) del Channel Rack (1 paso = 0.25 beats)
        double secondsPerStep = secondsPerBeat / 4.0;

        // --- CÁLCULO DE DURACIÓN TOTAL ---
        int maxColumn = 0;
        for (PlaylistItem item : playlist) {
            if (item.getStartBar() > maxColumn) maxColumn = item.getStartBar();
        }
        // Añadimos un margen de 2 compases (para que las colas de sonido no se corten abruptamente)
        double totalSeconds = (maxColumn + 2) * secondsPerBar;
        int totalFrames = (int) (totalSeconds * SAMPLE_RATE);
        float[] mixBuffer = new float[totalFrames * CHANNELS];

        // --- PROCESAR PLAYLIST ---
        for (PlaylistItem item : playlist) {
            ChannelRackRowController row = rackController.findRowController(item.getPatternName());

            if (row != null && row.getSample() != null) {
                float[] sourceAudio = convertSampleToFloat(row.getSample());
                if (sourceAudio == null) continue;

                // ¡CAMBIO CLAVE AQUÍ!
                // Restamos 1 al startBar porque el Compás 1 debe empezar en el segundo 0.0
                double blockStartTime = (item.getStartBar() - 1) * secondsPerBar;

                int stepsInRow = row.getStepCount();

                for (int i = 0; i < stepsInRow; i++) {
                    StepData stepData = row.getCombinedStepData(i);

                    if (stepData != null && stepData.isActive()) {
                        // El delay del step dentro del patrón
                        double stepDelay = i * secondsPerStep;

                        // Tiempo absoluto donde debe sonar esta muestra de audio
                        double absTime = blockStartTime + stepDelay;

                        int startFrameIndex = (int) (absTime * SAMPLE_RATE);
                        int bufferIndex = startFrameIndex * CHANNELS;

                        float pitchFactor = (float) Math.pow(2, stepData.getSemitoneOffset() / 12.0);
                        float volume = (float) stepData.getVolume();

                        mixSampleWithResampling(mixBuffer, bufferIndex, sourceAudio, pitchFactor, stepData);                    }
                }
            }
        }

        saveToFile(outputFile, mixBuffer, totalFrames);
    }

    private static void mixSampleWithResampling(float[] mixBuffer, int startIndex, float[] source, float speed, StepData stepData) {
        // 1. Extraer los parámetros de tu diseño de sonido
        float volume = (float) stepData.getVolume();
        float pan = (float) stepData.getPan();
        double attack = stepData.getAttack();   // De 0.0 a 0.5
        double release = stepData.getRelease(); // De 0.0 a 0.5
        double durationFactor = stepData.getDurationFactor(); // De 0.1 a 1.0

        // 2. Lógica de Panning (Estéreo)
        // -1.0 es Izquierda 100%, 0.0 es Centro, 1.0 es Derecha 100%
        float leftPanGain = pan <= 0 ? 1.0f : 1.0f - pan;
        float rightPanGain = pan >= 0 ? 1.0f : 1.0f + pan;

        // 3. Lógica de Duración y Envolvente (Attack/Release)
        int totalSourceSamples = source.length;
        // Acortamos el sample en base a la duración elegida
        int targetSamples = (int) (totalSourceSamples * durationFactor);

        // Cuántos "píxeles" de audio tardará en hacer los fundidos
        int attackSamples = (int) (targetSamples * attack);
        int releaseSamples = (int) (targetSamples * release);

        double readIndex = 0;

        // El salto es de 2 en 2 porque el buffer final es Estéreo (i = Izquierda, i+1 = Derecha)
        for (int i = startIndex; i < mixBuffer.length; i += 2) {

            // Si llegamos a la duración recortada, dejamos de pintar el sonido
            if (readIndex >= targetSamples || i + 1 >= mixBuffer.length) {
                break;
            }

            int indexInt = (int) readIndex;
            double frac = readIndex - indexInt;

            if (indexInt + 1 >= source.length) break;

            // Leer el audio original con interpolación (Pitch)
            float s1 = source[indexInt];
            float s2 = source[indexInt + 1];
            float rawSample = (float) (s1 + (s2 - s1) * frac);

            // 4. Aplicar Envolvente Dinámica (Attack / Release)
            float envelope = 1.0f; // Por defecto el volumen está al máximo

            if (readIndex < attackSamples && attackSamples > 0) {
                // Fase de Attack: El volumen va subiendo de 0 a 1
                envelope = (float) (readIndex / attackSamples);
            } else if (readIndex > targetSamples - releaseSamples && releaseSamples > 0) {
                // Fase de Release: El volumen va bajando de 1 a 0
                envelope = (float) ((targetSamples - readIndex) / releaseSamples);
            }

            // 5. Aplicar volumen general y la envolvente al sample crudo
            float processedSample = rawSample * volume * envelope;

            // 6. Escribir en el archivo (Separando Izquierda y Derecha)
            mixBuffer[i] += processedSample * leftPanGain;       // Canal Izquierdo (L)
            mixBuffer[i + 1] += processedSample * rightPanGain;  // Canal Derecho (R)

            readIndex += speed;
        }
    }

    private static float[] convertSampleToFloat(Sample sample) {
        short[] data = sample.getData();
        if (data == null) return null;
        float[] floatData = new float[data.length];
        for (int i = 0; i < data.length; i++) floatData[i] = data[i] / 32768.0f;
        return floatData;
    }

    private static void saveToFile(File outputFile, float[] mixBuffer, int totalFrames) throws Exception {
        byte[] outputBytes = new byte[mixBuffer.length * 2];
        int byteIndex = 0;
        for (float sample : mixBuffer) {
            float clamped = Math.max(-1.0f, Math.min(1.0f, sample * 0.8f));
            short s = (short) (clamped * 32767);
            outputBytes[byteIndex++] = (byte) (s & 0xFF);
            outputBytes[byteIndex++] = (byte) ((s >> 8) & 0xFF);
        }
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);
        AudioInputStream audioStream = new AudioInputStream(new ByteArrayInputStream(outputBytes), format, totalFrames);
        AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, outputFile);
    }
}
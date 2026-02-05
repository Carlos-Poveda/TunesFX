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

        // --- CALIBRACIÓN DE TIEMPO ---
        // 1. Duración de un solo tiempo (una negra)
        double secondsPerBeat = 60.0 / bpm;

        // 2. ¿Qué representa UNA COLUMNA de tu Grid en la Playlist?
        // Si tu rejilla visual está dividida en BEATS (negras), usa: secondsPerBeat
        // Si tu rejilla está dividida en COMPASES (4 negras), usa: secondsPerBeat * 4
        // Dado tu error de 6 segundos, lo más probable es que cada columna sea un BEAT.
        double secondsPerColumn = secondsPerBeat;

        // 3. Duración de un paso (Step) del Channel Rack
        // Normalmente 16 pasos = 1 compás (4 beats). Así que 1 paso = 0.25 beats.
        double secondsPerStep = secondsPerBeat / 4.0;

        // --- CÁLCULO DE DURACIÓN TOTAL ---
        int maxColumn = 0;
        for (PlaylistItem item : playlist) {
            if (item.getStartBar() > maxColumn) maxColumn = item.getStartBar();
        }
        // Añadimos un margen de 4 columnas para que no se corte el último sonido
        double totalSeconds = (maxColumn + 4) * secondsPerColumn;
        int totalFrames = (int) (totalSeconds * SAMPLE_RATE);
        float[] mixBuffer = new float[totalFrames * CHANNELS];

        // --- PROCESAR PLAYLIST ---
        for (PlaylistItem item : playlist) {
            ChannelRackRowController row = rackController.findRowController(item.getPatternName());

            if (row != null && row.getSample() != null) {
                float[] sourceAudio = convertSampleToFloat(row.getSample());
                if (sourceAudio == null) continue;

                // TIEMPO DE INICIO: Columna actual * Duración de una columna
                double blockStartTime = item.getStartBar() * secondsPerColumn;

                int stepsInRow = row.getStepCount();

                for (int i = 0; i < stepsInRow; i++) {
                    StepData stepData = row.getCombinedStepData(i);

                    if (stepData != null && stepData.isActive()) {
                        // El delay del step dentro del patrón
                        double stepDelay = i * secondsPerStep;
                        double absTime = blockStartTime + stepDelay;

                        int startFrameIndex = (int) (absTime * SAMPLE_RATE);
                        int bufferIndex = startFrameIndex * CHANNELS;

                        float pitchFactor = (float) Math.pow(2, stepData.getSemitoneOffset() / 12.0);
                        float volume = (float) stepData.getVolume();

                        mixSampleWithResampling(mixBuffer, bufferIndex, sourceAudio, pitchFactor, volume);
                    }
                }
            }
        }

        saveToFile(outputFile, mixBuffer, totalFrames);
    }

    private static void mixSampleWithResampling(float[] mixBuffer, int startIndex, float[] source, float speed, float volume) {
        double readIndex = 0;
        for (int i = startIndex; i < mixBuffer.length; i += 2) {
            if (readIndex >= source.length || i >= mixBuffer.length) break;
            int indexInt = (int) readIndex;
            double frac = readIndex - indexInt;
            if (indexInt + 1 >= source.length) break;
            float s1 = source[indexInt];
            float s2 = source[indexInt + 1];
            float val = (float) ((s1 + (s2 - s1) * frac) * volume);
            mixBuffer[i] += val;
            if (i + 1 < mixBuffer.length) mixBuffer[i + 1] += val;
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
package org.example.tunesfx;

import org.example.tunesfx.utils.Utils;

public class Wavetable {
    // Serum usa 2048 muestras por frame, nosotros usaremos tu SIZE actual
    public static final int TABLE_SIZE = 8192;

    // Cuántas "fotos" o formas distintas tiene esta tabla
    private final int numFrames;

    // Matriz 2D: [Número de Frame][Muestras]
    private final float[][] tableData;

    private String name;

    public Wavetable(String name, int numFrames) {
        this.name = name;
        this.numFrames = numFrames;
        this.tableData = new float[numFrames][TABLE_SIZE];
    }

    /**
     * Obtiene el valor exacto interpolando entre dos frames.
     * @param index La posición en la onda (0 a TABLE_SIZE)
     * @param position La posición en la tabla (0.0 a 1.0) -> El slider "WT Pos"
     */
    public float getSample(int index, double position) {
        // Asegurar límites
        if (position < 0) position = 0;
        if (position > 1) position = 1;

        // Calcular en qué frame estamos y cuál es el siguiente
        // Ejemplo: Si hay 5 frames y pos es 0.5, estamos entre el frame 2 y 3.
        double frameIndexFloat = position * (numFrames - 1);
        int frameA = (int) frameIndexFloat;
        int frameB = Math.min(frameA + 1, numFrames - 1);

        // Cuánto mezclar (0.0 = todo frameA, 1.0 = todo frameB)
        double mix = frameIndexFloat - frameA;

        float sampleA = tableData[frameA][index];
        float sampleB = tableData[frameB][index];

        // Mezcla lineal (Crossfade)
        return (float) (sampleA * (1.0 - mix) + sampleB * mix);
    }

    // === GENERADORES DE PRESETS ===

    // Crea una tabla que hace morphing: Seno -> Triángulo -> Sierra -> Cuadrada
    public static Wavetable createBasicMorph() {
        int frames = 4;
        Wavetable wt = new Wavetable("Basic Shapes", frames);

        // Frame 0: Seno
        generateWave(wt.tableData[0], WaveType.SINE);
        // Frame 1: Triángulo
        generateWave(wt.tableData[1], WaveType.TRIANGLE);
        // Frame 2: Sierra
        generateWave(wt.tableData[2], WaveType.SAW);
        // Frame 3: Cuadrada
        generateWave(wt.tableData[3], WaveType.SQUARE);

        return wt;
    }

    // Crea una tabla que evoluciona: Seno -> FM rara -> Ruido
    public static Wavetable createAlienMorph() {
        int frames = 64; // Muchos pasos para suavidad
        Wavetable wt = new Wavetable("Alien FM", frames);

        for (int f = 0; f < frames; f++) {
            double phaseMod = (double)f / frames * 10.0; // Aumentamos la modulación
            for (int i = 0; i < TABLE_SIZE; i++) {
                double t = (double)i / TABLE_SIZE;
                // Fórmula FM simple: Seno modulado por otro seno
                wt.tableData[f][i] = (float) Math.sin(2 * Math.PI * t + Math.sin(2 * Math.PI * t * phaseMod));
            }
        }
        return wt;
    }

    // Enum interno auxiliar para generar las formas
    private enum WaveType { SINE, TRIANGLE, SAW, SQUARE }

    private static void generateWave(float[] buffer, WaveType type) {
        for (int i = 0; i < TABLE_SIZE; i++) {
            double t = (double)i / TABLE_SIZE; // 0.0 a 1.0
            double val = 0;
            switch (type) {
                case SINE: val = Math.sin(2 * Math.PI * t); break;
                case SAW: val = 2.0 * (t - 0.5); break; // Sierra simple
                case SQUARE: val = Math.sin(2 * Math.PI * t) > 0 ? 1 : -1; break;
                case TRIANGLE: val = 2.0 * Math.abs(2.0 * (t - Math.floor(t + 0.5))) - 1.0; break;
            }
            buffer[i] = (float) val;
        }
    }

    public String getName() { return name; }
}
package org.example.tunesfx;

public class OscillatorDSP {

    private Wavetable waveTable;
    private double wtPosition = 0.0;
    private int waveTableStepSize;
    private double waveTableIndex; // Cambiado a double para futura interpolación (Paso 5)
    private double keyFrequency;
    private int toneOffset;
    private int volume = 100;

    private static final int TONE_OFFSET_LIMIT = 400;

    public OscillatorDSP() {
        this.waveTable = Wavetable.createBasicMorph();
        inicializarOscilador();
    }

    private void inicializarOscilador() {
        keyFrequency = 440.0;
        toneOffset = 0;
        applyToneOffset();
    }

    // === LÓGICA DE AUDIO ===

    public double getNextSample() {
        // Casting a int para leer la tabla (mejoraremos esto en el Paso 5)
        int indexA = (int) waveTableIndex;
        int indexB = indexA + 1;

        if (indexB >= Wavetable.TABLE_SIZE) {
            indexB = 0;
        }

        double fraction = waveTableIndex - indexA;

        float valA = waveTable.getSample(indexA, fraction);
        float valB = waveTable.getSample(indexB, fraction);

        double interpolatedSample = valA + (fraction * (valB - valA));

        double finalSample = interpolatedSample * getVolumenMultiplier();

        waveTableIndex += waveTableStepSize;

        if (waveTableIndex >= Wavetable.TABLE_SIZE) {
            waveTableIndex -= Wavetable.TABLE_SIZE;
        }

        return finalSample;
    }

    public void resetPhase() {
        this.waveTableIndex = 0;
    }

    // Método auxiliar para WaveViewer
    public double[] getSampleWaveform(int numSamples) {
        double[] samples = new double[numSamples];
        // Simulamos una frecuencia visual para el dibujo
        double frequency = 1.0 / (numSamples / (double)Sintetizador.AudioInfo.SAMPLE_RATE) * 3.0;
        double index = 0;
        int stepSize = (int)(Wavetable.TABLE_SIZE * (frequency * Math.pow(2, getToneOffset())) / Sintetizador.AudioInfo.SAMPLE_RATE);

        for (int i = 0; i < numSamples; i++) {
            samples[i] = waveTable.getSamples()[(int)index] * getVolumenMultiplier();
            index = (index + stepSize) % Wavetable.TABLE_SIZE;
        }
        return samples;
    }

    // === SETTERS Y CÁLCULOS ===

    public void setWaveTable(Wavetable waveTable) {
        this.waveTable = waveTable;
    }

    public void setKeyFrequency(double frequency) {
        keyFrequency = frequency;
        applyToneOffset();
    }

    public void setToneOffset(int offset) {
        // Limitamos el rango aquí por seguridad
        if (offset > TONE_OFFSET_LIMIT) offset = TONE_OFFSET_LIMIT;
        if (offset < -TONE_OFFSET_LIMIT) offset = -TONE_OFFSET_LIMIT;
        this.toneOffset = offset;
        applyToneOffset();
    }

    public int getToneOffsetInt() {
        return toneOffset;
    }

    public void setVolume(int volume) {
        if (volume > 100) volume = 100;
        if (volume < 0) volume = 0;
        this.volume = volume; //m
    }

    public int getVolume() {
        return volume;
    }

    private void applyToneOffset() {
        // Recalculamos cuánto debemos saltar en la tabla por cada sample
        double step = (Wavetable.TABLE_SIZE * (keyFrequency * Math.pow(2, getToneOffset())) / Sintetizador.AudioInfo.SAMPLE_RATE);
        this.waveTableStepSize = (int) step;
        // Nota: Al castear a int perdemos precisión (pitch drift), arreglaremos esto en el Paso 5.
    }

    private double getVolumenMultiplier() {
        return volume / 100.0;
    }

    private double getToneOffset() {
        return toneOffset / 100d;
    }
}
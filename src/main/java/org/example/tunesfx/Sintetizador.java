package org.example.tunesfx;

import org.example.tunesfx.utils.Utils;
import java.util.HashMap;

public class Sintetizador {

    private static final HashMap<Character,Double> KEY_FREQUENCIES = new HashMap<>();
    private boolean shouldGenerate;
    private static final int NUM_OSCILLATORS = 5;

    private final Audio hiloAudio = new Audio(() -> {
        if (!shouldGenerate) {
            return null;
        }
        short[] s = new short[Audio.BUFFER_SIZE];
        for (int i = 0; i < Audio.BUFFER_SIZE; i++) {
            double d = nextSample();
            s[i] = (short) (Short.MAX_VALUE * d);
        }
        return s;
    });

    static {
        final char[] PIANO_KEYS = "-<zsxdcvgbhnjmq2w3er5t6y7ui9o0p".toCharArray();
        final int STARTING_NOTE = 46;
        for (int i = 0; i < PIANO_KEYS.length; i++) {
            int currentNote = STARTING_NOTE + i;
            KEY_FREQUENCIES.put(PIANO_KEYS[i], Utils.getKeyFrequency(currentNote));
        }
    }

    private Oscilator[] oscillators;
    private WaveViewer waveViewer;

    private Runnable updateCallback;

    // --- NUEVOS CAMPOS PARA FILTRO ---
    private Filter filter;
    private boolean filterEnabled = true;
    private double filterCutoff = 1000.0;
    private double filterResonance = 1.0;

    public Sintetizador(Oscilator[] oscillators, WaveViewer waveViewer) {
        this.oscillators = oscillators;
        this.waveViewer = waveViewer;

        // Configura los osciladores en el waveViewer
        this.waveViewer.setOscillators(this.oscillators);

        // Inicializar filtro
        this.filter = new Filter(AudioInfo.SAMPLE_RATE);
        updateFilter();
    }

    public void updateWaveviewer() {
        if (updateCallback != null) {
            updateCallback.run();
        }
    }

    public void setUpdateCallback(Runnable callback) {
        this.updateCallback = callback;
    }

    // --- NUEVOS MÉTODOS PARA CONTROLAR EL FILTRO ---
    public void setFilterEnabled(boolean enabled) {
        this.filterEnabled = enabled;
        if (enabled) {
            filter.setType(Filter.FilterType.LOW_PASS);
        } else {
            filter.setType(Filter.FilterType.OFF);
        }
    }

    public void setFilterType(Filter.FilterType type) {
        filter.setType(type);
    }

    public void setFilterCutoff(double cutoff) {
        this.filterCutoff = cutoff;
        updateFilter();
    }

    public void setFilterResonance(double resonance) {
        this.filterResonance = resonance;
        updateFilter();
    }

    private void updateFilter() {
        filter.setCutoff(filterCutoff);
        filter.setResonance(filterResonance);
    }

    // --- Métodos de lógica de audio ---

    public void setFrequency(double frequency) {
        for (Oscilator osc : oscillators) {
            osc.setKeyFrequency(frequency);
        }
    }

    public double nextSample() {
        double totalSample = 0;
        for (Oscilator osc : oscillators) {
            totalSample += osc.getNextSample();
        }
        double mixedSample = totalSample / NUM_OSCILLATORS;

        // Aplicar filtro si está habilitado
        return filter.process(mixedSample);
    }

    /**
     * Genera un bloque de audio (sample)
     * basado en la configuración actual del oscilador.
     * @param numSamples La longitud del sample (ej. 44100 para 1 segundo)
     * @return Un array de shorts con los datos del sample.
     */
    public short[] generateSample(int numSamples) {
        // 1. Reiniciar la fase de todos los osciladores
        for (Oscilator osc : oscillators) {
            osc.resetPhase();
        }

        // 2. Reiniciar el filtro para un sample limpio
        filter.reset();

        // 3. Crear el array del sample
        short[] s = new short[numSamples];

        // 4. Llenar el array
        for (int i = 0; i < numSamples; i++) {
            double d = nextSample(); // Obtener el siguiente valor de la mezcla (ya incluye filtro)
            s[i] = (short) (Short.MAX_VALUE * d);
        }

        // 5. Devolver los datos
        return s;
    }

    public void onKeyPressed(char keyChar) {
        if (!KEY_FREQUENCIES.containsKey(keyChar)) {
            return;
        }
        if (hiloAudio.isInitialized() && !hiloAudio.isRunning()) {
            setFrequency(KEY_FREQUENCIES.get(keyChar));
            shouldGenerate = true;
            hiloAudio.triggerPlayBack();
        }
    }

    public void onKeyReleased() {
        shouldGenerate = false;
    }

    public void shutdownAudio() {
        hiloAudio.close();
    }

    // --- Getters para que la clase Main obtenga los nodos de UI ---

    public Oscilator[] getOscillatorsFX() {
        return oscillators;
    }

    public WaveViewer getWaveViewerFX() {
        return waveViewer;
    }

    // --- Clase interna de info (sin cambios) ---
    public static class AudioInfo {
        public static final int SAMPLE_RATE = 44100;
    }
}
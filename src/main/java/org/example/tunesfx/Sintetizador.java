package org.example.tunesfx;

import org.example.tunesfx.utils.Utils;
import java.util.HashMap;

public class Sintetizador {

    private static final HashMap<Character,Double> KEY_FREQUENCIES = new HashMap<>();
    private boolean shouldGenerate;
    private static final int NUM_OSCILLATORS = 5;

    private Filter filtro;
    private boolean filtroActivado = true;
    private LFO lfo;
    private boolean lfoActivado = false;

    private static final double LFO_CUTOFF_RANGE = 3000.0;
    private static final double LFO_RESONANCE_RANGE = 0.5;
    private static final double LFO_VOLUME_RANGE = 0.5;
    private static final double LFO_PTICH_RANGE = 200.0;

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

    public Sintetizador(Oscilator[] oscillators, WaveViewer waveViewer) {
        this.oscillators = oscillators;
        this.waveViewer = waveViewer;

        this.filtro = new Filter();

        this.lfo = new LFO();

        // Configura los osciladores en el waveViewer
        this.waveViewer.setOscillators(this.oscillators);
    }

    public void updateWaveviewer() {
        if (updateCallback != null) {
            updateCallback.run();
        }
    }

    public void setUpdateCallback(Runnable callback) {
        this.updateCallback = callback;
    }

    // --- Métodos de lógica de audio ---

    public void setFrequency(double frequency) {
        for (Oscilator osc : oscillators) {
            osc.setKeyFrequency(frequency);
        }
    }

    public double nextSample() {
        if (lfoActivado && lfo.getTarget() != LFO.Target.NONE) {
            aplicarModulacionLFO();
        }

        double totalSample = 0;
        for (Oscilator osc : oscillators) {
            totalSample += osc.getNextSample();
        }
        double muestraMezclada = totalSample / NUM_OSCILLATORS;

        if (filtroActivado) {
            muestraMezclada = filtro.procesar(muestraMezclada);
        }
        return muestraMezclada;
    }

    private void aplicarModulacionLFO() {
        double modValue = lfo.getModulationValue();

        switch (lfo.getTarget()) {
            case FILTER_CUTOFF:
                double baseCutoff = filtro.getFrecuenciaCorte();
                double modulatedCutoff = baseCutoff + (modValue * LFO_CUTOFF_RANGE);
                // Aplicar límites
                modulatedCutoff = Math.max(20, Math.min(20000, modulatedCutoff));
                filtro.setFrecuenciaCorte(modulatedCutoff);
                break;

            case FILTER_RESONANCE:
                double baseResonance = filtro.getResonancia();
                double modulatedResonance = baseResonance + (modValue * LFO_RESONANCE_RANGE);
                modulatedResonance = Math.max(0.1, Math.min(1.0, modulatedResonance));
                filtro.setResonancia(modulatedResonance);
                break;

            case OSC_VOLUME:
                // Aquí podrías modificar el volumen de los osciladores
                // Por ahora lo dejamos como placeholder
                break;

            case PITCH:
                // Aquí podrías modificar el pitch de los osciladores
                // Por ahora lo dejamos como placeholder
                break;
        }
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

        // 2. Crear el array del sample
        short[] s = new short[numSamples];

        // 3. Llenar el array
        for (int i = 0; i < numSamples; i++) {
            double d = nextSample(); // Obtener el siguiente valor de la mezcla
            s[i] = (short) (Short.MAX_VALUE * d);
        }

        // 4. Devolver los datos
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

    public void setFiltroActivado(boolean activado) {
        this.filtroActivado = activado;
        if (!activado) {
            filtro.reset();
        }
    }

    public void setTipoFiltro(Filter.Tipo tipo) {
        filtro.setTipo(tipo);
    }

    public void setFrecuenciaCorteFiltro(double frecuencia) {
        filtro.setFrecuenciaCorte(frecuencia);
    }

    public void setResonanciaFiltro(double resonancia) {
        filtro.setResonancia(resonancia);
    }

    public boolean isFiltroActivado() {
        return filtroActivado;
    }

    public Filter.Tipo getTipoFiltro() {
        return filtro.getTipo();
    }

    public double getFrecuenciaCorteFiltro() {
        return filtro.getFrecuenciaCorte();
    }

    public double getResonanciaFiltro() {
        return filtro.getResonancia();
    }

    // --- NUEVOS MÉTODOS PARA CONTROLAR EL LFO ---

    public void setLFOActivado(boolean activado) {
        this.lfoActivado = activado;
        if (!activado) {
            lfo.reset();
        }
    }

    public void setLFWaveform(LFO.Waveform waveform) {
        lfo.setWaveform(waveform);
    }

    public void setLFORate(double rate) {
        lfo.setRate(rate);
    }

    public void setLFOAmount(double amount) {
        lfo.setAmount(amount);
    }

    public void setLFOTarget(LFO.Target target) {
        lfo.setTarget(target);
    }

    public boolean isLFOActivado() {
        return lfoActivado;
    }

    public LFO.Waveform getLFWaveform() {
        return lfo.getWaveform();
    }

    public double getLFORate() {
        return lfo.getRate();
    }

    public double getLFOAmount() {
        return lfo.getAmount();
    }

    public LFO.Target getLFOTarget() {
        return lfo.getTarget();
    }

    // --- Clase interna de info (sin cambios) ---
    public static class AudioInfo {
        public static final int SAMPLE_RATE = 44100;
    }
}
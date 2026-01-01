package org.example.tunesfx;

import org.example.tunesfx.audio.Audio;
import org.example.tunesfx.utils.LFO;
import org.example.tunesfx.utils.Utils;

import java.util.HashMap;

public class Sintetizador {

    private static final HashMap<Character,Double> KEY_FREQUENCIES = new HashMap<>();
    private boolean shouldGenerate;
    private static final int NUM_OSCILLATORS = 5;

    // --- Componentes ---
    private Oscilator[] oscillators;
    private WaveViewer waveViewer;
    private Filter filtro;
    private LFO lfo;

    // --- Estados ---
    private boolean filtroActivado = false; // Por defecto false para coincidir con UI
    private boolean lfoActivado = false;

    // --- Variables para Modulación Estable (Evitar drift) ---
    private double baseFilterCutoff = 1000.0;
    private double baseFilterResonance = 0.5;
    private double lfoVolumeGain = 1.0; // Multiplicador de volumen (1.0 = normal)

    // --- Rangos de Modulación ---
    private static final double LFO_CUTOFF_RANGE = 3000.0;
    private static final double LFO_RESONANCE_RANGE = 0.5;
    private static final double LFO_VOLUME_RANGE = 0.8; // Modula el volumen un 80%

    // --- OPTIMIZACIÓN: Control Rate ---
    // Actualizamos parámetros de LFO/Filtro cada 64 muestras (aprox 1.4ms)
    // para no sobrecargar la CPU recalculando senos/cosenos 44100 veces/seg.
    private int controlCounter = 0;
    private static final int CONTROL_RATE_SAMPLES = 64;

    private Runnable updateCallback;

    // --- Hilo de Audio ---
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

    public Sintetizador(Oscilator[] oscillators, WaveViewer waveViewer) {
        this.oscillators = oscillators;
        this.waveViewer = waveViewer;

        this.filtro = new Filter();
        this.lfo = new LFO();

        // Configura los osciladores en el waveViewer
        this.waveViewer.setOscillators(this.oscillators);
    }

    // --- Callback UI ---
    public void updateWaveviewer() {
        if (updateCallback != null) {
            updateCallback.run();
        }
    }

    public void setUpdateCallback(Runnable callback) {
        this.updateCallback = callback;
    }

    // --- LÓGICA DE AUDIO PRINCIPAL ---

    public void setFrequency(double frequency) {
        for (Oscilator osc : oscillators) {
            osc.setKeyFrequency(frequency);
        }
    }

    public double nextSample() {
        // 1. OPTIMIZACIÓN: Actualizar LFO y parámetros del filtro solo cada X muestras (Control Rate)
        if (controlCounter++ >= CONTROL_RATE_SAMPLES) {
            controlCounter = 0;
            if (lfoActivado && lfo.getTarget() != LFO.Target.NONE) {
                aplicarModulacionLFO();
            } else {
                // Si el LFO se apaga, asegurarnos de restaurar el volumen a 1.0
                lfoVolumeGain = 1.0;
            }
        }

        // 2. Sumar osciladores
        double totalSample = 0;
        for (Oscilator osc : oscillators) {
            totalSample += osc.getNextSample();
        }

        // 3. Mezclar
        double muestraMezclada = totalSample / NUM_OSCILLATORS;

        // 4. Aplicar modulación de volumen (Tremolo) si existe
        if (lfoActivado && lfo.getTarget() == LFO.Target.OSC_VOLUME) {
            muestraMezclada *= lfoVolumeGain;
        }

        // 5. Aplicar Filtro (El procesamiento de audio es por muestra, pero los coeficientes se actualizaron arriba)
        if (filtroActivado) {
            muestraMezclada = filtro.procesar(muestraMezclada);
        }

        return muestraMezclada;
    }

    private void aplicarModulacionLFO() {
        // Obtenemos un valor entre -Amount y +Amount
        double modValue = lfo.getModulationValue();

        switch (lfo.getTarget()) {
            case FILTER_CUTOFF:
                // Modulamos sobre la base establecida por el slider
                double modulatedCutoff = baseFilterCutoff + (modValue * LFO_CUTOFF_RANGE);
                modulatedCutoff = Math.max(20, Math.min(20000, modulatedCutoff));
                filtro.setFrecuenciaCorte(modulatedCutoff);
                break;

            case FILTER_RESONANCE:
                // Modulamos sobre la base establecida por el slider
                double modulatedResonance = baseFilterResonance + (modValue * LFO_RESONANCE_RANGE);
                modulatedResonance = Math.max(0.1, Math.min(1.0, modulatedResonance));
                filtro.setResonancia(modulatedResonance);
                break;

            case OSC_VOLUME:
                // Efecto Tremolo: oscila el volumen alrededor de 1.0
                // (1.0 - rango) a 1.0
                // Hacemos que sea unipolar (0 a 1) o bipolar dependiendo del gusto.
                // Aquí simplemente restamos ganancia según el LFO.
                double volMod = (modValue * LFO_VOLUME_RANGE);
                // Aseguramos que el volumen esté entre 0.0 y 1.0
                lfoVolumeGain = Math.max(0.0, Math.min(1.0, 1.0 - Math.abs(volMod)));
                break;

            case PITCH:
                // Placeholder: Requeriría cambiar setFrequency en tiempo real en los osciladores.
                // Podrías implementarlo llamando a setFrequency(baseFreq + mod)
                break;

            case NONE:
                break;
        }
    }

    /**
     * Genera un bloque de audio para guardar (WAV)
     */
    public short[] generateSample(int numSamples) {
        for (Oscilator osc : oscillators) {
            osc.resetPhase();
        }
        // Reseteamos filtro y LFO para que la grabación empiece limpia
        filtro.reset();
        lfo.reset();

        short[] s = new short[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double d = nextSample();
            s[i] = (short) (Short.MAX_VALUE * d);
        }
        return s;
    }

    // --- MANEJO DE TECLADO ---

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

    // --- GETTERS & SETTERS (Con lógica para valores base) ---

    public Oscilator[] getOscillatorsFX() { return oscillators; }
    public WaveViewer getWaveViewerFX() { return waveViewer; }

    // FILTRO
    public void setFiltroActivado(boolean activado) {
        this.filtroActivado = activado;
        if (!activado) filtro.reset();
    }
    public boolean isFiltroActivado() { return filtroActivado; }

    public void setTipoFiltro(Filter.Tipo tipo) { filtro.setTipo(tipo); }
    public Filter.Tipo getTipoFiltro() { return filtro.getTipo(); }

    public void setFrecuenciaCorteFiltro(double frecuencia) {
        this.baseFilterCutoff = frecuencia; // Guardamos la base
        // Si el LFO no está controlando esto, actualizamos el filtro directamente
        if (!lfoActivado || lfo.getTarget() != LFO.Target.FILTER_CUTOFF) {
            filtro.setFrecuenciaCorte(frecuencia);
        }
    }
    public double getFrecuenciaCorteFiltro() { return baseFilterCutoff; } // UI lee la base

    public void setResonanciaFiltro(double resonancia) {
        this.baseFilterResonance = resonancia; // Guardamos la base
        if (!lfoActivado || lfo.getTarget() != LFO.Target.FILTER_RESONANCE) {
            filtro.setResonancia(resonancia);
        }
    }
    public double getResonanciaFiltro() { return baseFilterResonance; } // UI lee la base

    // LFO
    public void setLFOActivado(boolean activado) {
        this.lfoActivado = activado;
        if (!activado) {
            lfo.reset();
            // Restaurar valores base al apagar LFO
            filtro.setFrecuenciaCorte(baseFilterCutoff);
            filtro.setResonancia(baseFilterResonance);
            lfoVolumeGain = 1.0;
        }
    }
    public boolean isLFOActivado() { return lfoActivado; }

    public void setLFWaveform(LFO.Waveform waveform) { lfo.setWaveform(waveform); }
    public LFO.Waveform getLFWaveform() { return lfo.getWaveform(); }

    public void setLFORate(double rate) { lfo.setRate(rate); }
    public double getLFORate() { return lfo.getRate(); }

    public void setLFOAmount(double amount) { lfo.setAmount(amount); }
    public double getLFOAmount() { return lfo.getAmount(); }

    public void setLFOTarget(LFO.Target target) {
        // Si cambiamos de target, restauramos los valores del target anterior
        if (lfoActivado) {
            filtro.setFrecuenciaCorte(baseFilterCutoff);
            filtro.setResonancia(baseFilterResonance);
            lfoVolumeGain = 1.0;
        }
        lfo.setTarget(target);
    }
    public LFO.Target getLFOTarget() { return lfo.getTarget(); }

    // --- Info interna ---
    public static class AudioInfo {
        public static final int SAMPLE_RATE = 44100;
    }
}
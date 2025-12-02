package org.example.tunesfx;

import org.example.tunesfx.utils.Utils;
import java.util.HashMap;

public class Sintetizador {

    private static final HashMap<Character, Double> KEY_FREQUENCIES = new HashMap<>();
    private boolean shouldGenerate;
    private static final int NUM_OSCILLATORS = 5;
    private int controlCounter = 0;
    private static final int CONTROL_RATE = 32;
    private double lastModulateCutoff = 12000.0;



    // === OPTIMIZACIÓN (Paso 1): Buffer reutilizable para evitar Garbage Collection ===
    private final short[] bufferCache = new short[Audio.BUFFER_SIZE];

    static {
        // Mapeo de teclas del teclado a notas musicales
        final char[] PIANO_KEYS = "-<zsxdcvgbhnjmq2w3er5t6y7ui9o0p".toCharArray();
        final int STARTING_NOTE = 46;
        for (int i = 0; i < PIANO_KEYS.length; i++) {
            int currentNote = STARTING_NOTE + i;
            KEY_FREQUENCIES.put(PIANO_KEYS[i], Utils.getKeyFrequency(currentNote));
        }
    }

    // Componentes del Sintetizador
    private Oscilator[] oscillators;
    private WaveViewer waveViewer;
    private Runnable updateCallback;

    // Filtro
    private Filter filter;
    private boolean filterEnabled = true;
    private double filterCutoff = 12000.0;
    private double filterResonance = 0.7;
    private Filter.FilterType defaultFilterType = Filter.FilterType.LOW_PASS;

    // LFO y modulación
    private LFO lfo1;
    private double lfo1ToFilter = 0.0;
    private double lfo1ToPitch = 0.0;

    // Envolvente de modulación
    private Envelope modEnvelope;
    private double modEnvToFilter = 0.0;

    // Efectos
    private EffectsProcessor effectsProcessor;
    private double effectsMix = 0.0;

    // Glide (Portamento)
    private double glideTime = 0.0;
    private double currentFrequency = 440.0;
    private double targetFrequency = 440.0;

    // Unison (Voces múltiples desafinadas)
    private int unisonVoices = 1;
    private double unisonDetune = 0.0;
    private double unisonSpread = 0.0;
    private double[] unisonDetuneValues;

    public Sintetizador(Oscilator[] oscillators, WaveViewer waveViewer) {
        this.oscillators = oscillators;
        this.waveViewer = waveViewer;
        this.waveViewer.setOscillators(this.oscillators);

        // Inicializar filtro
        this.filter = new Filter(AudioInfo.SAMPLE_RATE);
        this.filter.setType(defaultFilterType);
        updateFilter();

        // Inicializar LFO
        this.lfo1 = new LFO(AudioInfo.SAMPLE_RATE);

        // Inicializar envolvente de modulación
        this.modEnvelope = new Envelope(AudioInfo.SAMPLE_RATE);

        // Inicializar efectos
        this.effectsProcessor = new EffectsProcessor(AudioInfo.SAMPLE_RATE);

        // Inicializar unison
        updateUnisonDetune();

        // === INTEGRACIÓN AUDIO (Paso 3) con OPTIMIZACIÓN (Paso 1) ===
        // Iniciamos el sistema de audio pasando la lógica de generación
        Audio.startAudioSystem(() -> {
            if (!shouldGenerate) {
                return null;
            }

            // Rellenamos el buffer reutilizable (Zero Allocation)
            for (int i = 0; i < Audio.BUFFER_SIZE; i++) {
                double d = nextSample();

                // Clipping (Limitador) para evitar distorsión digital si sumamos > 1.0
                if (d > 1.0) d = 1.0;
                if (d < -1.0) d = -1.0;

                bufferCache[i] = (short) (Short.MAX_VALUE * d);
            }
            // Devolvemos siempre la misma referencia con datos nuevos
            return bufferCache;
        });
    }

    public void updateWaveviewer() {
        if (updateCallback != null) {
            updateCallback.run();
        }
    }

    public void setUpdateCallback(Runnable callback) {
        this.updateCallback = callback;
    }

    // ================== SECCIÓN DE FILTRO ==================
    public void setFilterEnabled(boolean enabled) {
        this.filterEnabled = enabled;
        if (enabled) {
            filter.setType(defaultFilterType);
        } else {
            filter.setType(Filter.FilterType.OFF);
        }
    }

    public void setFilterType(Filter.FilterType type) {
        this.defaultFilterType = type;
        if (filterEnabled) {
            filter.setType(type);
        }
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

    // ================== SECCIÓN DE LFO ==================
    public void setLFO1Frequency(double freq) {
        lfo1.setFrequency(freq);
    }

    public void setLFO1Amplitude(double amp) {
        lfo1.setAmplitude(amp);
    }

    public void setLFO1Waveform(LFO.Waveform wave) {
        lfo1.setWaveform(wave);
    }

    public void setLFO1ToFilter(double amount) {
        this.lfo1ToFilter = amount;
    }

    public void setLFO1ToPitch(double amount) {
        this.lfo1ToPitch = amount;
    }

    // ================== SECCIÓN DE ENVOLVENTE ==================
    public void setModEnvAttack(double attack) {
        modEnvelope.setAttackTime(attack);
    }

    public void setModEnvDecay(double decay) {
        modEnvelope.setDecayTime(decay);
    }

    public void setModEnvSustain(double sustain) {
        modEnvelope.setSustainLevel(sustain);
    }

    public void setModEnvRelease(double release) {
        modEnvelope.setReleaseTime(release);
    }

    public void setModEnvToFilter(double amount) {
        this.modEnvToFilter = amount;
    }

    public void triggerModEnv() {
        modEnvelope.trigger();
    }

    public void releaseModEnv() {
        modEnvelope.release();
    }

    // ================== SECCIÓN DE EFECTOS ==================
    public void setReverbLevel(double level) {
        effectsProcessor.setReverbLevel(level);
    }

    public void setDelayLevel(double level) {
        effectsProcessor.setDelayLevel(level);
    }

    public void setDelayFeedback(double feedback) {
        effectsProcessor.setDelayFeedback(feedback);
    }

    public void setDelayTime(double time) {
        effectsProcessor.setDelayTime(time);
    }

    public void setEffectsMix(double mix) {
        this.effectsMix = mix;
        effectsProcessor.setMix(mix);
    }

    // ================== SECCIÓN GLOBAL / UNISON ==================
    public void setGlideTime(double time) {
        this.glideTime = time;
    }

    public void setUnisonVoices(int voices) {
        this.unisonVoices = Math.max(1, Math.min(8, voices));
        updateUnisonDetune();
    }

    public void setUnisonDetune(double detune) {
        this.unisonDetune = detune;
        updateUnisonDetune();
    }

    public void setUnisonSpread(double spread) {
        this.unisonSpread = spread;
        updateUnisonDetune();
    }

    private void updateUnisonDetune() {
        unisonDetuneValues = new double[unisonVoices];
        for (int i = 0; i < unisonVoices; i++) {
            double detuneCents = unisonDetune * (i - (unisonVoices - 1) / 2.0);
            double spreadFactor = unisonSpread * (i - (unisonVoices - 1) / 2.0);
            unisonDetuneValues[i] = detuneCents + spreadFactor;
        }
    }

    // ================== LÓGICA DE AUDIO (DSP) ==================

    public void setFrequency(double frequency) {
        if (glideTime > 0 && currentFrequency != frequency) {
            targetFrequency = frequency;
        } else {
            currentFrequency = frequency;
            targetFrequency = frequency;
            applyFrequencyToOscillators();
        }
    }

    private void applyFrequencyToOscillators() {
        for (int i = 0; i < Math.min(oscillators.length, unisonVoices); i++) {
            double detuneFactor = Math.pow(2, unisonDetuneValues[i] / 1200.0);
            // NOTA: oscillators[i] es la clase Oscilator (Wrapper), que delega al DSP
            oscillators[i].setKeyFrequency(currentFrequency * detuneFactor);
        }

        // Silenciar osciladores que sobran (si bajamos las voces unison)
        for (int i = unisonVoices; i < oscillators.length; i++) {
            oscillators[i].setKeyFrequency(0);
        }
    }

    /**
     * Calcula la siguiente muestra de audio combinando todo.
     * @return Valor double entre -1.0 y 1.0 (aprox)
     */
    public double nextSample() {
        // 1. Procesar Glide (suavizado de pitch)
        if (glideTime > 0 && currentFrequency != targetFrequency) {
            double diff = targetFrequency - currentFrequency;
            double maxChange = (targetFrequency * glideTime) / AudioInfo.SAMPLE_RATE;
            if (Math.abs(diff) > maxChange) {
                currentFrequency += Math.signum(diff) * maxChange;
            } else {
                currentFrequency = targetFrequency;
            }
            applyFrequencyToOscillators();
        }

        // 2. Calcular valores de Modulación (LFO y Envolvente)
        double lfo1Value = lfo1.getNextSample();
        double modEnvValue = modEnvelope.nextSample();

        // 3. Modular Cutoff del Filtro
        controlCounter++;
        if (controlCounter >= CONTROL_RATE) {
            controlCounter = 0; //Reiniciar contador

            if (modEnvToFilter > 0 || lfo1ToFilter > 0) {
                double modulatedCutoff = filterCutoff;

                // Modulación por Envolvente
                if (modEnvToFilter > 0) {
                    modulatedCutoff += modEnvValue * modEnvToFilter * 5000;
                }
                // Modulación por LFO
                if (lfo1ToFilter > 0) {
                    modulatedCutoff += lfo1Value * lfo1ToFilter * 5000;
                }

                modulatedCutoff = Math.max(20, Math.min(22000, modulatedCutoff));
                if (Math.abs(modulatedCutoff - lastModulateCutoff) > 0.1) {
                    filter.setCutoff(modulatedCutoff);
                    lastModulateCutoff = modulatedCutoff;
                }
            }
        }

        // 4. Modular Pitch de Osciladores
        if (lfo1ToPitch > 0) {
            double pitchModulation = lfo1Value * lfo1ToPitch * 2.0; // +/- 2 semitonos
            for (int i = 0; i < Math.min(oscillators.length, unisonVoices); i++) {
                double baseFrequency = 440.0; // Solo referencia para cálculo relativo
                double detuneFactor = Math.pow(2, unisonDetuneValues[i] / 1200.0);
                double modulatedFrequency = baseFrequency * Math.pow(2, pitchModulation / 12.0) * detuneFactor;
                // Ajustamos sobre la frecuencia actual que se está tocando
                oscillators[i].setKeyFrequency(currentFrequency * (modulatedFrequency/baseFrequency));
            }
        }

        // 5. Sumar Osciladores
        double totalSample = 0;
        int activeOscillators = 0;

        for (int i = 0; i < Math.min(oscillators.length, unisonVoices); i++) {
            totalSample += oscillators[i].getNextSample();
            activeOscillators++;
        }

        double mixedSample = activeOscillators > 0 ? totalSample / activeOscillators : 0;

        // 6. Aplicar Filtro
        double filteredSample = filter.process(mixedSample);

        // 7. Aplicar Efectos (Delay / Reverb)
        double effectedSample = effectsProcessor.process(filteredSample);

        // 8. Mezcla Final (Dry/Wet)
        return (filteredSample * (1.0 - effectsMix)) + (effectedSample * effectsMix);
    }

    /**
     * Genera un sample "offline" (para guardar en archivo/banco)
     */
    public short[] generateSample(int numSamples) {
        // Resetear fases para que el sample empiece limpio
        for (Oscilator osc : oscillators) {
            osc.resetPhase();
        }
        filter.reset();
        lfo1.reset();
        modEnvelope.trigger();

        // Nota: Usamos el effectsProcessor actual.
        // No creamos uno nuevo para no perder configuraciones ni romper referencias.

        short[] s = new short[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double d = nextSample();

            // Protección de clipping
            if (d > 1.0) d = 1.0;
            if (d < -1.0) d = -1.0;

            s[i] = (short) (Short.MAX_VALUE * d);
        }
        return s;
    }

    // ================== EVENTOS DE TECLADO ==================
    public void onKeyPressed(char keyChar) {
        if (!KEY_FREQUENCIES.containsKey(keyChar)) {
            return;
        }

        // Lógica actualizada para Audio Singleton (Paso 3)
        Audio audio = Audio.getInstance();

        if (audio.isInitialized()) {
            // Si estaba en silencio, arrancamos nota nueva
            if (!audio.isRunning()) {
                setFrequency(KEY_FREQUENCIES.get(keyChar));
                shouldGenerate = true;
                audio.triggerPlayBack();
                triggerModEnv();
            } else {
                // Si ya sonaba (Legato), solo cambiamos nota y reactivamos envolvente
                setFrequency(KEY_FREQUENCIES.get(keyChar));
                triggerModEnv();
            }
        }
    }

    public void onKeyReleased() {
        shouldGenerate = false;
        releaseModEnv();
    }

    public void shutdownAudio() {
        Audio.shutdownOpenAL();
    }

    public Oscilator[] getOscillatorsFX() {
        return oscillators;
    }

    public WaveViewer getWaveViewerFX() {
        return waveViewer;
    }

    public static class AudioInfo {
        public static final int SAMPLE_RATE = 44100;
    }
}
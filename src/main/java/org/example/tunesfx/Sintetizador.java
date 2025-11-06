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

    // Glide
    private double glideTime = 0.0;
    private double currentFrequency = 440.0;
    private double targetFrequency = 440.0;

    // Unison
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
    }

    public void updateWaveviewer() {
        if (updateCallback != null) {
            updateCallback.run();
        }
    }

    public void setUpdateCallback(Runnable callback) {
        this.updateCallback = callback;
    }

    // === FILTRO ===
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

    // === LFO Y MODULACIÓN ===
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

    // === ENVOLVENTE DE MODULACIÓN ===
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

    // === EFECTOS ===
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

    // === GLIDE ===
    public void setGlideTime(double time) {
        this.glideTime = time;
    }

    // === UNISON ===
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

    // === MÉTODOS DE AUDIO PRINCIPALES ===
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
            oscillators[i].setKeyFrequency(currentFrequency * detuneFactor);
        }

        // Silenciar osciladores no usados en unison
        for (int i = unisonVoices; i < oscillators.length; i++) {
            oscillators[i].setKeyFrequency(0);
        }
    }

    public double nextSample() {
        // Aplicar glide
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

        double deltaTime = 1.0 / AudioInfo.SAMPLE_RATE;

        // Calcular modulación del LFO
        double lfo1Value = lfo1.getNextSample();

        // Calcular modulación de la envolvente
        double modEnvValue = modEnvelope.nextSample();

        // Aplicar modulación de la envolvente al cutoff del filtro
        if (modEnvToFilter > 0) {
            double modulatedCutoff = filterCutoff;
            double modulationAmount = modEnvValue * modEnvToFilter * 5000;
            modulatedCutoff += modulationAmount;
            modulatedCutoff = Math.max(20, Math.min(22000, modulatedCutoff));
            filter.setCutoff(modulatedCutoff);
        }

        // Aplicar modulación del LFO al cutoff del filtro
        if (lfo1ToFilter > 0) {
            double modulatedCutoff = filterCutoff;
            double modulationAmount = lfo1Value * lfo1ToFilter * 5000;
            modulatedCutoff += modulationAmount;
            modulatedCutoff = Math.max(20, Math.min(22000, modulatedCutoff));
            filter.setCutoff(modulatedCutoff);
        }

        // Aplicar modulación de pitch a los osciladores
        if (lfo1ToPitch > 0) {
            double pitchModulation = lfo1Value * lfo1ToPitch * 2.0;
            for (int i = 0; i < Math.min(oscillators.length, unisonVoices); i++) {
                double baseFrequency = 440.0;
                double detuneFactor = Math.pow(2, unisonDetuneValues[i] / 1200.0);
                double modulatedFrequency = baseFrequency * Math.pow(2, pitchModulation / 12.0) * detuneFactor;
                oscillators[i].setKeyFrequency(modulatedFrequency);
            }
        }

        // Generar muestras de osciladores
        double totalSample = 0;
        int activeOscillators = 0;

        for (int i = 0; i < Math.min(oscillators.length, unisonVoices); i++) {
            totalSample += oscillators[i].getNextSample();
            activeOscillators++;
        }

        double mixedSample = activeOscillators > 0 ? totalSample / activeOscillators : 0;

        // Aplicar filtro
        double filteredSample = filter.process(mixedSample);

        // Aplicar efectos
        double effectedSample = effectsProcessor.process(filteredSample);

        // Mezcla dry/wet para efectos
        double finalSample = (filteredSample * (1.0 - effectsMix)) + (effectedSample * effectsMix);

        return finalSample;
    }

    public short[] generateSample(int numSamples) {
        for (Oscilator osc : oscillators) {
            osc.resetPhase();
        }
        filter.reset();
        lfo1.reset();
        modEnvelope.trigger();
        effectsProcessor = new EffectsProcessor(AudioInfo.SAMPLE_RATE); // Reset effects

        short[] s = new short[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double d = nextSample();
            s[i] = (short) (Short.MAX_VALUE * d);
        }
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
            triggerModEnv();
        }
    }

    public void onKeyReleased() {
        shouldGenerate = false;
        releaseModEnv();
    }

    public void shutdownAudio() {
        hiloAudio.close();
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
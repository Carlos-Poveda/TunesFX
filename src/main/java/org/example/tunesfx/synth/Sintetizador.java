package org.example.tunesfx.synth;

import org.example.tunesfx.audio.Audio;
import org.example.tunesfx.utils.Utils;
import java.util.HashMap;

public class Sintetizador {

    private static final HashMap<Character,Double> KEY_FREQUENCIES = new HashMap<>();
    private static final int MAX_VOICES = 16;

    private Oscilator[] uiOscillators;
    private WaveViewer waveViewer;
    private Filter filtroGlobal;
    private LFO lfo;
    private DelayUnit delay;

    private final Voice[] voices = new Voice[MAX_VOICES];

    // Estados
    private boolean filtroActivado = false;
    private boolean lfoActivado = false;

    // ADSR Global
    private double attack = 0.01, decay = 0.1, sustain = 0.7, release = 0.3;

    // Modulación
    private double baseFilterCutoff = 1000.0;
    private double baseFilterResonance = 0.5;
    private double lfoVolumeGain = 1.0;

    private Runnable updateCallback;

    private final Audio hiloAudio = new Audio(() -> {
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
        this.uiOscillators = oscillators;
        this.waveViewer = waveViewer;
        this.filtroGlobal = new Filter();
        this.lfo = new LFO();
        this.delay = new DelayUnit();

        for (int i = 0; i < MAX_VOICES; i++) {
            voices[i] = new Voice();
        }
        this.waveViewer.setOscillators(this.uiOscillators);
    }

    public double nextSample() {
        if (lfoActivado && lfo.getTarget() != LFO.Target.NONE) aplicarModulacionLFO();
        else lfoVolumeGain = 1.0;

        double mixedOutput = 0.0;
        int activeCount = 0;

        // Sumar voces
        for (Voice v : voices) {
            if (v.isActive()) {
                mixedOutput += v.getSample();
                activeCount++;
            }
        }

        // --- CORRECCIÓN DE SATURACIÓN ---
        // Si hay muchas voces, bajamos el volumen global antes del efecto
        if (activeCount > 1) {
            mixedOutput *= 0.6; // Reducir ganancia para dejar espacio a los acordes
        }

        // Efectos
        if (lfoActivado && lfo.getTarget() == LFO.Target.OSC_VOLUME) mixedOutput *= lfoVolumeGain;
        if (filtroActivado) mixedOutput = filtroGlobal.procesar(mixedOutput);
        mixedOutput = delay.process(mixedOutput);

        // Limitador Final (Hard Clipper suave)
        // Esto evita que suene "roto" si pasa de 1.0
        return Math.tanh(mixedOutput);
    }

    private void aplicarModulacionLFO() {
        double modValue = lfo.getModulationValue();
        switch (lfo.getTarget()) {
            case FILTER_CUTOFF:
                double modCutoff = baseFilterCutoff + (modValue * 3000);
                filtroGlobal.setFrecuenciaCorte(Math.max(20, Math.min(20000, modCutoff)));
                break;
            case FILTER_RESONANCE:
                filtroGlobal.setResonancia(Math.max(0, Math.min(1, baseFilterResonance + (modValue * 0.5))));
                break;
            case OSC_VOLUME:
                lfoVolumeGain = Math.max(0, Math.min(1, 1.0 - Math.abs(modValue)));
                break;
            case PITCH:
                for (Voice v : voices) if (v.isActive()) v.modulatePitch(modValue);
                break;
            default: break;
        }
    }

    public void noteOn(double frequency) {
        if (hiloAudio.isInitialized() && !hiloAudio.isRunning()) hiloAudio.triggerPlayBack();
        Voice freeVoice = getFreeVoice();
        if (freeVoice != null) freeVoice.trigger(frequency);
    }

    public void noteOff(double frequency) {
        for (Voice v : voices) {
            // Comparación con epsilon para evitar errores de punto flotante
            if (v.isActive() && Math.abs(v.frequency - frequency) < 0.01) {
                v.release();
                // No hacemos break porque podría haber dos notas iguales (unison extremo)
            }
        }
    }

    public void allNotesOff() { for (Voice v : voices) v.forceStop(); }

    private Voice getFreeVoice() {
        for (Voice v : voices) if (!v.isActive()) return v;
        for (Voice v : voices) if (v.adsr.getState() == ADSR.State.RELEASE) return v;
        return voices[0];
    }

    private class Voice {
        private final ADSR adsr = new ADSR();
        private double frequency;
        private boolean active = false;
        private double[] oscPhases = new double[5];

        public void trigger(double freq) {
            this.frequency = freq;
            this.active = true;
            adsr.setAttackTime(attack); adsr.setDecayTime(decay);
            adsr.setSustainLevel(sustain); adsr.setReleaseTime(release);
            adsr.noteOn();
        }

        public void release() { adsr.noteOff(); }
        public void forceStop() { active = false; adsr.noteOff(); }
        public void modulatePitch(double lfoVal) { /* Pendiente: Vibrato */ }

        public double getSample() {
            if (!active) return 0.0;

            double envelope = adsr.getNextLevel();
            // Si el ADSR terminó, apagamos la voz para ahorrar CPU
            if (envelope <= 0.0001 && adsr.getState() == ADSR.State.IDLE) {
                active = false;
                return 0.0;
            }

            double voiceMix = 0.0;
            for (int i = 0; i < uiOscillators.length; i++) {
                Oscilator uiOsc = uiOscillators[i];

                // Calcular frecuencia real incluyendo el semitone offset del oscilador
                int semiOffset = (int) uiOsc.getToneOffsetInt();
                double oscFreq = this.frequency * Math.pow(2.0, semiOffset / 12.0);

                double phaseIncrement = oscFreq * 2.0 * Math.PI / AudioInfo.SAMPLE_RATE;
                oscPhases[i] += phaseIncrement;
                if (oscPhases[i] > 2.0 * Math.PI) oscPhases[i] -= 2.0 * Math.PI;

                voiceMix += uiOsc.computeSample(oscPhases[i]);
            }

            // Normalizamos por número de osciladores
            return (voiceMix / uiOscillators.length) * envelope;
        }
        public boolean isActive() { return active; }
    }

    // --- DELEGADOS Y SETTERS ---
    public void onKeyPressed(char keyChar) {
        if (!KEY_FREQUENCIES.containsKey(keyChar)) return;
        // Solo disparar si no está ya sonando esa frecuencia (opcional, para evitar ametralladora)
        // Pero en polifonía, a veces queremos re-disparar. Lo dejamos así.
        noteOn(KEY_FREQUENCIES.get(keyChar));
    }
    public void onKeyReleased(char keyChar) { if (!KEY_FREQUENCIES.containsKey(keyChar)) return;
        double freq = KEY_FREQUENCIES.get(keyChar);
        noteOff(freq); }

    public void setFrequency(double freq) { } // Deprecated en poly

    public short[] generateSample(int numSamples) {
        allNotesOff(); filtroGlobal.reset(); lfo.reset(); delay.reset();
        noteOn(261.63); // C4
        short[] s = new short[numSamples];
        for (int i = 0; i < numSamples; i++) {
            if (i == numSamples - (int)(AudioInfo.SAMPLE_RATE * 0.2)) noteOff(261.63);
            double d = nextSample();
            s[i] = (short) (Short.MAX_VALUE * d);
        }
        return s;
    }

    public void setUpdateCallback(Runnable callback) { this.updateCallback = callback; }
    public void updateWaveviewer() { if (updateCallback != null) updateCallback.run(); }
    public void setAttack(double v) { this.attack = v; }
    public void setDecay(double v) { this.decay = v; }
    public void setSustain(double v) { this.sustain = v; }
    public void setRelease(double v) { this.release = v; }
    public void setFiltroActivado(boolean v) { filtroActivado = v; if(!v) filtroGlobal.reset(); }
    public void setTipoFiltro(Filter.Tipo t) { filtroGlobal.setTipo(t); }
    public void setFrecuenciaCorteFiltro(double f) { baseFilterCutoff = f; filtroGlobal.setFrecuenciaCorte(f); }
    public double getFrecuenciaCorteFiltro() { return baseFilterCutoff; }
    public void setResonanciaFiltro(double r) { baseFilterResonance = r; filtroGlobal.setResonancia(r); }
    public double getResonanciaFiltro() { return baseFilterResonance; }
    public void setLFOActivado(boolean v) { lfoActivado = v; if(!v) lfo.reset(); }
    public void setLFOTarget(LFO.Target t) { lfo.setTarget(t); }
    public void setLFWaveform(LFO.Waveform w) { lfo.setWaveform(w); }
    public void setLFORate(double r) { lfo.setRate(r); }
    public void setLFOAmount(double a) { lfo.setAmount(a); }
    public void setDelayActive(boolean v) { delay.setActive(v); }
    public void setDelayTime(double v) { delay.setTime(v); }
    public void setDelayFeedback(double v) { delay.setFeedback(v); }
    public void setDelayMix(double v) { delay.setMix(v); }
    public static class AudioInfo { public static final int SAMPLE_RATE = 44100; }
}
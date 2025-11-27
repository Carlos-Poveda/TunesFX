package org.example.tunesfx;

import org.example.tunesfx.utils.Utils;

public class LFO {
    public enum Waveform {
        SINE, TRIANGLE, SAW, SQUARE, RANDOM
    }

    public enum Target {
        FILTER_CUTOFF, FILTER_RESONANCE, OSC_VOLUME, PITCH, NONE
    }

    private Waveform waveform = Waveform.SINE;
    private Target target = Target.NONE;
    private double rate = 1.0; // Hz
    private double amount = 0.5; // 0.0 a 1.0
    private double phase = 0.0; // Fase actual 0.0 a 1.0

    // Para waveform RANDOM
    private double randomValue = 0.0;
    private int randomCounter = 0;
    private static final int RANDOM_UPDATE_RATE = 100; // Actualizar cada 100 muestras

    public LFO() {
        reset();
    }

    public void reset() {
        phase = 0.0;
        randomValue = Math.random() * 2 - 1; // Valor entre -1 y 1
        randomCounter = 0;
    }

    public double getNextSample() {
        double sample = 0.0;

        switch (waveform) {
            case SINE:
                sample = Math.sin(2 * Math.PI * phase);
                break;
            case TRIANGLE:
                sample = 2 * Math.abs(2 * phase - 1) - 1;
                break;
            case SAW:
                sample = 2 * phase - 1;
                break;
            case SQUARE:
                sample = (phase < 0.5) ? 1.0 : -1.0;
                break;
            case RANDOM:
                if (randomCounter++ >= RANDOM_UPDATE_RATE) {
                    randomValue = Math.random() * 2 - 1;
                    randomCounter = 0;
                }
                sample = randomValue;
                break;
        }

        // Avanzar la fase
        phase += rate / Sintetizador.AudioInfo.SAMPLE_RATE;
        if (phase >= 1.0) {
            phase -= 1.0;
        }

        return sample * amount;
    }

    public double getModulationValue() {
        return getNextSample();
    }

    // Aplicar modulación a un valor base
    public double applyModulation(double baseValue, double modulationRange) {
        double mod = getModulationValue();
        return baseValue + (mod * modulationRange);
    }

    // Getters y Setters
    public Waveform getWaveform() { return waveform; }
    public void setWaveform(Waveform waveform) {
        this.waveform = waveform;
        reset();
    }

    public Target getTarget() { return target; }
    public void setTarget(Target target) { this.target = target; }

    public double getRate() { return rate; }
    public void setRate(double rate) {
        this.rate = Math.max(0.1, Math.min(20.0, rate));
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) {
        this.amount = Math.max(0.0, Math.min(1.0, amount));
    }
}
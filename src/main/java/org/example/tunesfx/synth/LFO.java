package org.example.tunesfx.synth;

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

    // Pide el número de muestras que hemos saltado (Control Rate)
    public double getNextSample(int samplesAdvanced) {
        double sample = switch (waveform) {
            case SINE -> Math.sin(2 * Math.PI * phase);
            case TRIANGLE -> 2 * Math.abs(2 * phase - 1) - 1;
            case SAW -> 2 * phase - 1;
            case SQUARE -> (phase < 0.5) ? 1.0 : -1.0;
            case RANDOM -> randomValue;
        };

        // AVANCE CORREGIDO: Multiplicamos por los samples saltados
        phase += (rate * samplesAdvanced) / Sintetizador.AudioInfo.SAMPLE_RATE;

        if (phase >= 1.0) {
            phase -= 1.0;
            if (waveform == Waveform.RANDOM) {
                randomValue = Math.random() * 2 - 1;
            }
        }
        return sample * amount;
    }

    // Actualiza también este método:
    public double getModulationValue(int samplesAdvanced) {
        return getNextSample(samplesAdvanced);
    }

// Aplicar modulación a un valor base
//    public double applyModulation(double baseValue, double modulationRange) {
//        double mod = getModulationValue();
//        return baseValue + (mod * modulationRange);
//    }

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

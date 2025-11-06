package org.example.tunesfx;

import static java.lang.Math.*;

public class LFO {
    public enum Waveform {
        SINE, TRIANGLE, SQUARE, SAW, RAMP, RANDOM
    }

    private Waveform waveform;
    private double frequency;
    private double amplitude;
    private double phase;
    private double sampleRate;

    public LFO(double sampleRate) {
        this.sampleRate = sampleRate;
        this.waveform = Waveform.SINE;
        this.frequency = 1.0;
        this.amplitude = 1.0;
        this.phase = 0.0;
    }

    public void setWaveform(Waveform waveform) {
        this.waveform = waveform;
        System.out.println("LFO Waveform set to: " + waveform);
    }

    public void setFrequency(double frequency) {
        this.frequency = Math.max(0.1, Math.min(20.0, frequency));
        System.out.println("LFO Frequency set to: " + frequency + " Hz");
    }

    public void setAmplitude(double amplitude) {
        this.amplitude = Math.max(0.0, Math.min(1.0, amplitude));
        System.out.println("LFO Amplitude set to: " + amplitude);
    }

    public void setPhase(double phase) {
        this.phase = phase;
    }

    public double getNextSample() {
        double value = 0.0;
        double t = phase;

        switch (waveform) {
            case SINE:
                value = sin(2 * PI * t);
                break;
            case TRIANGLE:
                value = 2 * abs(2 * (t - floor(t + 0.5))) - 1;
                break;
            case SQUARE:
                value = (t - floor(t)) < 0.5 ? 1 : -1;
                break;
            case SAW:
                value = 2 * (t - floor(t + 0.5));
                break;
            case RAMP:
                value = 2 * (1 - (t - floor(t)));
                break;
            case RANDOM:
                value = (random() * 2 - 1);
                break;
        }

        phase += frequency / sampleRate;
        if (phase >= 1.0) {
            phase -= 1.0;
        }

        double result = value * amplitude;

        // DEBUG: Mostrar valores del LFO ocasionalmente
        if (Math.random() < 0.0005) { // Muy ocasionalmente para no saturar
            System.out.println("LFO Output: " + result + " (waveform: " + waveform + ", freq: " + frequency + ", amp: " + amplitude + ")");
        }

        return result;
    }

    public void reset() {
        phase = 0.0;
        System.out.println("LFO Reset");
    }
}
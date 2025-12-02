package org.example.tunesfx.audio;

public class StepData {
    private boolean active;
    private int semitoneOffset;
    // 0.0 = Instantáneo, 1.0 = Muy lento/largo
    private double attack = 0.0;
    private double release = 0.0;
    private double volume = 1.0; // Ganancia extra (0.0 a 1.0)
    private double durationFactor = 1.0; // 1.0 = 100% de la duración original
    public StepData() {
        this.active = false;
        this.semitoneOffset = 0;

    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getSemitoneOffset() { return semitoneOffset; }
    public void setSemitoneOffset(int semitoneOffset) { this.semitoneOffset = semitoneOffset; }

    // Calcular el multiplicador de pitch para OpenAL
    // Fórmula: 2^(semitonos/12)
    public float getPitchMultiplier() {
        return (float) Math.pow(2, semitoneOffset / 12.0);
    }

    public double getAttack() {
        return attack;
    }

    public void setAttack(double attack) {
        this.attack = attack;
    }

    public double getRelease() {
        return release;
    }

    public void setRelease(double release) {
        this.release = release;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getDurationFactor() {
        return durationFactor;
    }

    public void setDurationFactor(double durationFactor) {
        this.durationFactor = durationFactor;
    }
}
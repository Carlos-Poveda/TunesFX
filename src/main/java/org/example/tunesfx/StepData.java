package org.example.tunesfx;

public class StepData {
    private boolean active;
    private int semitoneOffset; // 0 = nota original, 2 = +2 semitonos, -12 = octava abajo

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
}
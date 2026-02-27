package org.example.tunesfx.synth;

public class ADSR {
    public enum State { IDLE, ATTACK, DECAY, SUSTAIN, RELEASE }

    private State state = State.IDLE;
    private double currentLevel = 0.0;

    // Valores en milisegundos y porcentaje
    private double attackTimeMs = 10.0;
    private double decayTimeMs = 100.0;
    private double sustainLevel = 0.7; // 0.0 a 1.0
    private double releaseTimeMs = 200.0;

    private double attackRate;
    private double decayRate;
    private double releaseRate;

    public ADSR() {
        calculateRates();
    }

    private void calculateRates() {
        double sampleRate = Sintetizador.AudioInfo.SAMPLE_RATE;
        // Incremento por muestra para ir de 0 a 1 en el tiempo especificado
        attackRate = 1.0 / (attackTimeMs * sampleRate / 1000.0);
        // Decremento por muestra para ir de 1 al Sustain en el tiempo especificado
        decayRate = (1.0 - sustainLevel) / (decayTimeMs * sampleRate / 1000.0);
        // Decremento por muestra para ir del Sustain a 0
        releaseRate = sustainLevel / (releaseTimeMs * sampleRate / 1000.0);
    }

    public void noteOn() {
        state = State.ATTACK;
        calculateRates();
    }

    public void noteOff() {
        state = State.RELEASE;
        calculateRates();
    }

    public double getNextEnvelope() {
        switch (state) {
            case ATTACK:
                currentLevel += attackRate;
                if (currentLevel >= 1.0) {
                    currentLevel = 1.0;
                    state = State.DECAY;
                }
                break;
            case DECAY:
                currentLevel -= decayRate;
                if (currentLevel <= sustainLevel) {
                    currentLevel = sustainLevel;
                    state = State.SUSTAIN;
                }
                break;
            case SUSTAIN:
                currentLevel = sustainLevel;
                break;
            case RELEASE:
                currentLevel -= releaseRate;
                if (currentLevel <= 0.0) {
                    currentLevel = 0.0;
                    state = State.IDLE;
                }
                break;
            case IDLE:
                currentLevel = 0.0;
                break;
        }
        return currentLevel;
    }

    public boolean isIdle() { return state == State.IDLE; }

    // Setters para la UI
    public void setAttackTime(double ms) { this.attackTimeMs = Math.max(1, ms); calculateRates(); }
    public void setDecayTime(double ms) { this.decayTimeMs = Math.max(1, ms); calculateRates(); }
    public void setSustainLevel(double level) { this.sustainLevel = Math.max(0, Math.min(1, level)); calculateRates(); }
    public void setReleaseTime(double ms) { this.releaseTimeMs = Math.max(1, ms); calculateRates(); }
}
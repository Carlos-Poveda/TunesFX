package org.example.tunesfx.synth;

public class ADSR {
    public enum State { IDLE, ATTACK, DECAY, SUSTAIN, RELEASE }

    private State state = State.IDLE;
    private double currentLevel = 0.0;
    private double attackRate, decayRate, releaseRate;
    private double sustainLevel;

    // Parámetros almacenados (tiempos en segundos)
    private double attackTime = 0.01;
    private double decayTime = 0.1;
    private double releaseTime = 0.3;

    // Target Sustain (0.0 a 1.0)
    private double targetSustain = 0.7;

    public void noteOn() {
        state = State.ATTACK;
        calcRates();
    }

    public void noteOff() {
        state = State.RELEASE;
        calcRates();
    }

    public double getNextLevel() {
        switch (state) {
            case IDLE:
                currentLevel = 0.0;
                break;
            case ATTACK:
                currentLevel += attackRate;
                if (currentLevel >= 1.0) {
                    currentLevel = 1.0;
                    state = State.DECAY;
                }
                break;
            case DECAY:
                currentLevel -= decayRate;
                if (currentLevel <= targetSustain) {
                    currentLevel = targetSustain;
                    state = State.SUSTAIN;
                }
                break;
            case SUSTAIN:
                currentLevel = targetSustain;
                break;
            case RELEASE:
                // Release exponencial para sonido natural
                currentLevel *= releaseRate;
                if (currentLevel < 0.001) {
                    currentLevel = 0.0;
                    state = State.IDLE;
                }
                break;
        }
        return currentLevel;
    }

    private void calcRates() {
        double sampleRate = Sintetizador.AudioInfo.SAMPLE_RATE;

        // Attack: Lineal (cuánto sumar por muestra)
        // Mínimo 10 muestras para evitar clicks
        double aSamples = Math.max(10, attackTime * sampleRate);
        this.attackRate = 1.0 / aSamples;

        // Decay: Lineal (cuánto restar por muestra)
        double dSamples = Math.max(10, decayTime * sampleRate);
        // Distancia a recorrer: de 1.0 a Sustain
        this.decayRate = (1.0 - targetSustain) / dSamples;

        // Release: Exponencial (multiplicador)
        // Mapeamos el tiempo a un factor de decaimiento
        // Math.exp(-5.0 / muestras) hace que caiga al 1% en ese tiempo
        double rSamples = Math.max(10, releaseTime * sampleRate);
        this.releaseRate = Math.exp(-5.0 / rSamples);
    }

    // --- Setters ---
    public void setAttackTime(double seconds) { this.attackTime = Math.max(0.001, seconds); calcRates(); }
    public void setDecayTime(double seconds) { this.decayTime = Math.max(0.001, seconds); calcRates(); }
    public void setSustainLevel(double level) { this.targetSustain = Math.max(0.0, Math.min(1.0, level)); calcRates(); }
    public void setReleaseTime(double seconds) { this.releaseTime = Math.max(0.001, seconds); calcRates(); }

    public boolean isActive() {
        return state != State.IDLE;
    }

    public State getState() { return state; }
}
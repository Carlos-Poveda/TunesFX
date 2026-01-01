package org.example.tunesfx;

public class Envelope {
    private double attackTime;  // en segundos
    private double decayTime;   // en segundos
    private double sustainLevel; // 0.0 a 1.0
    private double releaseTime; // en segundos

    private double sampleRate;
    private int currentStage;
    private double currentLevel;
    private double multiplier;
    private double currentSampleIndex;
    private double stageValueStart;
    private double nextStageLevel;

    private static final int STAGE_OFF = 0;
    private static final int STAGE_ATTACK = 1;
    private static final int STAGE_DECAY = 2;
    private static final int STAGE_SUSTAIN = 3;
    private static final int STAGE_RELEASE = 4;

    public Envelope(double sampleRate) {
        this.sampleRate = sampleRate;
        this.attackTime = 0.01;
        this.decayTime = 0.1;
        this.sustainLevel = 0.5;
        this.releaseTime = 0.2;
        this.currentStage = STAGE_OFF;
        this.currentLevel = 0.0;
        this.multiplier = 1.0;
        this.currentSampleIndex = 0.0;
        this.stageValueStart = 0.0;
        this.nextStageLevel = 0.0;
    }

    public void setAttackTime(double attackTime) {
        this.attackTime = attackTime;
    }

    public void setDecayTime(double decayTime) {
        this.decayTime = decayTime;
    }

    public void setSustainLevel(double sustainLevel) {
        this.sustainLevel = sustainLevel;
    }

    public void setReleaseTime(double releaseTime) {
        this.releaseTime = releaseTime;
    }

    public void trigger() {
        currentStage = STAGE_ATTACK;
        currentSampleIndex = 0.0;
        stageValueStart = currentLevel;
        nextStageLevel = 1.0;
        calculateMultiplier(stageValueStart, nextStageLevel, attackTime);
    }

    public void release() {
        currentStage = STAGE_RELEASE;
        currentSampleIndex = 0.0;
        stageValueStart = currentLevel;
        nextStageLevel = 0.0;
        calculateMultiplier(stageValueStart, nextStageLevel, releaseTime);
    }

    public double nextSample() {
        if (currentStage == STAGE_OFF) {
            return 0.0;
        }

        if (currentSampleIndex >= getStageLength(currentStage)) {
            // Avanzar al siguiente stage
            switch (currentStage) {
                case STAGE_ATTACK:
                    currentStage = STAGE_DECAY;
                    currentSampleIndex = 0.0;
                    stageValueStart = currentLevel;
                    nextStageLevel = sustainLevel;
                    calculateMultiplier(stageValueStart, nextStageLevel, decayTime);
                    break;
                case STAGE_DECAY:
                    currentStage = STAGE_SUSTAIN;
                    currentSampleIndex = 0.0;
                    stageValueStart = currentLevel;
                    nextStageLevel = sustainLevel;
                    // En sustain no hay multiplicador porque se mantiene el nivel
                    break;
                case STAGE_RELEASE:
                    currentStage = STAGE_OFF;
                    currentLevel = 0.0;
                    break;
            }
        }

        if (currentStage == STAGE_SUSTAIN) {
            currentLevel = sustainLevel;
        } else {
            currentLevel = stageValueStart + (1.0 - Math.exp(-currentSampleIndex * multiplier)) * (nextStageLevel - stageValueStart);
        }

        currentSampleIndex++;
        return currentLevel;
    }

    private double getStageLength(int stage) {
        switch (stage) {
            case STAGE_ATTACK: return attackTime * sampleRate;
            case STAGE_DECAY: return decayTime * sampleRate;
            case STAGE_SUSTAIN: return Double.MAX_VALUE; // Sustain se mantiene hasta que se suelte
            case STAGE_RELEASE: return releaseTime * sampleRate;
            default: return 0.0;
        }
    }

    private void calculateMultiplier(double startLevel, double endLevel, double time) {
        if (time <= 0.0) {
            multiplier = 1.0;
            return;
        }
        double numSamples = time * sampleRate;
        multiplier = 1.0 + (Math.log(Math.abs(endLevel - startLevel) / 0.0001) / numSamples);
    }

    public boolean isActive() {
        return currentStage != STAGE_OFF;
    }
}
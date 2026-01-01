package org.example.tunesfx;

public class EffectsProcessor {
    private double mix;
    private double reverbLevel;
    private double delayLevel;
    private double delayFeedback;
    private double delayTime;

    private double[] reverbBuffer;
    private double[] delayBuffer;
    private int reverbIndex;
    private int delayIndex;
    private int sampleRate;

    public EffectsProcessor(int sampleRate) {
        this.sampleRate = sampleRate;
        this.mix = 0.0;
        this.reverbLevel = 0.0;
        this.delayLevel = 0.0;
        this.delayFeedback = 0.5;
        this.delayTime = 0.5;

        int reverbSize = (int)(sampleRate * 2.0);
        int delaySize = (int)(sampleRate * 2.0);
        reverbBuffer = new double[reverbSize];
        delayBuffer = new double[delaySize];
        reverbIndex = 0;
        delayIndex = 0;
    }

    public void setReverbLevel(double level) {
        this.reverbLevel = Math.max(0.0, Math.min(1.0, level));
    }

    public void setDelayLevel(double level) {
        this.delayLevel = Math.max(0.0, Math.min(1.0, level));
    }

    public void setDelayFeedback(double feedback) {
        this.delayFeedback = Math.max(0.0, Math.min(0.95, feedback));
    }

    public void setDelayTime(double time) {
        this.delayTime = Math.max(0.01, Math.min(2.0, time));
    }

    public void setMix(double mix) {
        this.mix = Math.max(0.0, Math.min(1.0, mix));
    }

    public double process(double input) {
        if (mix == 0.0) {
            return input; // Passthrough si mix es 0
        }

        double dry = input;
        double wet = 0.0;

        // Procesar delay
        if (delayLevel > 0) {
            int delaySamples = (int)(delayTime * sampleRate);
            int readIndex = (delayIndex - delaySamples + delayBuffer.length) % delayBuffer.length;

            double delayed = delayBuffer[readIndex];
            wet += delayed * delayLevel;

            // Feedback del delay
            delayBuffer[delayIndex] = input + (delayed * delayFeedback);
            delayIndex = (delayIndex + 1) % delayBuffer.length;
        }

        // Procesar reverb simple (comb filters)
        if (reverbLevel > 0) {
            // Múltiples taps para reverb más interesante
            int[] taps = {
                    (int)(0.0297 * sampleRate),
                    (int)(0.0371 * sampleRate),
                    (int)(0.0411 * sampleRate),
                    (int)(0.0437 * sampleRate)
            };

            double reverbSum = 0;
            for (int tap : taps) {
                int readPos = (reverbIndex - tap + reverbBuffer.length) % reverbBuffer.length;
                reverbSum += reverbBuffer[readPos] * 0.25;
            }

            wet += reverbSum * reverbLevel;

            // Actualizar buffer de reverb
            reverbBuffer[reverbIndex] = input * 0.3 + reverbSum * 0.7;
            reverbIndex = (reverbIndex + 1) % reverbBuffer.length;
        }

        // Mix dry/wet
        return (dry * (1.0 - mix)) + (wet * mix);
    }
}
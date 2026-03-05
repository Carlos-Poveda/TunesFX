package org.example.tunesfx.synth;
import org.example.tunesfx.utils.Utils;

public enum WaveTable {
    Sine, Square, Saw, Triangle, Pulse25, Organ, Noise;

    public static final int SIZE = 8192;
    private final float[] samples = new float[SIZE];

    static {
        final double FUND_FREQ = 1d / (SIZE / (double) Sintetizador.AudioInfo.SAMPLE_RATE);
        final double W = Utils.Math.frequencyToAngularFrecuency(FUND_FREQ);
        for (int i = 0; i < SIZE; i++){
            double t = i / (double) Sintetizador.AudioInfo.SAMPLE_RATE;
            double tDivP = t / (1d/FUND_FREQ);
            double phase = tDivP - Math.floor(tDivP);
            Sine.samples[i] = (float) Math.sin(W * t);
            Square.samples[i] = Math.signum(Sine.samples[i]);
            Saw.samples[i] = (float) (2d * (tDivP - Math.floor(0.5 + tDivP)));
            Triangle.samples[i] = (float) (2d * Math.abs(Saw.samples[i]) - 1d);

            // Pulse 25%: 1.0 si estamos en el primer 25% del ciclo, -1.0 en el resto
            Pulse25.samples[i] = (phase < 0.25) ? 1.0f : -1.0f;
            // Organ: Fundamental + 1ª octava (mitad de volumen) + 2ª octava (cuarto de volumen)
            // Dividimos entre 1.75 para normalizar y que no pase de 1.0 (evita clics)
            Organ.samples[i] = (float) ((Math.sin(W * t) +
                    (0.5 * Math.sin(2 * W * t)) +
                    (0.25 * Math.sin(3 * W * t))) / 1.75);
            // Noise: Ruido aleatorio entre -1.0 y 1.0
            Noise.samples[i] = (float) (Math.random() * 2d - 1d);
        }
    }

    public float[] getSamples() {
        return samples;
    }
}
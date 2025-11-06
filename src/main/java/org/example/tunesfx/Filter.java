package org.example.tunesfx;

public class Filter {
    public enum FilterType {
        LOW_PASS, HIGH_PASS, BAND_PASS, NOTCH, OFF
    }

    private FilterType type;
    private double cutoff;
    private double resonance;
    private double sampleRate;

    // Estado del filtro (para el algoritmo biquad)
    private double x1, x2, y1, y2;

    // Coeficientes del filtro
    private double b0, b1, b2, a0, a1, a2;

    public Filter(double sampleRate) {
        this.sampleRate = sampleRate;
        this.type = FilterType.OFF;
        this.cutoff = 1000.0;
        this.resonance = 1.0;
        calculateCoefficients();
    }

    public void setType(FilterType type) {
        this.type = type;
        calculateCoefficients();
    }

    public void setCutoff(double cutoff) {
        this.cutoff = Math.max(20.0, Math.min(20000.0, cutoff));
        calculateCoefficients();
    }

    public void setResonance(double resonance) {
        this.resonance = Math.max(0.1, Math.min(10.0, resonance));
        calculateCoefficients();
    }

    private void calculateCoefficients() {
        if (type == FilterType.OFF) {
            // Configuración de "paso-through" cuando el filtro está desactivado
            b0 = 1.0; b1 = 0.0; b2 = 0.0;
            a0 = 1.0; a1 = 0.0; a2 = 0.0;
            return;
        }

        double omega = 2.0 * Math.PI * cutoff / sampleRate;
        double sinOmega = Math.sin(omega);
        double cosOmega = Math.cos(omega);
        double alpha = sinOmega / (2.0 * resonance);

        switch (type) {
            case LOW_PASS:
                b0 = (1 - cosOmega) / 2;
                b1 = 1 - cosOmega;
                b2 = (1 - cosOmega) / 2;
                a0 = 1 + alpha;
                a1 = -2 * cosOmega;
                a2 = 1 - alpha;
                break;

            case HIGH_PASS:
                b0 = (1 + cosOmega) / 2;
                b1 = -(1 + cosOmega);
                b2 = (1 + cosOmega) / 2;
                a0 = 1 + alpha;
                a1 = -2 * cosOmega;
                a2 = 1 - alpha;
                break;

            case BAND_PASS:
                b0 = alpha;
                b1 = 0;
                b2 = -alpha;
                a0 = 1 + alpha;
                a1 = -2 * cosOmega;
                a2 = 1 - alpha;
                break;

            case NOTCH:
                b0 = 1;
                b1 = -2 * cosOmega;
                b2 = 1;
                a0 = 1 + alpha;
                a1 = -2 * cosOmega;
                a2 = 1 - alpha;
                break;

            default:
                b0 = 1.0; b1 = 0.0; b2 = 0.0;
                a0 = 1.0; a1 = 0.0; a2 = 0.0;
                break;
        }

        // Normalizar coeficientes
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;
    }

    public double process(double input) {
        if (type == FilterType.OFF) {
            return input; // Passthrough cuando está desactivado
        }

        double output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;

        // Actualizar estado del filtro
        x2 = x1;
        x1 = input;
        y2 = y1;
        y1 = output;

        return output;
    }

    public void reset() {
        x1 = x2 = y1 = y2 = 0;
    }

    // Getters
    public FilterType getType() { return type; }
    public double getCutoff() { return cutoff; }
    public double getResonance() { return resonance; }
}
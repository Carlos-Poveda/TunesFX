package org.example.tunesfx.synth;

public class Filter {
    public enum Tipo {
        LOW_PASS, HIGH_PASS, BAND_PASS, OFF
    }

    private Tipo tipo = Tipo.OFF;
    private double frecuenciaCorte;
    private double resonancia; // Valor de 0.0 a 1.0 (mapeado internamente a Q)

    // Estado del filtro (History)
    private double x1, x2, y1, y2;

    // Coeficientes
    private double a0, a1, a2, b1, b2;

    public Filter() {
        setTipo(Tipo.OFF);
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
        // Valores por defecto seguros si se activa por primera vez
        if (this.frecuenciaCorte == 0) this.frecuenciaCorte = 1000;
        if (this.resonancia == 0) this.resonancia = 0.5;
        calcularCoeficientes();
    }

    public void setFrecuenciaCorte(double frecuencia) {
        // Limitamos entre 20Hz y 20kHz
        this.frecuenciaCorte = Math.max(20.0, Math.min(20000.0, frecuencia));
        calcularCoeficientes();
    }

    public void setResonancia(double resonancia) {
        // Limitamos entre 0 y 1 (desde la UI)
        this.resonancia = Math.max(0.0, Math.min(1.0, resonancia));
        calcularCoeficientes();
    }

    private void calcularCoeficientes() {
        if (tipo == Tipo.OFF) return;

        double sampleRate = Sintetizador.AudioInfo.SAMPLE_RATE;
        double w0 = 2 * Math.PI * frecuenciaCorte / sampleRate;
        double cosW0 = Math.cos(w0);
        double sinW0 = Math.sin(w0);

        // Mapear la resonancia (0.0 - 1.0) a un valor Q útil.
        // Q = 0.707 es plano. Q > 1 empieza a resonar. Q = 5 o 10 es muy resonante.
        // Fórmula: Q va de 0.707 a 10.0
        double q = 0.707 + (resonancia * 9.0);

        double alpha = sinW0 / (2 * q);

        // Variables temporales para coeficientes normalizados
        double b0_tmp = 0, b1_tmp = 0, b2_tmp = 0, a0_tmp = 0, a1_tmp = 0, a2_tmp = 0;

        switch (tipo) {
            case LOW_PASS:
                b0_tmp =  (1 - cosW0) / 2;
                b1_tmp =   1 - cosW0;
                b2_tmp =  (1 - cosW0) / 2;
                a0_tmp =   1 + alpha;
                a1_tmp =  -2 * cosW0;
                a2_tmp =   1 - alpha;
                break;

            case HIGH_PASS:
                b0_tmp =  (1 + cosW0) / 2;
                b1_tmp = -(1 + cosW0);
                b2_tmp =  (1 + cosW0) / 2;
                a0_tmp =   1 + alpha;
                a1_tmp =  -2 * cosW0;
                a2_tmp =   1 - alpha;
                break;

            case BAND_PASS:
                b0_tmp =   alpha;
                b1_tmp =   0;
                b2_tmp =  -alpha;
                a0_tmp =   1 + alpha;
                a1_tmp =  -2 * cosW0;
                a2_tmp =   1 - alpha;
                break;

            default:
                return;
        }

        // Normalizar los coeficientes dividiendo todo por a0
        // (Esto es crucial para que el filtro sea estable)
        this.a0 = b0_tmp / a0_tmp;
        this.a1 = b1_tmp / a0_tmp;
        this.a2 = b2_tmp / a0_tmp;
        this.b1 = a1_tmp / a0_tmp;
        this.b2 = a2_tmp / a0_tmp;
    }

    public double procesar(double entrada) {
        if (tipo == Tipo.OFF) return entrada;

        // Fórmula estándar Biquad (Forma Directa I)
        // y[n] = b0*x[n] + b1*x[n-1] + b2*x[n-2] - a1*y[n-1] - a2*y[n-2]
        double salida = (a0 * entrada) + (a1 * x1) + (a2 * x2) - (b1 * y1) - (b2 * y2);

        // Mover el historial (shift buffers)
        x2 = x1;
        x1 = entrada;
        y2 = y1;
        y1 = salida;

        return salida;
    }

    public void reset() {
        x1 = x2 = y1 = y2 = 0;
    }

    // Getters
    public Tipo getTipo() { return tipo; }
    public double getFrecuenciaCorte() { return frecuenciaCorte; }
    public double getResonancia() { return resonancia; }
}
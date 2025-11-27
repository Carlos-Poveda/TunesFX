package org.example.tunesfx;

public class Filter {
    public enum Tipo {
        LOW_PASS, HIGH_PASS, BAND_PASS, OFF
    }

    private Tipo tipo = Tipo.OFF;
    private double frecuenciaCorte;
    private double resonancia;

    // Estado del filtro para cada canal (estéreo)
    private double[] estadoX = new double[2]; // Entradas anteriores
    private double[] estadoY = new double[2]; // Salidas anteriores

    // Coeficientes del filtro
    private double a0, a1, a2, b1, b2;

    public Filter() {
        // Configurar valores por defecto para LOW_PASS
        setTipo(Tipo.LOW_PASS);
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;

        // CONFIGURAR PARÁMETROS POR DEFECTO SEGÚN EL TIPO DE FILTRO
        switch (tipo) {
            case LOW_PASS:
                // Filtro pasa bajos - suave, corte medio
                this.frecuenciaCorte = 2000.0; // Hz
                this.resonancia = 0.3;
                break;

            case HIGH_PASS:
                // Filtro pasa altos - elimina frecuencias bajas
                this.frecuenciaCorte = 500.0; // Hz
                this.resonancia = 0.4;
                break;

            case BAND_PASS:
                // Filtro pasa banda - enfoca en frecuencias medias
                this.frecuenciaCorte = 1000.0; // Hz
                this.resonancia = 0.6; // Mayor resonancia para efecto más pronunciado
                break;

            case OFF:
                // No necesita parámetros específicos
                break;
        }

        calcularCoeficientes();
    }

    public void setFrecuenciaCorte(double frecuencia) {
        this.frecuenciaCorte = Math.max(20.0, Math.min(20000.0, frecuencia));
        calcularCoeficientes();
    }

    public void setResonancia(double resonancia) {
        this.resonancia = Math.max(0.1, Math.min(1.0, resonancia));
        calcularCoeficientes();
    }

    private void calcularCoeficientes() {
        if (tipo == Tipo.OFF) {
            return;
        }

        double sampleRate = Sintetizador.AudioInfo.SAMPLE_RATE;
        double frecuenciaNormalizada = frecuenciaCorte / sampleRate;

        // Filtro basado en la transformación bilineal
        double omega = 2 * Math.PI * frecuenciaNormalizada;
        double sinOmega = Math.sin(omega);
        double cosOmega = Math.cos(omega);

        double alpha = sinOmega / (2.0 * resonancia);

        switch (tipo) {
            case LOW_PASS:
                a0 = (1 - cosOmega) / 2;
                a1 = 1 - cosOmega;
                a2 = a0;
                b1 = -2 * cosOmega;
                b2 = 1 - alpha;
                break;

            case HIGH_PASS:
                a0 = (1 + cosOmega) / 2;
                a1 = -(1 + cosOmega);
                a2 = a0;
                b1 = -2 * cosOmega;
                b2 = 1 - alpha;
                break;

            case BAND_PASS:
                a0 = alpha;
                a1 = 0;
                a2 = -alpha;
                b1 = -2 * cosOmega;
                b2 = 1 - alpha;
                break;

            default:
                return;
        }

        // Normalizar coeficientes
        double norm = 1 + alpha;
        a0 /= norm;
        a1 /= norm;
        a2 /= norm;
        b1 /= norm;
        b2 /= norm;
    }

    public double procesar(double entrada) {
        if (tipo == Tipo.OFF) {
            return entrada;
        }

        // Aplicar filtro IIR biquad
        double salida = a0 * entrada + a1 * estadoX[0] + a2 * estadoX[1]
                - b1 * estadoY[0] - b2 * estadoY[1];

        // Actualizar estado
        estadoX[1] = estadoX[0];
        estadoX[0] = entrada;
        estadoY[1] = estadoY[0];
        estadoY[0] = salida;

        return salida;
    }

    public void reset() {
        estadoX[0] = estadoX[1] = 0;
        estadoY[0] = estadoY[1] = 0;
    }

    // Getters para la UI
    public Tipo getTipo() { return tipo; }
    public double getFrecuenciaCorte() { return frecuenciaCorte; }
    public double getResonancia() { return resonancia; }
}